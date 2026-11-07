package net.edwin.mmcecomplement.compat;

import net.minecraft.block.Block;
import net.minecraftforge.registries.IForgeRegistry;

/** Reflection boundary: no ae2.* or appeng.* signatures in common loading. */
public final class Ae2sStorageCompat {
    private Ae2sStorageCompat() { }

    public static boolean isLoaded() {
        return net.minecraftforge.fml.common.Loader.isModLoaded(CompatMods.MODID_AE2S)
            && !net.minecraftforge.fml.common.Loader.isModLoaded(CompatMods.MODID_AE2);
    }

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        try {
            Class.forName("net.edwin.mmcecomplement.compat.ae2s.Ae2sStorageRegistration")
                .getMethod("registerBlocks", IForgeRegistry.class).invoke(null, registry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot register AE2S storage ports", e);
        }
    }
}
