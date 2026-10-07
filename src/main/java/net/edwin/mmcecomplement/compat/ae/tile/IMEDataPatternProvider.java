package net.edwin.mmcecomplement.compat.ae.tile;

import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Common data-interface contract shared by tier-one and tier-two providers. */
public interface IMEDataPatternProvider {

    TileSmartInterface.SmartInterfaceProvider getPrimaryDataProvider();

    List<? extends TileSmartInterface.SmartInterfaceProvider>
        getAllDataProviders();

    TileSmartInterface.SmartInterfaceProvider getDataProviderForGroup(
        long groupId);

    boolean isDataProviderActiveForGroup(long groupId);

    String getSelectedInterfaceType();

    boolean resetUnsupportedSelectionForController(BlockPos controllerPos);
}
