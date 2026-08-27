package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.concurrent.locks.Lock;

/**
 * Unfiltered mixed inventory assembly.  While enabled it exposes up to sixteen
 * resource entries from the connected ME network and pulls those entries into
 * its machine-facing buffers.
 */
public class TileMEFullExposureAssembly extends TileMEInventoryInputAssembly {

    private final IFluidStorageChannel fluidChannel = AEApi.instance()
        .storage().getStorageChannel(IFluidStorageChannel.class);
    private final IGasStorageChannel gasChannel = AEApi.instance()
        .storage().getStorageChannel(IGasStorageChannel.class);

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
    }

    /** This assembly deliberately has no marker/filter slots. */
    @Override
    public void setMarker(int slot, ItemStack marker) { }

    /** Prevent memory-card transfers from smuggling filter settings in. */
    @Override
    public NBTTagCompound downloadSettings() {
        return new NBTTagCompound();
    }

    @Override
    public void uploadSettings(NBTTagCompound ignored) { }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 120,
            !isActivePull() && !hasBufferedContents(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node,
                                              int ticksSinceLastCall) {
        if (!getProxy().isActive()) return TickRateModulation.IDLE;
        Lock lock = getInternalInventory().getRWLock().writeLock();
        lock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEItemStack> items =
                getProxy().getStorage().getInventory(channel);
            IMEMonitor<IAEFluidStack> fluids =
                getProxy().getStorage().getInventory(fluidChannel);
            IMEMonitor<IAEGasStack> gases =
                getProxy().getStorage().getInventory(gasChannel);
            boolean changed;
            if (isActivePull()) {
                changed = synchronizeExisting(items, fluids, gases);
                changed |= fillEmptySlots(items, fluids, gases);
            } else {
                changed = pushAllToNetwork(items, fluids, gases);
            }
            if (!isActivePull() && !hasBufferedContents()) {
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

    private boolean synchronizeExisting(IMEMonitor<IAEItemStack> items,
                                        IMEMonitor<IAEFluidStack> fluids,
                                        IMEMonitor<IAEGasStack> gases)
        throws GridAccessException {
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack item = getInternalInventory().getStackInSlot(slot);
            if (!item.isEmpty()) {
                IAEItemStack request = channel.createStack(item);
                if (request != null) {
                    IAEItemStack available = items.getStorageList()
                        .findPrecise(request);
                    long amount = available == null ? 0L
                        : available.getStackSize();
                    long room = Integer.MAX_VALUE - item.getCount();
                    amount = InventoryReserveUtil.extractable(amount,
                        getPermanentReserve(), room);
                    if (amount > 0L) {
                        request.setStackSize(amount);
                        IAEItemStack extracted = Platform.poweredExtraction(
                            getProxy().getEnergy(), items, request, source);
                        if (extracted != null && extracted.getStackSize() > 0L) {
                            item.grow((int) extracted.getStackSize());
                            getInternalInventory().setStackInSlot(slot, item);
                            changed = true;
                        }
                    }
                }
                continue;
            }
            IAEFluidStack fluid = getFluidTanks().getFluidInSlot(slot);
            if (fluid != null) {
                IAEFluidStack available = fluids.getStorageList()
                    .findPrecise(fluid);
                if (available != null && available.getStackSize() > 0L) {
                    long room = Integer.MAX_VALUE - fluid.getStackSize();
                    long amount = InventoryReserveUtil.extractable(
                        available.getStackSize(), getPermanentReserve(), room);
                    if (amount <= 0L) continue;
                    IAEFluidStack request = fluid.copy();
                    request.setStackSize(amount);
                    IAEFluidStack extracted = Platform.poweredExtraction(
                        getProxy().getEnergy(), fluids, request, source);
                    if (extracted != null && extracted.getStackSize() > 0L) {
                        fluid.setStackSize(fluid.getStackSize()
                            + extracted.getStackSize());
                        getFluidTanks().setFluidInSlot(slot, fluid);
                        changed = true;
                    }
                }
                continue;
            }
            GasStack gas = getGasTanks().getGasStack(slot);
            if (gas != null) {
                IAEGasStack request = AEGasStack.of(gas.copy());
                if (request != null) {
                    IAEGasStack available = gases.getStorageList()
                        .findPrecise(request);
                    long amount = available == null ? 0L
                        : available.getStackSize();
                    long room = Integer.MAX_VALUE - gas.amount;
                    amount = InventoryReserveUtil.extractable(amount,
                        getPermanentReserve(), room);
                    if (amount > 0L) {
                        request.setStackSize(amount);
                        IAEGasStack extracted = Platform.poweredExtraction(
                            getProxy().getEnergy(), gases, request, source);
                        if (extracted != null && extracted.getStackSize() > 0L) {
                            gas.amount += (int) extracted.getStackSize();
                            getGasTanks().setGas(slot, gas);
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    private boolean fillEmptySlots(IMEMonitor<IAEItemStack> items,
                                    IMEMonitor<IAEFluidStack> fluids,
                                    IMEMonitor<IAEGasStack> gases)
        throws GridAccessException {
        boolean changed = false;
        for (IAEItemStack candidate : items.getStorageList()) {
            int slot = findEmptySlot();
            if (slot < 0) return changed;
            ItemStack stack = candidate.createItemStack();
            if (stack.isEmpty()) continue;
            IAEItemStack request = candidate.copy();
            long amount = InventoryReserveUtil.extractable(
                candidate.getStackSize(), getPermanentReserve(),
                Integer.MAX_VALUE);
            if (amount <= 0L) continue;
            request.setStackSize(amount);
            IAEItemStack extracted = Platform.poweredExtraction(
                getProxy().getEnergy(), items, request, source);
            if (extracted != null && extracted.getStackSize() > 0L) {
                getInternalInventory().setStackInSlot(slot,
                    extracted.createItemStack());
                changed = true;
            }
        }
        for (IAEFluidStack candidate : fluids.getStorageList()) {
            int slot = findEmptySlot();
            if (slot < 0) return changed;
            IAEFluidStack request = candidate.copy();
            long amount = InventoryReserveUtil.extractable(
                candidate.getStackSize(), getPermanentReserve(),
                Integer.MAX_VALUE);
            if (amount <= 0L) continue;
            request.setStackSize(amount);
            IAEFluidStack extracted = Platform.poweredExtraction(
                getProxy().getEnergy(), fluids, request, source);
            if (extracted != null && extracted.getStackSize() > 0L) {
                getFluidTanks().setFluidInSlot(slot, extracted);
                changed = true;
            }
        }
        for (IAEGasStack candidate : gases.getStorageList()) {
            int slot = findEmptySlot();
            if (slot < 0) return changed;
            IAEGasStack request = candidate.copy();
            long amount = InventoryReserveUtil.extractable(
                candidate.getStackSize(), getPermanentReserve(),
                Integer.MAX_VALUE);
            if (amount <= 0L) continue;
            request.setStackSize(amount);
            IAEGasStack extracted = Platform.poweredExtraction(
                getProxy().getEnergy(), gases, request, source);
            if (extracted != null && extracted.getStackSize() > 0L) {
                getGasTanks().setGas(slot, extracted.getGasStack());
                changed = true;
            }
        }
        return changed;
    }

    private int findEmptySlot() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (getInternalInventory().getStackInSlot(slot).isEmpty()
                && getFluidTanks().getFluidInSlot(slot) == null
                && getGasTanks().getGasStack(slot) == null) return slot;
        }
        return -1;
    }

    private boolean pushAllToNetwork(IMEMonitor<IAEItemStack> items,
                                     IMEMonitor<IAEFluidStack> fluids,
                                     IMEMonitor<IAEGasStack> gases)
        throws GridAccessException {
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack item = getInternalInventory().getStackInSlot(slot);
            if (!item.isEmpty()) {
                IAEItemStack request = channel.createStack(item);
                if (request != null) {
                    request.setStackSize(item.getCount());
                    IAEItemStack remainder = Platform.poweredInsert(
                        getProxy().getEnergy(), items, request, source);
                    ItemStack left = remainder == null ? ItemStack.EMPTY
                        : remainder.createItemStack();
                    changed |= left.getCount() != item.getCount();
                    getInternalInventory().setStackInSlot(slot, left);
                }
            }
            IAEFluidStack fluid = getFluidTanks().getFluidInSlot(slot);
            if (fluid != null) {
                IAEFluidStack remainder = Platform.poweredInsert(
                    getProxy().getEnergy(), fluids, fluid.copy(), source);
                changed |= remainder == null
                    || remainder.getStackSize() != fluid.getStackSize();
                getFluidTanks().setFluidInSlot(slot, remainder);
            }
            GasStack gas = getGasTanks().getGasStack(slot);
            if (gas != null) {
                IAEGasStack request = AEGasStack.of(gas.copy());
                IAEGasStack remainder = request == null ? null
                    : Platform.poweredInsert(getProxy().getEnergy(), gases,
                        request, source);
                changed |= remainder == null
                    || remainder.getStackSize() != gas.amount;
                getGasTanks().setGas(slot,
                    remainder == null ? null : remainder.getGasStack());
            }
        }
        return changed;
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
}
