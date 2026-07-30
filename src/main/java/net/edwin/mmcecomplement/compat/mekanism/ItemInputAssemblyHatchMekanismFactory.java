package net.edwin.mmcecomplement.compat.mekanism;

import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.minecraft.tileentity.TileEntity;

public final class ItemInputAssemblyHatchMekanismFactory {
    private ItemInputAssemblyHatchMekanismFactory() {}
    public static TileEntity create(DataInputAssemblyTier tier) {
        return new TileItemInputAssemblyHatchMekanism(tier);
    }
}
