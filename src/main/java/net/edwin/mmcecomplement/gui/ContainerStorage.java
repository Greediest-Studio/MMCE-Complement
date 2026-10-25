package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Scrollable, variable-height view over a storage variant controller. */
public class ContainerStorage extends Container {
    public static final int COLUMNS = 9;
    public static final int MAX_ROWS = 6;
    public static final int PAGE_SIZE = COLUMNS * MAX_ROWS;
    /** Coordinates shared with GuiStorage so the drawn slots always match the
     * actual Slot positions. These follow the vanilla container spacing. */
    // 9 * 18 = 162 pixels; centered in the 174 pixel inset (6 px margins).
    public static final int SLOT_X = 11;
    public static final int STORAGE_TOP = 36;

    private final StorageController storage;
    private final EntityPlayer player;
    private int totalSlots;
    private int visibleRows;
    private final int pageSize;
    private int pageCount;
    private int page;
    private int mode;

    public ContainerStorage(StorageController storage, EntityPlayer player) {
        this.storage = storage;
        this.player = player;
        totalSlots = storage == null ? 0
            : Math.max(0, storage.mmceComplement$getStorageSlots());
        pageSize = PAGE_SIZE;
        refreshLayout();

        for (int i = 0; i < pageSize; i++) {
            addSlotToContainer(new StorageSlot(storage, this, i,
                SLOT_X + (i % COLUMNS) * 18,
                STORAGE_TOP + (i / COLUMNS) * 18));
        }
        bindPlayerInventory(player.inventory);
    }

    public int getPage() { return page; }
    public static final int MODE_ITEMS = 0, MODE_FLUIDS = 1, MODE_GASES = 2;
    public int getMode() { return mode; }
    private boolean formed() { return storage instanceof hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController && ((hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController)storage).isStructureFormed(); }
    public boolean hasFluidTab() { return formed() && storage != null && storage.mmceComplement$hasFluidStorage() && storage.mmceComplement$getFluidStorageSlots() > 0; }
    public boolean hasGasTab() { return formed() && storage != null && storage.mmceComplement$hasGasStorage() && storage.mmceComplement$getGasStorageSlots() > 0; }
    public boolean isModeAvailable(int value) { return value == MODE_ITEMS || (value == MODE_FLUIDS && hasFluidTab()) || (value == MODE_GASES && hasGasTab()); }
    public void setMode(int value) { if (isModeAvailable(value)) { mode=value; page=0; refreshLayout(); detectAndSendChanges(); } }
    public int getPageCount() { return pageCount; }
    public int getVisibleRows() { return visibleRows; }
    public int getPageSize() { return pageSize; }
    public int getTotalSlots() { return totalSlots; }
    public int getTankCount() { return mode == MODE_FLUIDS ? (storage == null ? 0 : storage.mmceComplement$getFluidStorageSlots()) : mode == MODE_GASES ? (storage == null ? 0 : storage.mmceComplement$getGasStorageSlots()) : 0; }
    public int getVisibleTankCount() { return Math.max(0, Math.min(pageSize, getTankCount() - page * COLUMNS)); }
    /** Returns only a real, visible tank; the player inventory is never a tank target. */
    public int getTankAt(int x, int y) {
        if (mode == MODE_ITEMS) return -1;
        int left = SLOT_X - 1, top = STORAGE_TOP - 1;
        if (x < left || x >= left + COLUMNS * 18
            || y < top || y >= top + visibleRows * 18) return -1;
        int local = (y - top) / 18 * COLUMNS + (x - left) / 18;
        return local < getVisibleTankCount() ? page * COLUMNS + local : -1;
    }
    public int getTankCapacity() { return mode == MODE_FLUIDS ? (storage == null ? 0 : storage.mmceComplement$getFluidStorageCapacity()) : mode == MODE_GASES ? (storage == null ? 0 : storage.mmceComplement$getGasStorageCapacity()) : 0; }
    public IFluidHandler getFluidTank(int index) { return storage == null ? null : storage.mmceComplement$getFluidTank(index); }
    public Object getGasStorage() { return storage == null ? null : storage.mmceComplement$getGasStorage(); }
    public int getVisibleSlotCount() {
        if (mode != MODE_ITEMS) return 0;
        return Math.max(0, Math.min(pageSize,
            totalSlots - page * COLUMNS));
    }
    public StorageController getStorage() { return storage; }

