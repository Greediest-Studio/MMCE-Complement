package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.fluids.util.IAEFluidTank;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import github.kasuminova.mmce.common.tile.MEFluidInputBus;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

import java.util.concurrent.locks.Lock;

/** Fluid variant of the inventory input bus. */
public class TileMEFluidInventoryInputBus extends MEFluidInputBus
    implements MEInventoryInputBus {

    private static final String TAG_ACTIVE_PULL = "inventoryActivePull";
    private static final int INVENTORY_CAPACITY = Integer.MAX_VALUE;

    private boolean activePull;
    private long permanentReserve;
    private boolean normalizingMarker;

    public TileMEFluidInventoryInputBus() {
        tanks.setCapacity(INVENTORY_CAPACITY);
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS);
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
            !activePull && !hasBufferedFluids(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node,
                                              int ticksSinceLastCall) {
        if (!proxy.isActive()) return TickRateModulation.IDLE;
        Lock writeLock = tanks.getRWLock().writeLock();
        writeLock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEFluidStack> monitor =
                proxy.getStorage().getInventory(channel);
            boolean changed = syncConfiguredFluids(monitor, activePull);
            if (!activePull && !hasBufferedFluids()) {
                return TickRateModulation.SLEEP;
            }
            return changed ? TickRateModulation.FASTER
                : TickRateModulation.SLOWER;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            inTick = false;
            writeLock.unlock();
        }
    }

    private boolean syncConfiguredFluids(IMEMonitor<IAEFluidStack> monitor,
                                         boolean allowPull)
        throws GridAccessException {
        boolean changed = false;
        for (int slot = 0; slot < tanks.getSlots(); slot++) {
            IAEFluidStack configured = getConfig().getFluidInSlot(slot);
            IAEFluidStack stored = tanks.getFluidInSlot(slot);

            if (stored != null
                && (configured == null || !sameType(stored, configured))) {
                IAEFluidStack remainder = insertToNetwork(monitor, stored);
                if (remainder == null
                    || remainder.getStackSize() != stored.getStackSize()) {
                    changed = true;
                }
                tanks.setFluidInSlot(slot, remainder);
                stored = remainder;
            }
            if (!allowPull || configured == null
                || (stored != null && !sameType(stored, configured))) {
                continue;
            }

            long storedAmount = stored == null ? 0L : stored.getStackSize();
            long room = INVENTORY_CAPACITY - storedAmount;
            if (room <= 0L) continue;
            IAEFluidStack request = configured.copy();
            IAEFluidStack available = monitor.getStorageList()
                .findPrecise(request);
            long networkAmount = available == null ? 0L
                : available.getStackSize();
            long amount = InventoryReserveUtil.extractable(networkAmount,
                permanentReserve, room);
            if (amount <= 0L) continue;
            request.setStackSize(amount);
            IAEFluidStack extracted = Platform.poweredExtraction(
                proxy.getEnergy(), monitor, request, source);
            if (extracted == null || extracted.getStackSize() <= 0L) continue;
            if (stored == null) {
                tanks.setFluidInSlot(slot, extracted);
            } else {
                IAEFluidStack combined = stored.copy();
                combined.setStackSize(storedAmount + extracted.getStackSize());
                tanks.setFluidInSlot(slot, combined);
            }
            changed = true;
        }
        return changed;
    }

    private IAEFluidStack insertToNetwork(IMEMonitor<IAEFluidStack> monitor,
                                          IAEFluidStack stack)
        throws GridAccessException {
        return Platform.poweredInsert(proxy.getEnergy(), monitor,
            stack.copy(), source);
    }

    private void pushAllToNetwork() {
        if (!proxy.isActive()) return;
        Lock writeLock = tanks.getRWLock().writeLock();
        writeLock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEFluidStack> monitor =
                proxy.getStorage().getInventory(channel);
            for (int slot = 0; slot < tanks.getSlots(); slot++) {
                IAEFluidStack stored = tanks.getFluidInSlot(slot);
                if (stored != null) {
                    tanks.setFluidInSlot(slot,
                        insertToNetwork(monitor, stored));
                }
            }
        } catch (GridAccessException ignored) {
        } finally {
            inTick = false;
            writeLock.unlock();
        }
    }

    private boolean hasBufferedFluids() {
        for (int slot = 0; slot < tanks.getSlots(); slot++) {
            IAEFluidStack stack = tanks.getFluidInSlot(slot);
            if (stack != null && stack.getStackSize() > 0L) return true;
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

    private static boolean sameType(IAEFluidStack left,
                                    IAEFluidStack right) {
        return left != null && right != null && left.equals(right);
    }

    @Override
    public synchronized void onFluidInventoryChanged(IAEFluidTank inventory,
                                                     int slot) {
        if (inventory == getConfig() && !normalizingMarker) {
            IAEFluidStack marker = getConfig().getFluidInSlot(slot);
            if (marker != null && marker.getStackSize() != 1L) {
                normalizingMarker = true;
                try {
                    IAEFluidStack normalized = marker.copy();
                    normalized.setStackSize(1L);
                    getConfig().setFluidInSlot(slot, normalized);
                } finally {
                    normalizingMarker = false;
                }
            }
        }
        super.onFluidInventoryChanged(inventory, slot);
    }

    private void normalizeMarkers() {
        for (int slot = 0; slot < getConfig().getSlots(); slot++) {
            IAEFluidStack marker = getConfig().getFluidInSlot(slot);
            if (marker != null && marker.getStackSize() != 1L) {
                IAEFluidStack normalized = marker.copy();
                normalized.setStackSize(1L);
                getConfig().setFluidInSlot(slot, normalized);
            }
        }
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot,
                                  InvOperation operation, ItemStack removed,
                                  ItemStack added) {
        super.onChangeInventory(inventory, slot, operation, removed, added);
        tanks.setCapacity(INVENTORY_CAPACITY);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        tanks.setCapacity(INVENTORY_CAPACITY);
        normalizeMarkers();
        activePull = compound.getBoolean(TAG_ACTIVE_PULL);
        permanentReserve = InventoryReserveUtil.clamp(
            compound.getLong(TAG_PERMANENT_RESERVE));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setBoolean(TAG_ACTIVE_PULL, activePull);
        compound.setLong(TAG_PERMANENT_RESERVE, permanentReserve);
    }
}
