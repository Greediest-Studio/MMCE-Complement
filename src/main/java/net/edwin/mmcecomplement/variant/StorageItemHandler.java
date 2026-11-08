package net.edwin.mmcecomplement.variant;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemStackHandler;

/** Item handler that marks the controller dirty when a storage slot changes. */
public class StorageItemHandler extends ItemStackHandler {
    private final TileMultiblockMachineController owner;
    private boolean resizing;

    public StorageItemHandler(TileMultiblockMachineController owner, int size) {
        super(size);
        this.owner = owner;
    }

    /** Capacity settings are committed by the caller after the resize finishes. */
    public synchronized boolean isResizing() { return resizing; }

    /** Move removed slots into retained empty slots, then eject any remainder. */
    public synchronized boolean resize(int size) {
        if (resizing || size < 1 || size > StorageCapacitySettings.MAX_SLOTS) return false;
        if (size == stacks.size()) return true;
        NonNullList<ItemStack> previous = stacks;
        NonNullList<ItemStack> next = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(size, previous.size()); i++) next.set(i, previous.get(i));
        int target = 0;
        for (int i = size; i < stacks.size(); i++) {
            ItemStack remaining = stacks.get(i).copy();
            if (remaining.isEmpty()) continue;
            for (; target < size && !remaining.isEmpty(); target++) {
                if (!next.get(target).isEmpty()) continue;
                int count = Math.min(remaining.getCount(), Math.min(getSlotLimit(target), remaining.getMaxStackSize()));
                if (count <= 0) continue;
                ItemStack moved = remaining.splitStack(count);
                next.set(target, moved);
            }
            previous.set(i, remaining);
        }
        // Publish the retained inventory before entity callbacks can access it.
        // Removed stacks are detached, so callbacks cannot extract them twice.
        stacks = next;
        resizing = true;
        boolean completed = false;
        try {
            for (int i = size; i < previous.size(); i++) {
                ItemStack remaining = previous.get(i);
                if (!remaining.isEmpty()) {
                    if (!StorageItemDrops.drop(owner.getWorld(), owner.getPos(), remaining)) return false;
                    previous.set(i, ItemStack.EMPTY);
                }
            }
            completed = true;
            return true;
        } finally {
            if (!completed) {
                // Keep any undropped remainder in its original tail slot.
                // Preserve changes made to retained slots by entity callbacks.
                for (int i = 0; i < size; i++) previous.set(i, stacks.get(i));
                stacks = previous;
            }
            resizing = false;
            onContentsChanged(0);
        }
    }

    @Override public synchronized int getSlots() { return super.getSlots(); }

    /** Serialize all pipe/GUI operations on the shared controller inventory. */
    @Override
    public synchronized ItemStack insertItem(int slot, ItemStack stack,
                                              boolean simulate) {
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public synchronized ItemStack extractItem(int slot, int amount,
                                              boolean simulate) {
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public synchronized void setStackInSlot(int slot, ItemStack stack) {
        super.setStackInSlot(slot, stack);
    }

    @Override
    public synchronized ItemStack getStackInSlot(int slot) {
        return super.getStackInSlot(slot);
    }

    @Override
    public synchronized NBTTagCompound serializeNBT() {
        return super.serializeNBT();
    }

    @Override
    public synchronized void deserializeNBT(NBTTagCompound nbt) {
        super.deserializeNBT(nbt);
    }

    @Override
    protected void onContentsChanged(int slot) {
        owner.markDirty();
        owner.markForUpdateSync();
        if (owner instanceof StorageController) {
            ((StorageController) owner).mmceComplement$markStorageChanged();
        }
    }
}
