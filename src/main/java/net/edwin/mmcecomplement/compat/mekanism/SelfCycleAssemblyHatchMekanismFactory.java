package net.edwin.mmcecomplement.compat.mekanism;

import net.minecraft.tileentity.TileEntity;

/** Keeps optional Mekanism tile construction out of the common block. */
public final class SelfCycleAssemblyHatchMekanismFactory {
    private SelfCycleAssemblyHatchMekanismFactory() { }
    public static TileEntity create() {
        return new TileSelfCycleAssemblyHatchMekanism();
    }
}
