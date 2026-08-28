package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEFullExposureAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEGasInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEInventoryInputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEOutputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEGasInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInventoryInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

/** Mekanism Energistics registration, isolated from common startup code. */
public final class AeGasRegistryCompat {

    private AeGasRegistryCompat() {}

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        ModBlocks.ME_GAS_INVENTORY_INPUT_BUS = new BlockMEGasInventoryInputBus();
        register(registry, ModBlocks.ME_GAS_INVENTORY_INPUT_BUS,
            TileMEGasInventoryInputBus.class, "me_gas_inventory_input_bus");

        ModBlocks.ME_INPUT_ASSEMBLY = new BlockMEInputAssembly();
        register(registry, ModBlocks.ME_INPUT_ASSEMBLY,
            TileMEInputAssembly.class, "me_input_assembly");

        ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY =
            new BlockMEInventoryInputAssembly();
        register(registry, ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY,
            TileMEInventoryInputAssembly.class, "me_inventory_input_assembly");

        ModBlocks.ME_OUTPUT_ASSEMBLY = new BlockMEOutputAssembly();
        register(registry, ModBlocks.ME_OUTPUT_ASSEMBLY,
            TileMEOutputAssembly.class, "me_output_assembly");

        ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY =
            new BlockMEFullExposureAssembly();
        register(registry, ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY,
            TileMEFullExposureAssembly.class, "me_full_exposure_assembly");
    }

    private static void register(IForgeRegistry<Block> registry, Block block,
                                 Class<? extends TileEntity> tile, String name) {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, name);
        block.setRegistryName(id);
        registry.register(block);
        GameRegistry.registerTileEntity(tile, id);
    }
}
