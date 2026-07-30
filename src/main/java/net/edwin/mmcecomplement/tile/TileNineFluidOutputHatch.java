package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;

/** Nine-slot output hatch using the same duplicate-fluid routing as the four-slot hatch. */
public class TileNineFluidOutputHatch extends TileQuadFluidOutputHatch {

    public static final int TANK_COUNT = 9;

    public TileNineFluidOutputHatch() {
        this(FluidHatchSize.NORMAL);
    }

    public TileNineFluidOutputHatch(FluidHatchSize hatchSize) {
        super(hatchSize, TANK_COUNT);
    }

    public static int capacityForTotal(int totalCapacity) {
        return capacityForTankCount(totalCapacity, TANK_COUNT);
    }
}
