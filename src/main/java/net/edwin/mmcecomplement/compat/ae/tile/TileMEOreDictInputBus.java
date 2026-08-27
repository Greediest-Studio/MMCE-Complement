package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.edwin.mmcecomplement.compat.ae.filter.OreDictFilter;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ME item input bus which filters by ore-dict names and can dynamically
 * pull the sixteen largest matching ME stacks.
 */
public class TileMEOreDictInputBus extends MEItemInputBus {
    private static final String TAG_WHITELIST = "oreDictWhitelist";
    private static final String TAG_BLACKLIST = "oreDictBlacklist";
    private static final String TAG_ACTIVE = "oreDictActivePull";
    private static final String LEGACY_TAG_WHITELIST = "mineralWhitelist";
    private static final String LEGACY_TAG_BLACKLIST = "mineralBlacklist";
    private static final String LEGACY_TAG_ACTIVE = "mineralActivePull";

    private String whitelist = "";
    private String blacklist = "";
    private boolean activePull;
    private long permanentReserve;

    @Override
    public IOInventory buildConfigInventory() {
        return InventoryMarkerUtil.buildItemMarkers(this);
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_ORE_DICT_INPUT_BUS == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_ORE_DICT_INPUT_BUS);
    }

    public String getWhitelist() { return whitelist; }
    public String getBlacklist() { return blacklist; }
    public boolean isActivePull() { return activePull; }
    public long getPermanentReserve() { return permanentReserve; }

    public void setWhitelist(String value) {
        String updated = clamp(value);
        if (whitelist.equals(updated)) return;
        whitelist = updated;
        markNoUpdate();
        wakeGridTicking();
    }
    public void setBlacklist(String value) {
        String updated = clamp(value);
        if (blacklist.equals(updated)) return;
        blacklist = updated;
        markNoUpdate();
        wakeGridTicking();
    }
    public void setActivePull(boolean value) {
        if (activePull == value) return;
        activePull = value;
        if (!value && getWorld() != null && !getWorld().isRemote) {
            clearActiveConfiguration();
            pushAllToNetwork();
        }
        markNoUpdate();
        wakeGridTicking();
    }

    public void setPermanentReserve(long value) {
        long updated = InventoryReserveUtil.clamp(value);
        if (permanentReserve == updated) return;
        permanentReserve = updated;
        markNoUpdate();
        wakeGridTicking();
    }

    /** Applies the optimistic client-side button state while the server packet
     * is in flight; the server remains authoritative and persists the value. */
    public void setClientActivePull(boolean value) {
        if (getWorld() == null || !getWorld().isRemote) return;
        activePull = value;
        if (!value) clearActiveConfiguration();
    }

    public void setClientWhitelist(String value) {
        if (getWorld() != null && getWorld().isRemote) whitelist = clamp(value);
    }

    public void setClientBlacklist(String value) {
        if (getWorld() != null && getWorld().isRemote) blacklist = clamp(value);
    }

    public void setClientPermanentReserve(long value) {
        if (getWorld() != null && getWorld().isRemote) {
            permanentReserve = InventoryReserveUtil.clamp(value);
        }
    }

    /**
     * This device is allowed to sleep in AE2. Configuration changes do not
     * necessarily touch MMCE's changed-slot flags, so markNoUpdate alone may
     * leave it asleep until another grid event occurs.
     */
    private void wakeGridTicking() {
        if (getWorld() == null || getWorld().isRemote) return;
        try {
            if (proxy.getNode() != null) {
                proxy.getTick().alertDevice(proxy.getNode());
            }
        } catch (GridAccessException ignored) { }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        // An already-active bus restored from NBT must not enter the grid in a
        // sleeping state before it has had a chance to create its markers.
        return new TickingRequest(10, 120, !activePull, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!proxy.isActive()) return TickRateModulation.IDLE;
        try {
            IStorageGrid storage = proxy.getStorage();
            IMEMonitor<IAEItemStack> monitor = storage.getInventory(channel);
            if (activePull) {
                updateActiveConfiguration(monitor);
                // Marker counts are deliberately fixed at one. Use the
                // inventory transfer path so the marker amount never limits
                // how much of the selected type is pulled.
                syncConfiguredItems(monitor);
            } else {
                syncConfiguredItems(monitor);
            }
            return TickRateModulation.SLOWER;
        } catch (Exception ignored) {
            return TickRateModulation.IDLE;
        }
    }

    /**
     * Updates the ghost configuration slots used by MMCE's ME input bus.
     *
     * <p>The old implementation extracted directly into the backing item
     * inventory. Once an item had been extracted completely it disappeared
     * from the ME monitor, was dropped from the next top-sixteen pass and was
     * immediately inserted back into the network. Keeping the automatically
     * selected types in the configuration inventory makes the selection
     * visible and stable, while {@link #syncConfiguredItems(IMEMonitor)} keeps
     * doing the actual transfer through the normal config-to-buffer path.</p>
     */
    private void updateActiveConfiguration(IMEMonitor<IAEItemStack> monitor) {
        List<ActiveCandidate> available = new ArrayList<>();
        for (IAEItemStack stack : monitor.getStorageList()) {
            ItemStack item = stack.createItemStack();
            if (!item.isEmpty() && isAllowed(item)) {
                addCandidate(available, item, stack.getStackSize());
            }
        }

        // Items buffered by this bus still logically belong to the available
        // ME supply. Counting them prevents a selected type from disappearing
        // merely because the previous tick extracted the whole network stack.
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            if (!stored.isEmpty() && isAllowed(stored)) {
                addCandidate(available, stored, stored.getCount());
            }
        }

        Collections.sort(available, new Comparator<ActiveCandidate>() {
            @Override public int compare(ActiveCandidate left,
                                         ActiveCandidate right) {
                int amount = Long.compare(right.amount, left.amount);
                return amount != 0 ? amount
                    : candidateKey(left.stack).compareTo(candidateKey(right.stack));
            }
        });

        IOInventory config = getConfigInventory();
        int count = Math.min(Math.min(16, config.getSlots()), available.size());
        boolean[] assigned = new boolean[count];

        // Retain selected types in their previous slots. This avoids needless
        // reshuffling whenever two candidates exchange ranking positions.
        for (int slot = 0; slot < config.getSlots(); slot++) {
            ItemStack configured = config.getStackInSlot(slot);
            int selected = findCandidate(available, count, configured, assigned);
            if (selected >= 0) {
                assigned[selected] = true;
                setConfiguration(config, slot, available.get(selected));
            } else if (!configured.isEmpty()) {
                config.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }

        for (int selected = 0; selected < count; selected++) {
            if (assigned[selected]) continue;
            int slot = findEmptyConfigurationSlot(config);
            if (slot < 0) break;
            setConfiguration(config, slot, available.get(selected));
            assigned[selected] = true;
        }
    }

    private static void addCandidate(List<ActiveCandidate> candidates,
                                     ItemStack stack, long amount) {
        if (amount <= 0L) return;
        for (ActiveCandidate candidate : candidates) {
            if (sameType(candidate.stack, stack)) {
                candidate.amount = saturatedAdd(candidate.amount, amount);
                return;
            }
        }
        ItemStack representative = stack.copy();
        representative.setCount(1);
        candidates.add(new ActiveCandidate(representative, amount));
    }

    private static int findCandidate(List<ActiveCandidate> candidates, int count,
                                     ItemStack configured, boolean[] assigned) {
        if (configured.isEmpty()) return -1;
        for (int i = 0; i < count; i++) {
            if (!assigned[i] && sameType(candidates.get(i).stack, configured)) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyConfigurationSlot(IOInventory config) {
        for (int slot = 0; slot < config.getSlots(); slot++) {
            if (config.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static void setConfiguration(IOInventory config, int slot,
                                         ActiveCandidate candidate) {
        ItemStack current = config.getStackInSlot(slot);
        int configuredAmount = 1;
        if (sameType(current, candidate.stack)
                && current.getCount() == configuredAmount) return;
        ItemStack marker = candidate.stack.copy();
        marker.setCount(configuredAmount);
        config.setStackInSlot(slot, marker);
    }

    private void clearActiveConfiguration() {
        IOInventory config = getConfigInventory();
        for (int slot = 0; slot < config.getSlots(); slot++) {
            if (!config.getStackInSlot(slot).isEmpty()) {
                config.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static String candidateKey(ItemStack stack) {
        return String.valueOf(stack.getItem().getRegistryName()) + '#'
            + stack.getMetadata() + '#'
            + (stack.hasTagCompound() ? stack.getTagCompound().toString() : "");
    }

    private void syncConfiguredItems(IMEMonitor<IAEItemStack> monitor) {
        IOInventory config = getConfigInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            ItemStack configured = slot < config.getSlots()
                ? config.getStackInSlot(slot) : ItemStack.EMPTY;
            if (configured.isEmpty() || !isAllowed(configured)) {
                if (!stored.isEmpty()) {
                    inventory.setStackInSlot(slot, insertToNetwork(monitor, stored));
                }
                continue;
            }
            if (!stored.isEmpty() && !sameType(stored, configured)) {
                stored = insertToNetwork(monitor, stored);
                inventory.setStackInSlot(slot, stored);
                if (!stored.isEmpty()) continue;
            }
            pullIntoSlot(monitor, slot, configured);
        }
    }

    private void pullIntoSlot(IMEMonitor<IAEItemStack> monitor, int slot, ItemStack type) {
        ItemStack stored = inventory.getStackInSlot(slot);
        if (!stored.isEmpty() && !sameType(stored, type)) return;
        int room = stored.isEmpty() ? Integer.MAX_VALUE
            : Integer.MAX_VALUE - stored.getCount();
        if (room <= 0) return;
        IAEItemStack request = channel.createStack(type);
        if (request == null) return;
        IAEItemStack available = monitor.getStorageList()
            .findPrecise(request);
        long networkAmount = available == null ? 0L
            : available.getStackSize();
        long amount = InventoryReserveUtil.extractable(networkAmount,
            permanentReserve, room);
        if (amount <= 0L) return;
        request.setStackSize(amount);
        IAEItemStack extracted = monitor.extractItems(request, Actionable.MODULATE, source);
        if (extracted == null) return;
        ItemStack result = extracted.createItemStack();
        if (result.isEmpty()) return;
        if (stored.isEmpty()) inventory.setStackInSlot(slot, result);
        else {
            stored.grow(result.getCount());
            inventory.setStackInSlot(slot, stored);
        }
    }

    private ItemStack insertToNetwork(IMEMonitor<IAEItemStack> monitor, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        IAEItemStack request = channel.createStack(stack);
        if (request == null) return stack;
        request.setStackSize(stack.getCount());
        IAEItemStack remainder = monitor.injectItems(request, Actionable.MODULATE, source);
        return remainder == null ? ItemStack.EMPTY : remainder.createItemStack();
    }

    private void pushAllToNetwork() {
        try {
            IMEMonitor<IAEItemStack> monitor = proxy.getStorage().getInventory(channel);
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stored = inventory.getStackInSlot(i);
                if (!stored.isEmpty()) {
                    inventory.setStackInSlot(i, insertToNetwork(monitor, stored));
                }
            }
        } catch (Exception ignored) { }
    }

    private static boolean sameType(ItemStack left, ItemStack right) {
        return !left.isEmpty() && !right.isEmpty() && left.isItemEqual(right)
            && ItemStack.areItemStackTagsEqual(left, right);
    }
    private static boolean isOreDictItem(ItemStack stack, Set<String> names) {
        int[] ids = OreDictionary.getOreIDs(stack);
        if (ids.length == 0) return false;
        for (int id : ids) names.add(OreDictionary.getOreName(id));
        return true;
    }
    private boolean isAllowed(ItemStack stack) {
        Set<String> names = new HashSet<>();
        if (!isOreDictItem(stack, names)) return false;
        return OreDictFilter.matches(whitelist, names, true)
            && !OreDictFilter.matches(blacklist, names, false);
    }
    private static String clamp(String value) {
        if (value == null) return "";
        return value.length() > 256 ? value.substring(0, 256) : value;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        InventoryMarkerUtil.normalizeItemMarkers(getConfigInventory());
        whitelist = compound.hasKey(TAG_WHITELIST)
            ? compound.getString(TAG_WHITELIST)
            : compound.getString(LEGACY_TAG_WHITELIST);
        blacklist = compound.hasKey(TAG_BLACKLIST)
            ? compound.getString(TAG_BLACKLIST)
            : compound.getString(LEGACY_TAG_BLACKLIST);
        activePull = compound.hasKey(TAG_ACTIVE)
            ? compound.getBoolean(TAG_ACTIVE)
            : compound.getBoolean(LEGACY_TAG_ACTIVE);
        permanentReserve = InventoryReserveUtil.clamp(
            compound.getLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE));
    }
    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setString(TAG_WHITELIST, whitelist);
        compound.setString(TAG_BLACKLIST, blacklist);
        compound.setBoolean(TAG_ACTIVE, activePull);
        compound.setLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE,
            permanentReserve);
    }

    private static final class ActiveCandidate {
        private final ItemStack stack;
        private long amount;

        private ActiveCandidate(ItemStack stack, long amount) {
            this.stack = stack;
            this.amount = amount;
        }
    }
}