    /**
     * Compacts and orders the storage inventory.  This is deliberately done
     * on the server, just like Inventory Sorter's middle mouse action, so a
     * client cannot spoof item movement by sorting its local view.
     */
    public void sortStorage() {
        if (!canInteractWith(player) || storage == null) return;
        if (mode == MODE_FLUIDS) {
            IFluidHandler fluids = storage.mmceComplement$getFluidStorage();
            if (fluids instanceof net.edwin.mmcecomplement.variant.StorageFluidHandler) {
                ((net.edwin.mmcecomplement.variant.StorageFluidHandler) fluids).sort();
            }
            detectAndSendChanges();
            return;
        }
        if (mode == MODE_GASES) {
            net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge.sort(
                storage.mmceComplement$getGasStorage());
            detectAndSendChanges();
            return;
        }
        ItemStackHandler handler = storage.mmceComplement$getStorageInventory();
        if (handler == null) return;

        List<StackGroup> groups = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) continue;
            StackGroup matching = null;
            for (StackGroup group : groups) {
                if (sameItem(group.prototype, stack)) {
                    matching = group;
                    break;
                }
            }
            if (matching == null) {
                ItemStack prototype = stack.copy();
                prototype.setCount(1);
                matching = new StackGroup(prototype);
                groups.add(matching);
            }
            matching.amount += stack.getCount();
        }

        groups.sort(new Comparator<StackGroup>() {
            @Override public int compare(StackGroup left, StackGroup right) {
                ResourceLocation l = left.prototype.getItem().getRegistryName();
                ResourceLocation r = right.prototype.getItem().getRegistryName();
                int result = String.valueOf(l).compareTo(String.valueOf(r));
                if (result != 0) return result;
                result = Integer.compare(left.prototype.getMetadata(),
                    right.prototype.getMetadata());
                if (result != 0) return result;
                String ln = left.prototype.hasTagCompound()
                    ? left.prototype.getTagCompound().toString() : "";
                String rn = right.prototype.hasTagCompound()
                    ? right.prototype.getTagCompound().toString() : "";
                return ln.compareTo(rn);
            }
        });

        int requiredSlots = 0;
        for (StackGroup group : groups) {
            int max = Math.max(1, group.prototype.getMaxStackSize());
            requiredSlots += (group.amount + max - 1) / max;
        }
        // A malformed or externally supplied overstack must never be lost by
        // a sort operation. Leave the inventory untouched if it cannot be
        // represented by the available slots.
        if (requiredSlots > handler.getSlots()) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
        int slot = 0;
        for (StackGroup group : groups) {
            int remaining = group.amount;
            int max = Math.max(1, group.prototype.getMaxStackSize());
            while (remaining > 0 && slot < handler.getSlots()) {
                ItemStack part = group.prototype.copy();
                int count = Math.min(max, remaining);
                part.setCount(count);
                handler.setStackInSlot(slot++, part);
                remaining -= count;
            }
        }
        detectAndSendChanges();
    }

    private static final class StackGroup {
        private final ItemStack prototype;
        private int amount;
        private StackGroup(ItemStack prototype) {
            this.prototype = prototype;
        }
    }

    private static boolean sameItem(ItemStack left, ItemStack right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return left == right || (left != null && right != null
                && left.isEmpty() && right.isEmpty());
        }
        ItemStack a = left.copy();
        ItemStack b = right.copy();
        a.setCount(1);
        b.setCount(1);
        return ItemStack.areItemStacksEqual(a, b);
    }

    public int getPlayerInventoryTop() {
        return STORAGE_TOP + visibleRows * 18 + 14;
    }

    public int getHotbarTop() {
        return getPlayerInventoryTop() + 58;
    }

    public void setPage(int page) {
        refreshLayout();
        this.page = Math.max(0, Math.min(page, pageCount - 1));
        detectAndSendChanges();
    }

    /** Re-read capacity so a machine modifier can resize the container live. */
    public void refreshLayout() {
        int slots = storage == null ? 0
            : Math.max(0, storage.mmceComplement$getStorageSlots());
        // Force the backing handler to resize now, so a live capacity change
        // immediately ejects stacks that no longer fit on the server.
        if (storage != null) storage.mmceComplement$getStorageInventory();
        totalSlots = slots;
        int viewSlots = mode == MODE_ITEMS ? slots : getTankCount();
        visibleRows = Math.max(1, Math.min(MAX_ROWS,
            (Math.max(1, viewSlots) + COLUMNS - 1) / COLUMNS));
        int totalRows = Math.max(1, (Math.max(1, viewSlots) + COLUMNS - 1)
            / COLUMNS);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        pageCount = maxScroll + 1;
        if (page >= pageCount) page = pageCount - 1;
        // Slot IDs stay stable across tabs; move the actual slots together
        // with their background whenever the number of visible rows changes.
        for (int i = 0; i < 36 && pageSize + i < inventorySlots.size(); i++) {
            Slot slot = inventorySlots.get(pageSize + i);
            slot.xPos = SLOT_X + (i % COLUMNS) * 18;
            slot.yPos = i < 27 ? getPlayerInventoryTop() + (i / COLUMNS) * 18
                : getHotbarTop();
        }
    }

    @Override
    public void detectAndSendChanges() {
        refreshLayout();
        super.detectAndSendChanges();
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) setPage(data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return storage != null && storage.mmceComplement$isStorageController()
            && formed()
            && player.getDistanceSq(((net.minecraft.tileentity.TileEntity) storage)
                .getPos()) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (mode != MODE_ITEMS) return ItemStack.EMPTY;
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack source = slot.getStack();
        ItemStack original = source.copy();
        boolean storageSlot = index < pageSize;
        if (storageSlot) {
            if (!mergeItemStack(source, pageSize, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeItemStack(source, 0, pageSize, false)) {
            return ItemStack.EMPTY;
        }
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.putStack(source);
        slot.onSlotChanged();
        return original;
    }

    private void bindPlayerInventory(InventoryPlayer inventory) {
        int top = getPlayerInventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlotToContainer(new Slot(inventory, col + row * COLUMNS + 9,
                    SLOT_X + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlotToContainer(new Slot(inventory, col,
                SLOT_X + col * 18, getHotbarTop()));
        }
    }

    public static final class StorageSlot extends Slot {
        private final StorageController storage;
        private final ContainerStorage owner;
        private final int localIndex;

        StorageSlot(StorageController storage, ContainerStorage owner,
                    int localIndex, int x, int y) {
            super(new InventoryPlayer(owner.player), localIndex, x, y);
            this.storage = storage;
            this.owner = owner;
            this.localIndex = localIndex;
        }

        private ItemStackHandler handler() {
            return storage == null ? null
                : storage.mmceComplement$getStorageInventory();
        }

        // page is a row offset, not a page-sized offset.  Keeping this in
        // rows makes the scrollbar a continuous view for large containers.
        private int index() { return owner.page * COLUMNS + localIndex; }

        private boolean exists() {
            return index() < owner.totalSlots;
        }

        @Override public ItemStack getStack() {
            if (owner.mode != MODE_ITEMS) return ItemStack.EMPTY;
            ItemStackHandler h = handler();
            return !exists() || h == null || index() >= h.getSlots()
                ? ItemStack.EMPTY : h.getStackInSlot(index());
        }

        @Override public void putStack(ItemStack stack) {
            if (owner.mode != MODE_ITEMS) return;
            ItemStackHandler h = handler();
            if (exists() && h != null && index() < h.getSlots()) {
                h.setStackInSlot(index(), stack);
            }
            onSlotChanged();
        }

        @Override public ItemStack decrStackSize(int amount) {
            if (owner.mode != MODE_ITEMS) return ItemStack.EMPTY;
            ItemStackHandler h = handler();
            return !exists() || h == null || index() >= h.getSlots()
                ? ItemStack.EMPTY : h.extractItem(index(), amount, false);
        }

        @Override public boolean isItemValid(ItemStack stack) { return owner.mode == MODE_ITEMS && exists(); }
        @Override public boolean isEnabled() { return owner.mode == MODE_ITEMS && exists(); }
        @Override public int getSlotStackLimit() { return 64; }
        @Override public boolean canTakeStack(EntityPlayer player) { return owner.mode == MODE_ITEMS && exists(); }
    }
}
