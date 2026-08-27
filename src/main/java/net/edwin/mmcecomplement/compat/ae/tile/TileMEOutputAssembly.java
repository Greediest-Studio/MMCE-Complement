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
import github.kasuminova.mmce.common.tile.MEItemOutputBus;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.Lock;

/** Mixed sixteen-channel item/fluid/gas ME output assembly. */
public class TileMEOutputAssembly extends MEItemOutputBus
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
    private final github.kasuminova.mmce.common.util.GasInventoryHandler
        gasHandler = new github.kasuminova.mmce.common.util.GasInventoryHandler(
            gasTanks);
    private final IOInventory displayInventory = buildDisplayInventory();

    private final MachineComponent<net.minecraftforge.fluids.capability.IFluidHandler>
        fluidComponent = new MachineComponent.FluidHatch(IOType.OUTPUT) {
            @Override
            public net.minecraftforge.fluids.capability.IFluidHandler
            getContainerProvider() {
                return fluidTanks;
            }

            @Override
            public long getGroupID() {
                return TileMEOutputAssembly.this.getGroupId();
            }
        };

    private final MachineComponent<github.kasuminova.mmce.common.util.IExtendedGasHandler>
        gasComponent = new MachineComponent<github.kasuminova.mmce.common.util.IExtendedGasHandler>(
            IOType.OUTPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_GAS;
            }

            @Override
            public github.kasuminova.mmce.common.util.IExtendedGasHandler
            getContainerProvider() {
                return gasHandler;
            }

            @Override
            public long getGroupID() {
                return TileMEOutputAssembly.this.getGroupId();
            }
        };

    public TileMEOutputAssembly() {
        // MEItemOutputBus initializes its stack-size setting from its own
        // buildInventory implementation.  This assembly supplies a custom
        // sixteen-slot inventory, so initialize the inherited setting here
        // as well; otherwise a freshly loaded tile could inherit the field's
        // JVM default (zero) and reject all item output.
        setConfiguredStackSize(CAPACITY);
        inventory.setListener(slot -> {
            markNoUpdate();
            refreshDisplay(slot);
            if (!inTick) wakeGridTicking();
        });
    }

    @Override
    public IOInventory buildInventory() {
        int[] slots = new int[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) slots[slot] = slot;
        IOInventory result = new IOInventory(this, new int[0], slots);
        result.setStackLimit(CAPACITY, slots);
        return result;
    }

    private IOInventory buildDisplayInventory() {
        int[] slots = new int[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) slots[slot] = slot;
        IOInventory result = new IOInventory(this, slots, new int[0]);
        result.setStackLimit(CAPACITY, slots);
        return result;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_OUTPUT_ASSEMBLY == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_OUTPUT_ASSEMBLY);
    }

    public IOInventory getDisplayInventory() { return displayInventory; }
    public IAEFluidTank getFluidTanks() { return fluidTanks; }
    public IGasInventory getGasTanks() { return gasTanks; }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 120, !hasBufferedContents(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node,
                                              int ticksSinceLastCall) {
        if (!proxy.isActive()) return TickRateModulation.IDLE;
        Lock lock = getInternalInventory().getRWLock().writeLock();
        lock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEItemStack> items =
                proxy.getStorage().getInventory(channel);
            IMEMonitor<IAEFluidStack> fluids =
                proxy.getStorage().getInventory(fluidChannel);
            IMEMonitor<IAEGasStack> gases =
                proxy.getStorage().getInventory(gasChannel);
            boolean changed = transfer(items, fluids, gases);
            refreshDisplay();
            return changed ? TickRateModulation.FASTER
                : TickRateModulation.SLOWER;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            inTick = false;
            lock.unlock();
        }
    }

    private boolean transfer(IMEMonitor<IAEItemStack> items,
                             IMEMonitor<IAEFluidStack> fluids,
                             IMEMonitor<IAEGasStack> gases)
        throws GridAccessException {
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack item = getInternalInventory().getStackInSlot(slot);
            if (!item.isEmpty()) {
                ItemStack remainder = insertItem(items, item);
                changed |= remainder.getCount() != item.getCount();
                getInternalInventory().setStackInSlot(slot, remainder);
            }
            IAEFluidStack fluid = fluidTanks.getFluidInSlot(slot);
            if (fluid != null) {
                IAEFluidStack remainder = Platform.poweredInsert(
                    proxy.getEnergy(), fluids, fluid.copy(), source);
                changed |= remainder == null
                    || remainder.getStackSize() != fluid.getStackSize();
                fluidTanks.setFluidInSlot(slot, remainder);
            }
            GasStack gas = gasTanks.getGasStack(slot);
            if (gas != null) {
                IAEGasStack request = AEGasStack.of(gas.copy());
                IAEGasStack remainder = request == null ? null
                    : Platform.poweredInsert(proxy.getEnergy(), gases,
                        request, source);
                changed |= remainder == null
                    || remainder.getStackSize() != gas.amount;
                gasTanks.setGas(slot,
                    remainder == null ? null : remainder.getGasStack());
            }
        }
        return changed;
    }

    private ItemStack insertItem(IMEMonitor<IAEItemStack> monitor,
                                 ItemStack stack)
        throws GridAccessException {
        IAEItemStack request = channel.createStack(stack);
        if (request == null) return stack;
        request.setStackSize(stack.getCount());
        IAEItemStack remainder = Platform.poweredInsert(proxy.getEnergy(),
            monitor, request, source);
        return remainder == null ? ItemStack.EMPTY
            : remainder.createItemStack();
    }

    private boolean hasBufferedContents() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!getInternalInventory().getStackInSlot(slot).isEmpty()) return true;
            IAEFluidStack fluid = fluidTanks.getFluidInSlot(slot);
            if (fluid != null && fluid.getStackSize() > 0L) return true;
            GasStack gas = gasTanks.getGasStack(slot);
            if (gas != null && gas.amount > 0) return true;
        }
        return false;
    }

    private void refreshDisplay() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) refreshDisplay(slot);
    }

    private void refreshDisplay(int slot) {
        ItemStack display = getInternalInventory().getStackInSlot(slot).copy();
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
                                 EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || capability == mekanism.common.capabilities.Capabilities.GAS_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidTanks);
        }
        if (capability == mekanism.common.capabilities.Capabilities.GAS_HANDLER_CAPABILITY) {
            return mekanism.common.capabilities.Capabilities.GAS_HANDLER_CAPABILITY.cast(gasHandler);
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
        if (compound.hasKey(TAG_GASES, 10)) gasTanks.load(
            compound.getCompoundTag(TAG_GASES));
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
        fluidTanks.writeToNBT(compound, TAG_FLUIDS);
        compound.setTag(TAG_GASES, gasTanks.save());
    }

    public void readDropNBT(NBTTagCompound compound) {
        if (compound.hasKey("inventory", 10)) {
            readInventoryNBT(compound.getCompoundTag("inventory"));
        }
        if (compound.hasKey(TAG_FLUIDS, 10)) {
            fluidTanks.readFromNBT(compound, TAG_FLUIDS);
        }
        if (compound.hasKey(TAG_GASES, 10)) {
            gasTanks.load(compound.getCompoundTag(TAG_GASES));
        }
        refreshDisplay();
    }
}
