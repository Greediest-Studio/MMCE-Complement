package net.edwin.mmcecomplement.compat.mekanism;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import net.minecraft.tileentity.TileEntity;

/** Optional Mekanism bridge for nine-tank hatches. */
public final class NineFluidHatchMekanismFactory {

    private NineFluidHatchMekanismFactory() {}

    public static TileEntity create(FluidHatchSize size) {
        return new TileNineFluidInputHatchMekanism(size);
    }

    public static TileEntity createOutput(FluidHatchSize size) {
        return new TileNineFluidOutputHatchMekanism(size);
    }
}
