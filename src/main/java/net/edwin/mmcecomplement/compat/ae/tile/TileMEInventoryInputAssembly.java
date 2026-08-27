package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.fluids.util.AEFluidInventory;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import com.mekeng.github.common.me.inventory.impl.GasInventory;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Lock;

/**
 * Mixed sixteen-slot variant of the three inventory input buses.  Markers are
 * type-only and the active switch controls whether the selected resources are
 * pulled from the ME network.
 */
public class TileMEInventoryInputAssembly extends TileMEInputAssembly
    implements MEInventoryInputBus {

    public static final String TAG_ACTIVE_PULL = "inventoryActivePull";
    private boolean activePull;
    private long permanentReserve;

    private final IFluidStorageChannel fluidChannel = appeng.api.AEApi.instance()
        .storage().getStorageChannel(IFluidStorageChannel.class);
    private final IGasStorageChannel gasChannel = appeng.api.AEApi.instance()
        .storage().getStorageChannel(IGasStorageChannel.class);

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY);
    }

    @Override
    public void setMarker(int slot, ItemStack marker) {
        super.setMarker(slot, marker.isEmpty()
            ? ItemStack.EMPTY : MixedMEInputMarker.withAmount(
                MixedMEInputMarker.sanitize(marker), 1L));
    }

    @Override
    public boolean isActivePull() {
        return activePull;
    }

    @Override
    public void setActivePull(boolean value) {
        if (activePull == value) return;
        activePull = value;
        if (!value && getWorld() != null && !getWorld().isRemote) {
            pushAllToNetwork();
        }
        markNoUpdate();
        wakeGridTicking();
    }

    @Override
    public void setClientActivePull(boolean value) {
        if (getWorld() != null && getWorld().isRemote) activePull = value;
    }

    @Override
    public long getPermanentReserve() {
        return permanentReserve;
    }

    @Override
    public void setPermanentReserve(long value) {
        long updated = InventoryReserveUtil.clamp(value);
        if (permanentReserve == updated) return;
        permanentReserve = updated;
        markNoUpdate();
        wakeGridTicking();
    }

    @Override
    public void setClientPermanentReserve(long value) {
        if (getWorld() != null && getWorld().isRemote) {
            permanentReserve = InventoryReserveUtil.clamp(value);
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 120,
            !activePull && !hasBufferedContents(), true);
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
            boolean changed = synchronize(items, fluids, gases);
            if (!activePull && !hasBufferedContents()) {
                return TickRateModulation.SLEEP;
            }
            return changed ? TickRateModulation.FASTER
                : TickRateModulation.SLOWER;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            inTick = false;
            lock.unlock();
        }
    }

    private boolean synchronize(IMEMonitor<IAEItemStack> items,
                                IMEMonitor<IAEFluidStack> fluids,
                                IMEMonitor<IAEGasStack> gases)
        throws GridAccessException {
        boolean changed = false;
        IOInventory config = getConfigInventory();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack marker = config.getStackInSlot(slot);
            int type = MixedMEInputMarker.getType(marker);
            changed |= returnWrongItem(slot, type, marker, items);
            changed |= returnWrongFluid(slot, type, marker, fluids);
            changed |= returnWrongGas(slot, type, marker, gases);
            if (!activePull) continue;
            if (type == MixedMEInputMarker.TYPE_ITEM) {
                changed |= pullItem(slot, marker, items);
            } else if (type == MixedMEInputMarker.TYPE_FLUID) {
                changed |= pullFluid(slot, MixedMEInputMarker.getFluid(marker),
                    fluids);
            } else if (type == MixedMEInputMarker.TYPE_GAS) {
                changed |= pullGas(slot, MixedMEInputMarker.getGas(marker),
                    gases);
            }
        }
        return changed;
    }

    private boolean returnWrongItem(int slot, int type, ItemStack marker,
                                    IMEMonitor<IAEItemStack> monitor)
        throws GridAccessException {
        ItemStack stored = getInternalInventory().getStackInSlot(slot);
        if (stored.isEmpty() || (type == MixedMEInputMarker.TYPE_ITEM
            && sameItem(stored, marker))) return false;
        ItemStack remainder = insertItem(monitor, stored);
        getInternalInventory().setStackInSlot(slot, remainder);
        return remainder.getCount() != stored.getCount();
    }

    private boolean returnWrongFluid(int slot, int type, ItemStack marker,
                                     IMEMonitor<IAEFluidStack> monitor)
        throws GridAccessException {
        IAEFluidStack stored = getFluidTanks().getFluidInSlot(slot);
        FluidStack configured = MixedMEInputMarker.getFluid(marker);
        if (stored == null || (type == MixedMEInputMarker.TYPE_FLUID
            && configured != null
            && stored.getFluidStack().isFluidEqual(configured))) return false;
        IAEFluidStack remainder = Platform.poweredInsert(proxy.getEnergy(),
            monitor, stored.copy(), source);
        getFluidTanks().setFluidInSlot(slot, remainder);
        return remainder == null
            || remainder.getStackSize() != stored.getStackSize();
    }

    private boolean returnWrongGas(int slot, int type, ItemStack marker,
                                   IMEMonitor<IAEGasStack> monitor)
        throws GridAccessException {
        GasStack stored = getGasTanks().getGasStack(slot);
        GasStack configured = MixedMEInputMarker.getGas(marker);
        if (stored == null || (type == MixedMEInputMarker.TYPE_GAS
            && configured != null && stored.isGasEqual(configured))) {
            return false;
        }
        IAEGasStack request = AEGasStack.of(stored.copy());
        if (request == null) return false;
        IAEGasStack remainder = Platform.poweredInsert(proxy.getEnergy(),
            monitor, request, source);
        getGasTanks().setGas(slot,
            remainder == null ? null : remainder.getGasStack());
        return remainder == null || remainder.getStackSize() != stored.amount;
    }

    private boolean pullItem(int slot, ItemStack marker,
                             IMEMonitor<IAEItemStack> monitor)
        throws GridAccessException {
        if (marker.isEmpty()) return false;
        ItemStack stored = getInternalInventory().getStackInSlot(slot);
        long current = stored.isEmpty() ? 0L : stored.getCount();
        long room = Integer.MAX_VALUE - current;
        IAEItemStack request = channel.createStack(marker);
        if (request == null) return false;
        IAEItemStack available = monitor.getStorageList().findPrecise(request);
        long networkAmount = available == null ? 0L
            : available.getStackSize();
        long amount = InventoryReserveUtil.extractable(networkAmount,
            permanentReserve, room);
        if (amount <= 0L) return false;
        request.setStackSize(amount);
        IAEItemStack extracted = Platform.poweredExtraction(
            proxy.getEnergy(), monitor, request, source);
        if (extracted == null || extracted.getStackSize() <= 0L) return false;
        ItemStack pulled = extracted.createItemStack();
        if (stored.isEmpty()) getInternalInventory().setStackInSlot(slot, pulled);
        else {
            stored.grow(pulled.getCount());
            getInternalInventory().setStackInSlot(slot, stored);
        }
        return true;
    }

    private boolean pullFluid(int slot, @Nullable FluidStack configured,
                              IMEMonitor<IAEFluidStack> monitor)
        throws GridAccessException {
        if (configured == null) return false;
        IAEFluidStack request = fluidChannel.createStack(configured);
        if (request == null) return false;
        IAEFluidStack available = monitor.getStorageList().findPrecise(request);
        long networkAmount = available == null ? 0L
            : available.getStackSize();
        long current = getFluidTanks().getFluidInSlot(slot) == null ? 0L
            : getFluidTanks().getFluidInSlot(slot).getStackSize();
        long amount = InventoryReserveUtil.extractable(networkAmount,
            permanentReserve, Integer.MAX_VALUE - current);
        if (amount <= 0L) return false;
        request.setStackSize(amount);
        IAEFluidStack extracted = Platform.poweredExtraction(
            proxy.getEnergy(), monitor, request, source);
        if (extracted == null || extracted.getStackSize() <= 0L) return false;
        IAEFluidStack stored = getFluidTanks().getFluidInSlot(slot);
        if (stored == null) getFluidTanks().setFluidInSlot(slot, extracted);
        else {
            IAEFluidStack combined = stored.copy();
            combined.setStackSize(current + extracted.getStackSize());
            getFluidTanks().setFluidInSlot(slot, combined);
        }
        return true;
    }

    private boolean pullGas(int slot, @Nullable GasStack configured,
                            IMEMonitor<IAEGasStack> monitor)
        throws GridAccessException {
        if (configured == null) return false;
        GasStack requested = configured.copy();
        IAEGasStack request = AEGasStack.of(requested);
        if (request == null) return false;
        IAEGasStack available = monitor.getStorageList().findPrecise(request);
        long networkAmount = available == null ? 0L
            : available.getStackSize();
        GasStack stored = getGasTanks().getGasStack(slot);
        long current = stored == null ? 0L : stored.amount;
        long amount = InventoryReserveUtil.extractable(networkAmount,
            permanentReserve, Integer.MAX_VALUE - current);
        if (amount <= 0L) return false;
        request.setStackSize(amount);
        IAEGasStack extracted = Platform.poweredExtraction(
            proxy.getEnergy(), monitor, request, source);
        if (extracted == null || extracted.getStackSize() <= 0L) return false;
        GasStack pulled = extracted.getGasStack();
        if (stored == null) getGasTanks().setGas(slot, pulled);
        else {
            GasStack combined = stored.copy();
            combined.amount += pulled.amount;
            getGasTanks().setGas(slot, combined);
        }
        return true;
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

    private void pushAllToNetwork() {
        if (!proxy.isActive()) return;
        try {
            IMEMonitor<IAEItemStack> items =
                proxy.getStorage().getInventory(channel);
            IMEMonitor<IAEFluidStack> fluids =
                proxy.getStorage().getInventory(fluidChannel);
            IMEMonitor<IAEGasStack> gases =
                proxy.getStorage().getInventory(gasChannel);
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                ItemStack item = getInternalInventory().getStackInSlot(slot);
                if (!item.isEmpty()) {
                    getInternalInventory().setStackInSlot(slot,
                        insertItem(items, item));
                }
                IAEFluidStack fluid = getFluidTanks().getFluidInSlot(slot);
                if (fluid != null) getFluidTanks().setFluidInSlot(slot,
                    Platform.poweredInsert(proxy.getEnergy(), fluids,
                        fluid.copy(), source));
                GasStack gas = getGasTanks().getGasStack(slot);
                if (gas != null) {
                    IAEGasStack request = AEGasStack.of(gas.copy());
                    IAEGasStack remainder = request == null ? null
                        : Platform.poweredInsert(proxy.getEnergy(), gases,
                            request, source);
                    getGasTanks().setGas(slot,
                        remainder == null ? null : remainder.getGasStack());
                }
            }
        } catch (GridAccessException ignored) { }
    }

    private boolean hasBufferedContents() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!getInternalInventory().getStackInSlot(slot).isEmpty()) return true;
            IAEFluidStack fluid = getFluidTanks().getFluidInSlot(slot);
            if (fluid != null && fluid.getStackSize() > 0L) return true;
            GasStack gas = getGasTanks().getGasStack(slot);
            if (gas != null && gas.amount > 0) return true;
        }
        return false;
    }

    private void wakeGridTicking() {
        if (getWorld() == null || getWorld().isRemote) return;
        try {
            IGridNode node = proxy.getNode();
            if (node != null) proxy.getTick().alertDevice(node);
        } catch (GridAccessException ignored) { }
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        return !left.isEmpty() && !right.isEmpty()
            && left.isItemEqual(right)
            && ItemStack.areItemStackTagsEqual(left, right);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        InventoryMarkerUtil.normalizeItemMarkers(getConfigInventory());
        activePull = compound.getBoolean(TAG_ACTIVE_PULL);
        permanentReserve = InventoryReserveUtil.clamp(
            compound.getLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setBoolean(TAG_ACTIVE_PULL, activePull);
        compound.setLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE,
            permanentReserve);
    }

    public void writeDropNBT(NBTTagCompound compound) {
        compound.setTag("inventory", getInternalInventory().writeNBT());
        compound.setTag("configInventory", getConfigInventory().writeNBT());
        ((AEFluidInventory) getFluidTanks()).writeToNBT(compound,
            "mixedFluidTanks");
        compound.setTag("mixedGasTanks", ((GasInventory) getGasTanks()).save());
        compound.setBoolean(TAG_ACTIVE_PULL, activePull);
        compound.setLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE,
            permanentReserve);
    }

    public void readDropNBT(NBTTagCompound compound) {
        if (compound.hasKey("inventory", 10)) {
            readInventoryNBT(compound.getCompoundTag("inventory"));
        }
        if (compound.hasKey("configInventory", 10)) {
            readConfigInventoryNBT(compound.getCompoundTag("configInventory"));
        }
        if (compound.hasKey("mixedFluidTanks", 10)) {
            ((AEFluidInventory) getFluidTanks()).readFromNBT(compound,
                "mixedFluidTanks");
        }
        if (compound.hasKey("mixedGasTanks", 10)) {
            ((GasInventory) getGasTanks()).load(
                compound.getCompoundTag("mixedGasTanks"));
        }
        activePull = compound.getBoolean(TAG_ACTIVE_PULL);
        permanentReserve = InventoryReserveUtil.clamp(
            compound.getLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE));
    }
}
