package net.edwin.mmcecomplement.variant;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

/** Item handler that marks the controller dirty when a storage slot changes. */
public class StorageItemHandler extends ItemStackHandler {
    private final TileMultiblockMachineController owner;

    public StorageItemHandler(TileMultiblockMachineController owner, int size) {
        super(size);
        this.owner = owner;
    }

    /** Retain handler identity for pipes; never remove an occupied slot. */
    public synchronized boolean resize(int size) {
        if (size < 1 || size > StorageCapacitySettings.MAX_SLOTS) return false;
        if (size == stacks.size()) return true;
        for (int i = size; i < stacks.size(); i++) if (!stacks.get(i).isEmpty()) return false;
        net.minecraft.util.NonNullList<ItemStack> next = net.minecraft.util.NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(size, stacks.size()); i++) next.set(i, stacks.get(i));
        stacks = next;
        onContentsChanged(0);
        return true;
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
