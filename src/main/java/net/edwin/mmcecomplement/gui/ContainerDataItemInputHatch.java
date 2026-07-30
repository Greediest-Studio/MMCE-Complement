package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Shared container for every data input assembly tier. */
public class ContainerDataItemInputHatch extends Container {

    private final TileDataItemInputHatch tile;
    private final int hatchSlotCount;
    private final int itemCenterX;

    public ContainerDataItemInputHatch(EntityPlayer player,
                                       TileDataItemInputHatch tile) {
        this(player, tile, 132);
    }

    public ContainerDataItemInputHatch(EntityPlayer player,
                                       TileDataItemInputHatch tile,
                                       int itemCenterX) {
        this.tile = tile;
        this.hatchSlotCount = tile.getInventory().getSlots();
        this.itemCenterX = itemCenterX;
        bindHatchInventory();
        bindPlayerInventory(player.inventory);
    }

    public TileDataItemInputHatch getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null
            && !tile.isInvalid()
            && tile.getWorld() == player.getEntityWorld()
            && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < hatchSlotCount) {
            if (!mergeItemStack(stack, hatchSlotCount,
                inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeItemStack(stack, 0, hatchSlotCount, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private void bindHatchInventory() {
        int columns = tile.getTier().getItemColumns();
        int startX = itemCenterX - columns * 18 / 2 + 1;
        for (int index = 0; index < hatchSlotCount; index++) {
            int row = index / columns;
            int column = index % columns;
            addSlotToContainer(new SlotItemHandler(
                tile.getInventory().asGUIAccess(), index,
                startX + column * 18, 46 + row * 18));
        }
    }

    private void bindPlayerInventory(InventoryPlayer inventory) {
        int inventoryY = tile.getTier().getGuiHeight() - 82;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventory,
                    column + row * 9 + 9,
                    47 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column,
                47 + column * 18, inventoryY + 58));
        }
    }
}
