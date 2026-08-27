package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.fluids.util.AEFluidInventory;
import appeng.fluids.util.IAEFluidInventory;
import appeng.fluids.util.IAEFluidTank;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.inventory.IGasInventory;
import com.mekeng.github.common.me.inventory.IGasInventoryHost;
import com.mekeng.github.common.me.inventory.impl.GasInventory;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import github.kasuminova.mmce.common.util.GasInventoryHandler;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import mekanism.api.gas.GasStack;
import mekanism.common.capabilities.Capabilities;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.Lock;

/**
 * Sixteen-channel ME input assembly. Each channel may hold an item, fluid or
 * gas target, while all three backing inventories participate in one MMCE
 * input group.
 */
public class TileMEInputAssembly extends MEItemInputBus
    implements IAEFluidInventory, IGasInventoryHost,
    MachineCombinationComponent {

    public static final int SLOT_COUNT = 16;
    private static final int CAPACITY = Integer.MAX_VALUE;
    private static final String TAG_FLUIDS = "mixedFluidTanks";
    private static final String TAG_GASES = "mixedGasTanks";

    private final IFluidStorageChannel fluidChannel = AEApi.instance()
        .storage().getStorageChannel(IFluidStorageChannel.class);
    private final IGasStorageChannel gasChannel = AEApi.instance()
        .storage().getStorageChannel(IGasStorageChannel.class);
    private final AEFluidInventory fluidTanks =
        new AEFluidInventory(this, SLOT_COUNT, CAPACITY);
    private final GasInventory gasTanks =
        new GasInventory(SLOT_COUNT, CAPACITY, this);
    private final GasInventoryHandler gasHandler =
        new GasInventoryHandler(gasTanks);
    private final IOInventory displayInventory = buildDisplayInventory();

    private final MachineComponent<net.minecraftforge.fluids.capability.IFluidHandler>
        fluidComponent = new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public net.minecraftforge.fluids.capability.IFluidHandler
            getContainerProvider() {
                return fluidTanks;
            }

            @Override
            public long getGroupID() {
                return TileMEInputAssembly.this.getGroupId();
            }
        };

    private final MachineComponent<IExtendedGasHandler> gasComponent =
        new MachineComponent<IExtendedGasHandler>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_GAS;
            }

            @Override
            public IExtendedGasHandler getContainerProvider() {
                return gasHandler;
            }

            @Override
            public long getGroupID() {
                return TileMEInputAssembly.this.getGroupId();
            }
        };

    public TileMEInputAssembly() {
        // The item GUI normally binds the real inventory directly. This GUI
        // binds a type-neutral display inventory, so mirror recipe consumption
        // immediately instead of waiting for the next grid tick.
        inventory.setListener(slot -> {
            markNoUpdate();
            refreshDisplay(slot);
            if (!inTick) wakeGridTicking();
        });
    }

    private IOInventory buildDisplayInventory() {
        int[] slots = slots();
        IOInventory display = new IOInventory(this, slots, new int[0]);
        display.setStackLimit(CAPACITY, slots);
        return display;
    }

    private static int[] slots() {
        int[] slots = new int[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) slots[slot] = slot;
        return slots;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_INPUT_ASSEMBLY == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_INPUT_ASSEMBLY);
    }

    public IOInventory getDisplayInventory() {
        return displayInventory;
    }

    public IAEFluidTank getFluidTanks() {
        return fluidTanks;
    }

    public IGasInventory getGasTanks() {
        return gasTanks;
    }

    public void setMarker(int slot, ItemStack marker) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        getConfigInventory().setStackInSlot(slot,
            MixedMEInputMarker.sanitize(marker));
        markNoUpdate();
        wakeGridTicking();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        // Machine recipes may consume any of the three buffers at any time.
        return new TickingRequest(10, 120, false, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node,
                                              int ticksSinceLastCall) {
        if (!proxy.isActive()) return TickRateModulation.IDLE;
        Lock itemLock = inventory.getRWLock().writeLock();
        itemLock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEItemStack> items =
                proxy.getStorage().getInventory(channel);
            IMEMonitor<IAEFluidStack> fluids =
                proxy.getStorage().getInventory(fluidChannel);
            IMEMonitor<IAEGasStack> gases =
                proxy.getStorage().getInventory(gasChannel);
            boolean changed;
            synchronized (fluidTanks) {
                synchronized (gasTanks) {
                    changed = synchronizeChannels(items, fluids, gases);
                }
            }
            refreshDisplay();
            return changed ? TickRateModulation.FASTER
                : TickRateModulation.SLOWER;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            inTick = false;
            itemLock.unlock();
        }
    }

    private boolean synchronizeChannels(IMEMonitor<IAEItemStack> items,
        IMEMonitor<IAEFluidStack> fluids, IMEMonitor<IAEGasStack> gases)
        throws GridAccessException {
        boolean changed = false;
        IOInventory config = getConfigInventory();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack marker = config.getStackInSlot(slot);
            int type = MixedMEInputMarker.getType(marker);

            changed |= returnWrongItem(slot, type, marker, items);
            changed |= returnWrongFluid(slot, type, marker, fluids);
            changed |= returnWrongGas(slot, type, marker, gases);

            if (type == MixedMEInputMarker.TYPE_ITEM) {
                changed |= balanceItem(slot, marker, items);
            } else if (type == MixedMEInputMarker.TYPE_FLUID) {
                changed |= balanceFluid(slot,
                    MixedMEInputMarker.getFluid(marker), fluids);
            } else if (type == MixedMEInputMarker.TYPE_GAS) {
                changed |= balanceGas(slot,
                    MixedMEInputMarker.getGas(marker), gases);
            }
        }
        return changed;
    }

    private boolean returnWrongItem(int slot, int type, ItemStack marker,
                                    IMEMonitor<IAEItemStack> monitor)
        throws GridAccessException {
        ItemStack stored = inventory.getStackInSlot(slot);
        if (stored.isEmpty() || (type == MixedMEInputMarker.TYPE_ITEM
            && sameItem(stored, marker))) return false;
        ItemStack remainder = insertItem(monitor, stored);
        inventory.setStackInSlot(slot, remainder);
        return remainder.getCount() != stored.getCount();
    }

    private boolean returnWrongFluid(int slot, int type, ItemStack marker,
        IMEMonitor<IAEFluidStack> monitor) throws GridAccessException {
        IAEFluidStack stored = fluidTanks.getFluidInSlot(slot);
        FluidStack configured = MixedMEInputMarker.getFluid(marker);
        if (stored == null || (type == MixedMEInputMarker.TYPE_FLUID
            && configured != null
            && stored.getFluidStack().isFluidEqual(configured))) return false;
        IAEFluidStack remainder = Platform.poweredInsert(proxy.getEnergy(),
            monitor, stored.copy(), source);
        fluidTanks.setFluidInSlot(slot, remainder);
        return remainder == null
            || remainder.getStackSize() != stored.getStackSize();
    }

    private boolean returnWrongGas(int slot, int type, ItemStack marker,
        IMEMonitor<IAEGasStack> monitor) throws GridAccessException {
        GasStack stored = gasTanks.getGasStack(slot);
        GasStack configured = MixedMEInputMarker.getGas(marker);
        if (stored == null || (type == MixedMEInputMarker.TYPE_GAS
            && configured != null && stored.isGasEqual(configured))) {
            return false;
        }
        IAEGasStack request = AEGasStack.of(stored.copy());
        if (request == null) return false;
        IAEGasStack remainder = Platform.poweredInsert(proxy.getEnergy(),
            monitor, request, source);
        gasTanks.setGas(slot,
            remainder == null ? null : remainder.getGasStack());
        return remainder == null
            || remainder.getStackSize() != stored.amount;
    }

    private boolean balanceItem(int slot, ItemStack marker,
                                IMEMonitor<IAEItemStack> monitor)
        throws GridAccessException {
        if (marker.isEmpty()) return false;
        ItemStack stored = inventory.getStackInSlot(slot);
        long current = stored.isEmpty() ? 0L : stored.getCount();
        long target = MixedMEInputMarker.getAmount(marker);
        if (current < target) {
            IAEItemStack request = channel.createStack(marker);
            if (request == null) return false;
            request.setStackSize(target - current);
            IAEItemStack extracted = Platform.poweredExtraction(
                proxy.getEnergy(), monitor, request, source);
            if (extracted == null || extracted.getStackSize() <= 0L) {
                return false;
            }
            ItemStack pulled = extracted.createItemStack();
            if (stored.isEmpty()) inventory.setStackInSlot(slot, pulled);
            else {
                stored.grow(pulled.getCount());
                inventory.setStackInSlot(slot, stored);
            }
            return true;
        }
        if (current > target) {
            ItemStack excess = stored.copy();
            excess.setCount((int) (current - target));
            ItemStack remainder = insertItem(monitor, excess);
            int inserted = excess.getCount() - remainder.getCount();
            if (inserted <= 0) return false;
            stored.shrink(inserted);
            inventory.setStackInSlot(slot, stored);
            return true;
        }
        return false;
    }

    private boolean balanceFluid(int slot, @Nullable FluidStack configured,
        IMEMonitor<IAEFluidStack> monitor) throws GridAccessException {
        if (configured == null) return false;
        IAEFluidStack stored = fluidTanks.getFluidInSlot(slot);
        long current = stored == null ? 0L : stored.getStackSize();
        long target = configured.amount;
        if (current < target) {
            IAEFluidStack request = fluidChannel.createStack(configured);
            if (request == null) return false;
            request.setStackSize(target - current);
            IAEFluidStack extracted = Platform.poweredExtraction(
                proxy.getEnergy(), monitor, request, source);
            if (extracted == null || extracted.getStackSize() <= 0L) {
                return false;
            }
            IAEFluidStack combined = extracted.copy();
            combined.setStackSize(current + extracted.getStackSize());
            fluidTanks.setFluidInSlot(slot, combined);
            return true;
        }
        if (current > target) {
            IAEFluidStack excess = stored.copy();
            excess.setStackSize(current - target);
            IAEFluidStack remainder = Platform.poweredInsert(
                proxy.getEnergy(), monitor, excess, source);
            long returned = remainder == null ? 0L
                : remainder.getStackSize();
            long inserted = excess.getStackSize() - returned;
            if (inserted <= 0L) return false;
            IAEFluidStack kept = stored.copy();
            kept.setStackSize(current - inserted);
            fluidTanks.setFluidInSlot(slot, kept);
            return true;
        }
        return false;
    }

    private boolean balanceGas(int slot, @Nullable GasStack configured,
        IMEMonitor<IAEGasStack> monitor) throws GridAccessException {
        if (configured == null) return false;
        GasStack stored = gasTanks.getGasStack(slot);
        long current = stored == null ? 0L : stored.amount;
        long target = configured.amount;
        if (current < target) {
            GasStack gasRequest = configured.copy();
            gasRequest.amount = (int) (target - current);
            IAEGasStack request = AEGasStack.of(gasRequest);
            if (request == null) return false;
            IAEGasStack extracted = Platform.poweredExtraction(
                proxy.getEnergy(), monitor, request, source);
            if (extracted == null || extracted.getStackSize() <= 0L) {
                return false;
            }
            GasStack pulled = extracted.getGasStack();
            if (stored == null) gasTanks.setGas(slot, pulled);
            else {
                GasStack combined = stored.copy();
                combined.amount += pulled.amount;
                gasTanks.setGas(slot, combined);
            }
            return true;
        }
        if (current > target) {
            GasStack excessGas = stored.copy();
            excessGas.amount = (int) (current - target);
            IAEGasStack excess = AEGasStack.of(excessGas);
            if (excess == null) return false;
            IAEGasStack remainder = Platform.poweredInsert(
                proxy.getEnergy(), monitor, excess, source);
            long returned = remainder == null ? 0L
                : remainder.getStackSize();
            long inserted = excess.getStackSize() - returned;
            if (inserted <= 0L) return false;
            GasStack kept = stored.copy();
            kept.amount -= (int) inserted;
            gasTanks.setGas(slot, kept.amount <= 0 ? null : kept);
            return true;
        }
        return false;
    }

    private ItemStack insertItem(IMEMonitor<IAEItemStack> monitor,
                                 ItemStack stack)
        throws GridAccessException {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        IAEItemStack request = channel.createStack(stack);
        if (request == null) return stack;
        request.setStackSize(stack.getCount());
        IAEItemStack remainder = Platform.poweredInsert(proxy.getEnergy(),
            monitor, request, source);
        return remainder == null ? ItemStack.EMPTY
            : remainder.createItemStack();
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        return !left.isEmpty() && !right.isEmpty()
            && left.isItemEqual(right)
            && ItemStack.areItemStackTagsEqual(left, right);
    }

    private void refreshDisplay() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) refreshDisplay(slot);
    }

    private void refreshDisplay(int slot) {
        ItemStack display = inventory.getStackInSlot(slot).copy();
        if (display.isEmpty()) {
            IAEFluidStack fluid = fluidTanks.getFluidInSlot(slot);
            if (fluid != null && fluid.getStackSize() > 0L) {
                display = MixedMEInputMarker.fluid(fluid.getFluidStack());
            } else {
                GasStack gas = gasTanks.getGasStack(slot);
                if (gas != null && gas.amount > 0) {
                    display = MixedMEInputMarker.gas(gas);
                }
            }
        }
        ItemStack old = displayInventory.getStackInSlot(slot);
        if (!ItemStack.areItemStacksEqual(old, display)) {
            displayInventory.setStackInSlot(slot, display);
        }
    }

    @Override
    public Collection<MachineComponent<?>> provideComponents() {
        return Arrays.<MachineComponent<?>>asList(fluidComponent, gasComponent);
    }

    @Override
    public boolean hasCapability(Capability<?> capability,
                                 @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || capability == Capabilities.GAS_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability,
                               @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                .cast(fluidTanks);
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(gasHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void onFluidInventoryChanged(IAEFluidTank inventory, int slot) {
        refreshDisplay(slot);
        if (!inTick) wakeGridTicking();
        markDirty();
    }

    @Override
    public void onGasInventoryChanged(IGasInventory inventory, int slot) {
        refreshDisplay(slot);
        if (!inTick) wakeGridTicking();
        markDirty();
    }

    private void wakeGridTicking() {
        if (getWorld() == null || getWorld().isRemote) return;
        try {
            IGridNode node = proxy.getNode();
            if (node != null) proxy.getTick().alertDevice(node);
        } catch (GridAccessException ignored) { }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        fluidTanks.readFromNBT(compound, TAG_FLUIDS);
        if (compound.hasKey(TAG_GASES, 10)) {
            gasTanks.load(compound.getCompoundTag(TAG_GASES));
        }
        fluidTanks.setCapacity(CAPACITY);
        gasTanks.setCap(CAPACITY);
        refreshDisplay();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        fluidTanks.writeToNBT(compound, TAG_FLUIDS);
        compound.setTag(TAG_GASES, gasTanks.save());
    }

    public void writeDropNBT(NBTTagCompound compound) {
        compound.setTag("inventory", getInternalInventory().writeNBT());
        compound.setTag("configInventory", getConfigInventory().writeNBT());
        fluidTanks.writeToNBT(compound, TAG_FLUIDS);
        compound.setTag(TAG_GASES, gasTanks.save());
    }

    public void readDropNBT(NBTTagCompound compound) {
        if (compound.hasKey("inventory", 10)) {
            readInventoryNBT(compound.getCompoundTag("inventory"));
        }
        if (compound.hasKey("configInventory", 10)) {
            readConfigInventoryNBT(
                compound.getCompoundTag("configInventory"));
        }
        fluidTanks.readFromNBT(compound, TAG_FLUIDS);
        if (compound.hasKey(TAG_GASES, 10)) {
            gasTanks.load(compound.getCompoundTag(TAG_GASES));
        }
        fluidTanks.setCapacity(CAPACITY);
        gasTanks.setCap(CAPACITY);
        refreshDisplay();
    }
}
