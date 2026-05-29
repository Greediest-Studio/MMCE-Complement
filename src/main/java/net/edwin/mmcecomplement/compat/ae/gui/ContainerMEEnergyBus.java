package net.edwin.mmcecomplement.compat.ae.gui;

import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyBusBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

public class ContainerMEEnergyBus extends Container {

    private final TileMEEnergyBusBase tile;

    public ContainerMEEnergyBus(EntityPlayer player, TileMEEnergyBusBase tile) {
        this.tile = tile;
        bindPlayerInventory(player.inventory);
    }

    public TileMEEnergyBusBase getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null
                && !tile.isInvalid()
                && tile.getWorld() == playerIn.world
                && playerIn.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    private void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventoryPlayer, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventoryPlayer, column, 8 + column * 18, 142));
        }
    }
}