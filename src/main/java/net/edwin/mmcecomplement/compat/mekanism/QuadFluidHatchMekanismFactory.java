package net.edwin.mmcecomplement.compat.mekanism;

import net.minecraft.tileentity.TileEntity;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;

/** Keeps optional Mekanism symbols out of the common hatch block. */
public final class QuadFluidHatchMekanismFactory {

    private QuadFluidHatchMekanismFactory() {}

    public static TileEntity create() {
        return new TileQuadFluidInputHatchMekanism();
    }

    public static TileEntity create(FluidHatchSize size) {
        return new TileQuadFluidInputHatchMekanism(size);
    }

    public static TileEntity createOutput() {
        return new TileQuadFluidOutputHatchMekanism();
    }

    public static TileEntity createOutput(FluidHatchSize size) {
        return new TileQuadFluidOutputHatchMekanism(size);
    }
}
