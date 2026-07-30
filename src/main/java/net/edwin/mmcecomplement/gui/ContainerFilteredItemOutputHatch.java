package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileFilteredItemOutputHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Safe view/extraction container for an item stack whose count exceeds 127. */
public class ContainerFilteredItemOutputHatch extends Container {

    private final TileFilteredItemOutputHatch tile;
    private int lastCount = Integer.MIN_VALUE;

    public ContainerFilteredItemOutputHatch(EntityPlayer player,
                                             TileFilteredItemOutputHatch tile) {
        this.tile = tile;
        addSlotToContainer(new DisplayOutputSlot(tile, 50, 35));
        bindPlayerInventory(player.inventory);
    }

    public TileFilteredItemOutputHatch getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid()
            && tile.getWorld() == player.getEntityWorld()
            && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendCount(listener, tile.getStoredCount());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int count = tile.getStoredCount();
        if (count != lastCount) {
            for (IContainerListener listener : listeners) {
                sendCount(listener, count);
            }
            lastCount = count;
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        int old = tile.getStoredCount();
        if (id == 0) {
            tile.setClientStoredCount((old & 0xFFFF0000) | (data & 0xFFFF));
        } else if (id == 1) {
            tile.setClientStoredCount((old & 0x0000FFFF) | ((data & 0xFFFF) << 16));
        }
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index != 0) return ItemStack.EMPTY;
        ItemStack stored = tile.getInventory().getStackInSlot(0);
        if (stored.isEmpty()) return ItemStack.EMPTY;
        int request = Math.min(stored.getMaxStackSize(), stored.getCount());
        ItemStack extracted = tile.getInventory().extractItem(0, request, false);
        if (extracted.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = extracted.copy();
        if (!mergeItemStack(extracted, 1, inventorySlots.size(), true)) {
            tile.getInventory().insertItem(0, extracted, false);
            return ItemStack.EMPTY;
        }
        if (!extracted.isEmpty()) {
            tile.getInventory().insertItem(0, extracted, false);
            result.setCount(result.getCount() - extracted.getCount());
        }
        return result.getCount() <= 0 ? ItemStack.EMPTY : result;
    }

    private void sendCount(IContainerListener listener, int count) {
        listener.sendWindowProperty(this, 0, count & 0xFFFF);
        listener.sendWindowProperty(this, 1, (count >>> 16) & 0xFFFF);
    }

    private void bindPlayerInventory(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventory,
                    column + row * 9 + 9, 8 + column * 18,
                    84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column,
                8 + column * 18, 142));
        }
    }

    private static final class DisplayOutputSlot extends SlotItemHandler {
        private final TileFilteredItemOutputHatch tile;

        private DisplayOutputSlot(TileFilteredItemOutputHatch tile,
                                  int x, int y) {
            super(tile.getInventory(), 0, x, y);
            this.tile = tile;
        }

        @Nonnull
        @Override
        public ItemStack getStack() {
            ItemStack stored = tile.getInventory().getStackInSlot(0);
            if (stored.isEmpty()) return ItemStack.EMPTY;
            ItemStack display = stored.copy();
            display.setCount(Math.min(stored.getMaxStackSize(),
                stored.getCount()));
            return display;
        }

        @Override public boolean isItemValid(@Nonnull ItemStack stack) { return false; }
        @Override public int getSlotStackLimit() { return 64; }
        @Override public void putStack(@Nonnull ItemStack stack) { }
    }
}
