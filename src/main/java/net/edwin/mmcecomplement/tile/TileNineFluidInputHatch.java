package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;

/** Nine isolated fluid/gas tanks. The first available size is MMCE's normal size. */
public class TileNineFluidInputHatch extends TileQuadFluidInputHatch {

    public static final int TANK_COUNT = 9;

    public TileNineFluidInputHatch() {
        this(FluidHatchSize.NORMAL);
    }

    public TileNineFluidInputHatch(FluidHatchSize hatchSize) {
        super(hatchSize, TANK_COUNT);
    }

    public static int capacityForTotal(int totalCapacity) {
        return capacityForTankCount(totalCapacity, TANK_COUNT);
    }
}
