package net.edwin.mmcecomplement.compat.mekanism;

import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.minecraft.tileentity.TileEntity;

public final class ItemOutputAssemblyHatchMekanismFactory {
    private ItemOutputAssemblyHatchMekanismFactory() {}

    public static TileEntity create(DataInputAssemblyTier tier) {
        return new TileItemOutputAssemblyHatchMekanism(tier);
    }
}
