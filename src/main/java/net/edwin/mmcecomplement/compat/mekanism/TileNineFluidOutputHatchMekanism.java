package net.edwin.mmcecomplement.compat.mekanism;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import net.edwin.mmcecomplement.tile.TileNineFluidOutputHatch;

/** Mekanism gas-capable nine-tank output hatch. */
public class TileNineFluidOutputHatchMekanism extends TileQuadFluidOutputHatchMekanism {

    public TileNineFluidOutputHatchMekanism() {
        this(FluidHatchSize.NORMAL);
    }

    public TileNineFluidOutputHatchMekanism(FluidHatchSize size) {
        super(size, TileNineFluidOutputHatch.TANK_COUNT);
    }
}
