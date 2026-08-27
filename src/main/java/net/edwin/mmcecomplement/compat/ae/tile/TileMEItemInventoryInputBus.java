package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.concurrent.locks.Lock;

/**
 * Item input bus whose configuration slots select types rather than amounts.
 * Every selected type is pulled up to the full integer-sized slot capacity.
 */
public class TileMEItemInventoryInputBus extends MEItemInputBus
    implements MEInventoryInputBus {

    public static final String TAG_ACTIVE_PULL = "inventoryActivePull";

    private boolean activePull;
    private long permanentReserve;

    @Override
    public IOInventory buildConfigInventory() {
        return InventoryMarkerUtil.buildItemMarkers(this);
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS);
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
            !activePull && !hasBufferedItems(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node,
                                              int ticksSinceLastCall) {
        if (!proxy.isActive()) return TickRateModulation.IDLE;
        Lock writeLock = inventory.getRWLock().writeLock();
        writeLock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEItemStack> monitor =
                proxy.getStorage().getInventory(channel);
            boolean changed = syncConfiguredItems(monitor, activePull);
            if (!activePull && !hasBufferedItems()) {
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

    private boolean syncConfiguredItems(IMEMonitor<IAEItemStack> monitor,
                                        boolean allowPull)
        throws GridAccessException {
        boolean changed = false;
        IOInventory config = getConfigInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack configured = slot < config.getSlots()
                ? config.getStackInSlot(slot) : ItemStack.EMPTY;
            ItemStack stored = inventory.getStackInSlot(slot);

            if (!stored.isEmpty()
                && (configured.isEmpty() || !sameType(stored, configured))) {
                ItemStack remainder = insertToNetwork(monitor, stored);
                if (remainder.getCount() != stored.getCount()) changed = true;
                inventory.setStackInSlot(slot, remainder);
                stored = remainder;
            }
            if (!allowPull || configured.isEmpty()
                || (!stored.isEmpty() && !sameType(stored, configured))) {
                continue;
            }

            int room = stored.isEmpty() ? Integer.MAX_VALUE
                : Integer.MAX_VALUE - stored.getCount();
            if (room <= 0) continue;
            IAEItemStack request = channel.createStack(configured);
            if (request == null) continue;
            IAEItemStack available = monitor.getStorageList()
                .findPrecise(request);
            long networkAmount = available == null ? 0L
                : available.getStackSize();
            long amount = InventoryReserveUtil.extractable(networkAmount,
                permanentReserve, room);
            if (amount <= 0L) continue;
            request.setStackSize(amount);
            IAEItemStack extracted = Platform.poweredExtraction(
                proxy.getEnergy(), monitor, request, source);
            if (extracted == null || extracted.getStackSize() <= 0L) continue;
            ItemStack pulled = extracted.createItemStack();
            if (pulled.isEmpty()) continue;
            if (stored.isEmpty()) {
                inventory.setStackInSlot(slot, pulled);
            } else {
                stored.grow(pulled.getCount());
                inventory.setStackInSlot(slot, stored);
            }
            changed = true;
        }
        return changed;
    }

    private ItemStack insertToNetwork(IMEMonitor<IAEItemStack> monitor,
                                      ItemStack stack)
        throws GridAccessException {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        IAEItemStack request = channel.createStack(stack);
        if (request == null) return stack;
        request.setStackSize(stack.getCount());
        IAEItemStack remainder = Platform.poweredInsert(
            proxy.getEnergy(), monitor, request, source);
        return remainder == null ? ItemStack.EMPTY
            : remainder.createItemStack();
    }

    private void pushAllToNetwork() {
        if (!proxy.isActive()) return;
        Lock writeLock = inventory.getRWLock().writeLock();
        writeLock.lock();
        inTick = true;
        try {
            IMEMonitor<IAEItemStack> monitor =
                proxy.getStorage().getInventory(channel);
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stored = inventory.getStackInSlot(slot);
                if (!stored.isEmpty()) {
                    inventory.setStackInSlot(slot,
                        insertToNetwork(monitor, stored));
                }
            }
        } catch (GridAccessException ignored) {
        } finally {
            inTick = false;
            writeLock.unlock();
        }
    }

    private boolean hasBufferedItems() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) return true;
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

    private static boolean sameType(ItemStack left, ItemStack right) {
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
            compound.getLong(TAG_PERMANENT_RESERVE));
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setBoolean(TAG_ACTIVE_PULL, activePull);
        compound.setLong(TAG_PERMANENT_RESERVE, permanentReserve);
    }
}
