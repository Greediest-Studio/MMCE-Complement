package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEManaInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEManaOutputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaOutputBus;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

/** CrazyAE/Modular Magic mana-channel registration. */
public final class AeManaRegistryCompat {

    private AeManaRegistryCompat() {}

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        ModBlocks.ME_MANA_INPUT_BUS = new BlockMEManaInputBus();
        register(registry, ModBlocks.ME_MANA_INPUT_BUS,
            TileMEManaInputBus.class, "me_mana_input_bus");

        ModBlocks.ME_MANA_OUTPUT_BUS = new BlockMEManaOutputBus();
        register(registry, ModBlocks.ME_MANA_OUTPUT_BUS,
            TileMEManaOutputBus.class, "me_mana_output_bus");
    }

    private static void register(IForgeRegistry<Block> registry, Block block,
                                 Class<? extends net.minecraft.tileentity.TileEntity> tile,
                                 String name) {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, name);
        block.setRegistryName(id);
        registry.register(block);
        GameRegistry.registerTileEntity(tile, id);
    }
}
