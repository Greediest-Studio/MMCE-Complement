package net.edwin.mmcecomplement.compat.flux;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.BlockFluxInputHatch;
import net.edwin.mmcecomplement.block.BlockFluxOutputHatch;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Loaded only after {@code CompatMods} verifies the Flux Networks API. */
public final class FluxRegistryCompat {

    private FluxRegistryCompat() {}

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        ModBlocks.FLUX_INPUT_HATCH = new BlockFluxInputHatch();
        ModBlocks.FLUX_INPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "flux_input_hatch"));
        registry.register(ModBlocks.FLUX_INPUT_HATCH);

        ModBlocks.FLUX_OUTPUT_HATCH = new BlockFluxOutputHatch();
        ModBlocks.FLUX_OUTPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "flux_output_hatch"));
        registry.register(ModBlocks.FLUX_OUTPUT_HATCH);

        GameRegistry.registerTileEntity(TileFluxInputHatch.class,
            new ResourceLocation(Tags.MOD_ID, "flux_input_hatch"));
        GameRegistry.registerTileEntity(TileFluxOutputHatch.class,
            new ResourceLocation(Tags.MOD_ID, "flux_output_hatch"));
    }
}
