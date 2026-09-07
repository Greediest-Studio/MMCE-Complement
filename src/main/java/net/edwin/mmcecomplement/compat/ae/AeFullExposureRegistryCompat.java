package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEFullExposureAssembly;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

/** Registers the full-exposure assembly independently of Mekanism presence. */
public final class AeFullExposureRegistryCompat {

    private AeFullExposureRegistryCompat() {}

    public static void registerBlock(IForgeRegistry<Block> registry) {
        if (ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY != null) return;
        ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY = new BlockMEFullExposureAssembly();
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID,
            "me_full_exposure_assembly");
        ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY.setRegistryName(id);
        registry.register(ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
        GameRegistry.registerTileEntity(AeFullExposureTileFactory.tileClass(), id);
    }
}
