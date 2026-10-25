package net.edwin.mmcecomplement.variant;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;

/** Binds only ports which belong to the actual matched storage structure. */
public final class StorageAccessPorts {
    private StorageAccessPorts() { }

    public static Set<StorageAccessPort> refresh(TileMultiblockMachineController controller,
                                                Set<StorageAccessPort> previous) {
        Set<StorageAccessPort> current = new HashSet<>();
        if (controller.isStructureFormed() && controller.getFoundPattern() != null) {
            for (BlockPos relative : controller.getFoundPattern().getTileBlocksArray().keySet()) {
                BlockPos pos = controller.getPos().add(relative);
                if (!controller.getWorld().isBlockLoaded(pos)) continue;
                TileEntity tile = controller.getWorld().getTileEntity(pos);
                if (tile instanceof StorageAccessPort && !tile.isInvalid()) {
                    StorageAccessPort port = (StorageAccessPort) tile;
                    current.add(port);
                    port.bindStorage(controller);
                }
            }
        }
        for (StorageAccessPort port : previous) {
            if (!current.contains(port)) port.unbindStorage(controller);
        }
        return current;
    }
}
