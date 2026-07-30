package net.edwin.mmcecomplement.compat.mekanism;

import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.minecraft.tileentity.TileEntity;

/** Keeps optional Mekanism symbols out of the common hatch block. */
public final class DataItemInputHatchMekanismFactory {

    private DataItemInputHatchMekanismFactory() {}

    public static TileEntity create(DataInputAssemblyTier tier) {
        return new TileDataItemInputHatchMekanism(tier);
    }
}
