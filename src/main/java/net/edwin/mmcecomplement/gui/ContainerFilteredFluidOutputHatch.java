package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileFilteredFluidOutputHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;

/** Synchronizes the int-max tank amount without relying on tile packet timing. */
public class ContainerFilteredFluidOutputHatch extends Container {

    private final TileFilteredFluidOutputHatch tile;
    private int lastAmount = Integer.MIN_VALUE;

    public ContainerFilteredFluidOutputHatch(EntityPlayer player,
                                              TileFilteredFluidOutputHatch tile) {
        this.tile = tile;
        bindPlayerInventory(player.inventory);
    }

    public TileFilteredFluidOutputHatch getTile() { return tile; }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid()
            && tile.getWorld() == player.getEntityWorld()
            && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendAmount(listener, tile.getStoredAmount());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int amount = tile.getStoredAmount();
        if (amount != lastAmount) {
            for (IContainerListener listener : listeners) {
                sendAmount(listener, amount);
            }
            lastAmount = amount;
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        int old = tile.getStoredAmount();
        if (id == 0) {
            tile.setClientStoredAmount((old & 0xFFFF0000) | (data & 0xFFFF));
        } else if (id == 1) {
            tile.setClientStoredAmount((old & 0x0000FFFF) | ((data & 0xFFFF) << 16));
        }
    }

    private void sendAmount(IContainerListener listener, int amount) {
        listener.sendWindowProperty(this, 0, amount & 0xFFFF);
        listener.sendWindowProperty(this, 1, (amount >>> 16) & 0xFFFF);
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
}
