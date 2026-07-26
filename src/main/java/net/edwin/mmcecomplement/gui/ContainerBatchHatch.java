package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class ContainerBatchHatch extends Container {

    private final TileBatchHatch tile;

    public ContainerBatchHatch(EntityPlayer player, TileBatchHatch tile) {
        this.tile = tile;
        bindPlayerInventory(player.inventory);
    }

    public TileBatchHatch getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null
            && !tile.isInvalid()
            && tile.getWorld() == player.getEntityWorld()
            && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    private void bindPlayerInventory(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventory,
                    column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }
}
