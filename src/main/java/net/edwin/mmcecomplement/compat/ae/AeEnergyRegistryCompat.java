package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEEnergyInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEEnergyOutputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyOutputBus;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

/** CrazyAE energy-channel registration, isolated from common startup code. */
public final class AeEnergyRegistryCompat {

    private AeEnergyRegistryCompat() {}

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        ModBlocks.ME_ENERGY_INPUT_BUS = new BlockMEEnergyInputBus();
        register(registry, ModBlocks.ME_ENERGY_INPUT_BUS,
            TileMEEnergyInputBus.class, "me_energy_input_bus");

        ModBlocks.ME_ENERGY_OUTPUT_BUS = new BlockMEEnergyOutputBus();
        register(registry, ModBlocks.ME_ENERGY_OUTPUT_BUS,
            TileMEEnergyOutputBus.class, "me_energy_output_bus");
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
