package net.edwin.mmcecomplement.compat.mekanism;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import net.edwin.mmcecomplement.tile.TileNineFluidInputHatch;

/** Mekanism gas-capable nine-tank input hatch. */
public class TileNineFluidInputHatchMekanism extends TileQuadFluidInputHatchMekanism {

    public TileNineFluidInputHatchMekanism() {
        this(FluidHatchSize.NORMAL);
    }

    public TileNineFluidInputHatchMekanism(FluidHatchSize size) {
        super(size, TileNineFluidInputHatch.TANK_COUNT);
    }
}
