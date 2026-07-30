package net.edwin.mmcecomplement.tile;

import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;

/**
 * Item/fluid input assembly without the smart-data interface portion.
 * The implementation deliberately reuses the well-tested tiered hybrid tank
 * and item inventory behaviour of the combined data input assembly.
 */
public class TileItemInputAssemblyHatch extends TileDataItemInputHatch {

    public TileItemInputAssemblyHatch() {
        super();
    }

    public TileItemInputAssemblyHatch(DataInputAssemblyTier tier) {
        super(tier);
    }
}
