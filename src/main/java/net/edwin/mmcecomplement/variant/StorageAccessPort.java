package net.edwin.mmcecomplement.variant;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;

/** Optional storage network ports implement this without coupling controllers to AE. */
public interface StorageAccessPort {
    void bindStorage(TileMultiblockMachineController controller);
    void unbindStorage(TileMultiblockMachineController controller);
}
