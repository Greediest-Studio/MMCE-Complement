package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/** Container for the tiered item/fluid output assembly. */
public class ContainerItemOutputAssemblyHatch extends Container {
    private final TileItemOutputAssemblyHatch tile;
    private final int hatchSlotCount;

    public ContainerItemOutputAssemblyHatch(EntityPlayer player,
                                             TileItemOutputAssemblyHatch tile) {
        this.tile = tile;
        this.hatchSlotCount = tile.getInventory().getSlots();
        int columns = tile.getTier().getItemColumns();
        int startX = 71 - columns * 18 / 2 + 1;
        for (int index = 0; index < hatchSlotCount; index++) {
            addSlotToContainer(new SlotItemHandler(tile.getInventory().asGUIAccess(), index,
                startX + index % columns * 18, 46 + index / columns * 18));
        }
        bindPlayerInventory(player.inventory, tile.getTier().getGuiHeight());
    }

    public TileItemOutputAssemblyHatch getTile() { return tile; }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid() && tile.getWorld() == player.getEntityWorld()
            && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Nonnull @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(); ItemStack original = stack.copy();
        if (index < hatchSlotCount) {
            if (!mergeItemStack(stack, hatchSlotCount, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else if (!mergeItemStack(stack, 0, hatchSlotCount, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack); return original;
    }

    private void bindPlayerInventory(InventoryPlayer inventory, int guiHeight) {
        int inventoryY = guiHeight - 82;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlotToContainer(new Slot(inventory, column + row * 9 + 9,
                47 + column * 18, inventoryY + row * 18));
        for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory,
            column, 47 + column * 18, inventoryY + 58));
    }
}
