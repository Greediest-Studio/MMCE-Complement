package net.edwin.mmcecomplement.variant;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraftforge.items.ItemStackHandler;

/** Item handler that marks the controller dirty when a storage slot changes. */
public class StorageItemHandler extends ItemStackHandler {
    private final TileMultiblockMachineController owner;

    public StorageItemHandler(TileMultiblockMachineController owner, int size) {
        super(size);
        this.owner = owner;
    }

    @Override
    protected void onContentsChanged(int slot) {
        owner.markDirty();
        owner.markForUpdateSync();
    }
}
