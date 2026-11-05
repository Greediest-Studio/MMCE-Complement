package net.edwin.mmcecomplement.compat.ae.tile;

import net.edwin.mmcecomplement.compat.ae.AeNetworkCompat;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.inventory.IGasInventory;
import github.kasuminova.mmce.common.tile.MEGasInputBus;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

/** Gas variant of the inventory input bus, loaded only with MekEng. */
public class TileMEGasInventoryInputBus extends MEGasInputBus
    implements MEInventoryInputBus {

    private static final String TAG_ACTIVE_PULL = "inventoryActivePull";
    private static final int INVENTORY_CAPACITY = Integer.MAX_VALUE;

    private boolean activePull;
    private long permanentReserve;
    private boolean normalizingMarker;

    public TileMEGasInventoryInputBus() {
        tanks.setCap(INVENTORY_CAPACITY);
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_GAS_INVENTORY_INPUT_BUS == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_GAS_INVENTORY_INPUT_BUS);
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
            !activePull && !hasBufferedGas(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node,
                                              int ticksSinceLastCall) {
        if (!isActive()) return TickRateModulation.IDLE;
        inTick = true;
        try {
            IMEMonitor<IAEGasStack> monitor =
                AeNetworkCompat.inventory(this, channel);
            synchronized (tanks) {
                boolean changed = syncConfiguredGas(monitor, activePull);
                if (!activePull && !hasBufferedGas()) {
                    return TickRateModulation.SLEEP;
                }
                return changed ? TickRateModulation.FASTER
                    : TickRateModulation.SLOWER;
            }
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            inTick = false;
        }
    }

    private boolean syncConfiguredGas(IMEMonitor<IAEGasStack> monitor,
                                      boolean allowPull)
        throws GridAccessException {
        boolean changed = false;
        for (int slot = 0; slot < tanks.size(); slot++) {
            GasStack configured = getConfig().getGasStack(slot);
            GasStack stored = tanks.getGasStack(slot);

            if (stored != null
                && (configured == null || !stored.isGasEqual(configured))) {
                GasStack remainder = insertToNetwork(monitor, stored);
                if (remainder == null || remainder.amount != stored.amount) {
                    changed = true;
                }
                tanks.setGas(slot, remainder);
                stored = remainder;
            }
            if (!allowPull || configured == null
                || (stored != null && !stored.isGasEqual(configured))) {
                continue;
            }

            int storedAmount = stored == null ? 0 : stored.amount;
            int room = INVENTORY_CAPACITY - storedAmount;
            if (room <= 0) continue;
            GasStack requestedGas = configured.copy();
            requestedGas.amount = room;
            IAEGasStack request = AEGasStack.of(requestedGas);
            if (request == null) continue;
            IAEGasStack available = monitor.getStorageList()
                .findPrecise(request);
            long networkAmount = available == null ? 0L
                : available.getStackSize();
            long amount = InventoryReserveUtil.extractable(networkAmount,
                permanentReserve, room);
            if (amount <= 0L) continue;
            request.setStackSize(amount);
            IAEGasStack extracted = AeNetworkCompat.poweredExtraction(this,
                AeNetworkCompat.energy(this), monitor, request, source);
            if (extracted == null || extracted.getStackSize() <= 0L) continue;
            GasStack pulled = extracted.getGasStack();
            if (stored == null) {
                tanks.setGas(slot, pulled);
            } else {
                GasStack combined = stored.copy();
                combined.amount += pulled.amount;
                tanks.setGas(slot, combined);
            }
            changed = true;
        }
        return changed;
    }

    private GasStack insertToNetwork(IMEMonitor<IAEGasStack> monitor,
                                     GasStack stack)
        throws GridAccessException {
        IAEGasStack request = AEGasStack.of(stack.copy());
        if (request == null) return stack;
        IAEGasStack remainder = AeNetworkCompat.poweredInsert(this,
            AeNetworkCompat.energy(this), monitor, request, source);
        return remainder == null ? null : remainder.getGasStack();
    }

    private void pushAllToNetwork() {
        if (!isActive()) return;
        inTick = true;
        try {
            IMEMonitor<IAEGasStack> monitor =
                AeNetworkCompat.inventory(this, channel);
            synchronized (tanks) {
                for (int slot = 0; slot < tanks.size(); slot++) {
                    GasStack stored = tanks.getGasStack(slot);
                    if (stored != null) {
                        tanks.setGas(slot,
                            insertToNetwork(monitor, stored));
                    }
                }
            }
        } catch (GridAccessException ignored) {
        } finally {
            inTick = false;
        }
    }

    private boolean hasBufferedGas() {
        for (int slot = 0; slot < tanks.size(); slot++) {
            GasStack stack = tanks.getGasStack(slot);
            if (stack != null && stack.amount > 0) return true;
        }
        return false;
    }

    private void wakeGridTicking() {
        if (getWorld() == null || getWorld().isRemote) return;
        try {
            IGridNode node = AeNetworkCompat.getNode(this);
            if (node != null) AeNetworkCompat.alert(this);
        } catch (GridAccessException ignored) { }
    }

    @Override
    public void onGasInventoryChanged(IGasInventory inventory, int slot) {
        if (inventory == getConfig() && !normalizingMarker) {
            GasStack marker = getConfig().getGasStack(slot);
            if (marker != null && marker.amount != 1) {
                normalizingMarker = true;
                try {
                    GasStack normalized = marker.copy();
                    normalized.amount = 1;
                    getConfig().setGas(slot, normalized);
                } finally {
                    normalizingMarker = false;
                }
            }
        }
        super.onGasInventoryChanged(inventory, slot);
    }

    private void normalizeMarkers() {
        for (int slot = 0; slot < getConfig().size(); slot++) {
            GasStack marker = getConfig().getGasStack(slot);
            if (marker != null && marker.amount != 1) {
                GasStack normalized = marker.copy();
                normalized.amount = 1;
                getConfig().setGas(slot, normalized);
            }
        }
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot,
                                  InvOperation operation, ItemStack removed,
                                  ItemStack added) {
        // The base bus recalculates a small upgrade-based capacity and clamps
        // its contents. Inventory buses keep their full capacity at all times.
        tanks.setCap(INVENTORY_CAPACITY);
        markNoUpdateSync();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        // MEGasBus restores its upgrade capacity after loading, which may have
        // truncated a large inventory buffer. Restore from the unchanged save.
        tanks.load(compound.getCompoundTag("tanks"));
        tanks.setCap(INVENTORY_CAPACITY);
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
