package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.BlockCasing;
import net.edwin.mmcecomplement.block.BlockDataItemInputHatch;
import net.edwin.mmcecomplement.block.BlockItemInputAssemblyHatch;
import net.edwin.mmcecomplement.block.BlockItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.block.BlockLiquidEnergizerHatch;
import net.edwin.mmcecomplement.block.BlockSelfCycleAssemblyHatch;
import net.edwin.mmcecomplement.block.BlockFilteredItemOutputHatch;
import net.edwin.mmcecomplement.block.BlockFilteredFluidOutputHatch;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEFluidInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEGasInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEInventoryInputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEOutputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEFullExposureAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEChannelInputHatch;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEPatternProviderII;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.block.BlockFluxInputHatch;
import net.edwin.mmcecomplement.block.BlockFluxOutputHatch;
import net.edwin.mmcecomplement.block.BlockAcceleratorHatch;
import net.edwin.mmcecomplement.block.BlockBatchHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneControlHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneSignalInputHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneSignalOutputHatch;
import net.edwin.mmcecomplement.block.BlockMachineGlass;
import net.edwin.mmcecomplement.block.BlockOverclockHatch;
import net.edwin.mmcecomplement.block.BlockThreadHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidOutputHatch;
import net.edwin.mmcecomplement.block.BlockNineFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockNineFluidOutputHatch;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEEnergyInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEEnergyOutputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEManaInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEManaOutputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyOutputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaOutputBus;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.init.ModItems;
import net.edwin.mmcecomplement.item.ItemAttachmentConstructTool;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneControlHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalInputHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalOutputHatch;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileLiquidEnergizerHatch;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileFilteredItemOutputHatch;
import net.edwin.mmcecomplement.tile.TileFilteredFluidOutputHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidOutputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidOutputHatch;
import net.edwin.mmcecomplement.compat.mekanism.TileQuadFluidInputHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileQuadFluidOutputHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileNineFluidInputHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileNineFluidOutputHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileDataItemInputHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileItemInputAssemblyHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileItemOutputAssemblyHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileSelfCycleAssemblyHatchMekanism;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFluidInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEGasInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInventoryInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEChannelInputHatch;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import hellfirepvp.modularmachinery.common.item.ItemBlockMEMachineComponent;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponentCustomName;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;

/**
 * Handles Forge registry events for MMCE Complement.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RegistryEvents {

    private RegistryEvents() {}

    @SubscribeEvent
    public static void onComponentTypeRegister(
        RegistryEvent.Register<ComponentType> event) {
        event.getRegistry().register(ModMEChannelTypes.COMPONENT);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRequirementTypeRegister(RegistryEvent.Register event) {
        if (event.getGenericType() == RequirementType.class) {
            event.getRegistry().register(ModMEChannelTypes.REQUIREMENT);
        }
    }

    @SubscribeEvent
    public static void onBlockRegister(RegistryEvent.Register<Block> event) {
        if (CompatMods.isFluxCompatLoaded()) {
            ModBlocks.FLUX_INPUT_HATCH = new BlockFluxInputHatch();
            ModBlocks.FLUX_INPUT_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "flux_input_hatch"));
            event.getRegistry().register(ModBlocks.FLUX_INPUT_HATCH);

            ModBlocks.FLUX_OUTPUT_HATCH = new BlockFluxOutputHatch();
            ModBlocks.FLUX_OUTPUT_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "flux_output_hatch"));
            event.getRegistry().register(ModBlocks.FLUX_OUTPUT_HATCH);
        }

        ModBlocks.BLOCK_CASING = new BlockCasing();
        ModBlocks.BLOCK_CASING.setRegistryName(new ResourceLocation(Tags.MOD_ID, "blockcasing"));
        event.getRegistry().register(ModBlocks.BLOCK_CASING);

        ModBlocks.MACHINE_GLASS = new BlockMachineGlass();
        ModBlocks.MACHINE_GLASS.setRegistryName(new ResourceLocation(Tags.MOD_ID, "machine_glass"));
        event.getRegistry().register(ModBlocks.MACHINE_GLASS);

        ModBlocks.THREAD_HATCH = new BlockThreadHatch();
        ModBlocks.THREAD_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "thread_hatch"));
        event.getRegistry().register(ModBlocks.THREAD_HATCH);

        ModBlocks.OVERCLOCK_HATCH = new BlockOverclockHatch();
        ModBlocks.OVERCLOCK_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "overclock_hatch"));
        event.getRegistry().register(ModBlocks.OVERCLOCK_HATCH);

        ModBlocks.ACCELERATOR_HATCH = new BlockAcceleratorHatch();
        ModBlocks.ACCELERATOR_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "accelerator_hatch"));
        event.getRegistry().register(ModBlocks.ACCELERATOR_HATCH);

        ModBlocks.BATCH_HATCH = new BlockBatchHatch();
        ModBlocks.BATCH_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "batch_hatch"));
        event.getRegistry().register(ModBlocks.BATCH_HATCH);

        GameRegistry.registerTileEntity(TileBatchHatch.class,
            new ResourceLocation(Tags.MOD_ID, "batch_hatch"));

        ModBlocks.REDSTONE_CONTROL_HATCH = new BlockRedstoneControlHatch();
        ModBlocks.REDSTONE_CONTROL_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "redstone_control_hatch"));
        event.getRegistry().register(ModBlocks.REDSTONE_CONTROL_HATCH);
        GameRegistry.registerTileEntity(TileRedstoneControlHatch.class,
            new ResourceLocation(Tags.MOD_ID, "redstone_control_hatch"));

        ModBlocks.REDSTONE_SIGNAL_INPUT_HATCH =
            new BlockRedstoneSignalInputHatch();
        ModBlocks.REDSTONE_SIGNAL_INPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "redstone_signal_input_hatch"));
        event.getRegistry().register(ModBlocks.REDSTONE_SIGNAL_INPUT_HATCH);
        GameRegistry.registerTileEntity(TileRedstoneSignalInputHatch.class,
            new ResourceLocation(Tags.MOD_ID, "redstone_signal_input_hatch"));

        ModBlocks.REDSTONE_SIGNAL_OUTPUT_HATCH =
            new BlockRedstoneSignalOutputHatch();
        ModBlocks.REDSTONE_SIGNAL_OUTPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "redstone_signal_output_hatch"));
        event.getRegistry().register(ModBlocks.REDSTONE_SIGNAL_OUTPUT_HATCH);
        GameRegistry.registerTileEntity(TileRedstoneSignalOutputHatch.class,
            new ResourceLocation(Tags.MOD_ID, "redstone_signal_output_hatch"));

        ModBlocks.LIQUID_ENERGIZER_HATCH =
            new BlockLiquidEnergizerHatch();
        ModBlocks.LIQUID_ENERGIZER_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "liquid_energizer_hatch"));
        event.getRegistry().register(ModBlocks.LIQUID_ENERGIZER_HATCH);
        GameRegistry.registerTileEntity(TileLiquidEnergizerHatch.class,
            new ResourceLocation(Tags.MOD_ID, "liquid_energizer_hatch"));

        ModBlocks.FILTERED_ITEM_OUTPUT_HATCH =
            new BlockFilteredItemOutputHatch();
        ModBlocks.FILTERED_ITEM_OUTPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID,
                "filtered_item_output_hatch"));
        event.getRegistry().register(ModBlocks.FILTERED_ITEM_OUTPUT_HATCH);
        GameRegistry.registerTileEntity(TileFilteredItemOutputHatch.class,
            new ResourceLocation(Tags.MOD_ID,
                "filtered_item_output_hatch"));

        ModBlocks.FILTERED_FLUID_OUTPUT_HATCH =
            new BlockFilteredFluidOutputHatch();
        ModBlocks.FILTERED_FLUID_OUTPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID,
                "filtered_fluid_output_hatch"));
        event.getRegistry().register(ModBlocks.FILTERED_FLUID_OUTPUT_HATCH);
        GameRegistry.registerTileEntity(TileFilteredFluidOutputHatch.class,
            new ResourceLocation(Tags.MOD_ID,
                "filtered_fluid_output_hatch"));

        ModBlocks.DATA_INPUT_ASSEMBLY_HATCH = new BlockDataItemInputHatch();
        ModBlocks.DATA_INPUT_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "data_input_assembly_hatch"));
        event.getRegistry().register(ModBlocks.DATA_INPUT_ASSEMBLY_HATCH);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileDataItemInputHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "data_input_assembly_hatch"));
        } else {
            GameRegistry.registerTileEntity(TileDataItemInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "data_input_assembly_hatch"));
        }

        ModBlocks.INPUT_ASSEMBLY_HATCH = new BlockItemInputAssemblyHatch();
        ModBlocks.INPUT_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "input_assembly_hatch"));
        event.getRegistry().register(ModBlocks.INPUT_ASSEMBLY_HATCH);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileItemInputAssemblyHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "input_assembly_hatch"));
        } else {
            GameRegistry.registerTileEntity(TileItemInputAssemblyHatch.class,
                new ResourceLocation(Tags.MOD_ID, "input_assembly_hatch"));
        }

        ModBlocks.OUTPUT_ASSEMBLY_HATCH = new BlockItemOutputAssemblyHatch();
        ModBlocks.OUTPUT_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "output_assembly_hatch"));
        event.getRegistry().register(ModBlocks.OUTPUT_ASSEMBLY_HATCH);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileItemOutputAssemblyHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "output_assembly_hatch"));
        } else {
            GameRegistry.registerTileEntity(TileItemOutputAssemblyHatch.class,
                new ResourceLocation(Tags.MOD_ID, "output_assembly_hatch"));
        }

        ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH =
            new BlockSelfCycleAssemblyHatch();
        ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "self_cycle_assembly_hatch"));
        event.getRegistry().register(ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(
                TileSelfCycleAssemblyHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID,
                    "self_cycle_assembly_hatch"));
        } else {
            GameRegistry.registerTileEntity(TileSelfCycleAssemblyHatch.class,
                new ResourceLocation(Tags.MOD_ID,
                    "self_cycle_assembly_hatch"));
        }

        ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY = new BlockQuadFluidInputHatch();
        ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "quad_fluid_input_hatch_tiny"));
        event.getRegistry().register(ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileQuadFluidInputHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "quad_fluid_input_hatch_tiny"));
        } else {
            GameRegistry.registerTileEntity(TileQuadFluidInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "quad_fluid_input_hatch_tiny"));
        }

        ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY = new BlockQuadFluidOutputHatch();
        ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "quad_fluid_output_hatch_tiny"));
        event.getRegistry().register(ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileQuadFluidOutputHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "quad_fluid_output_hatch_tiny"));
        } else {
            GameRegistry.registerTileEntity(TileQuadFluidOutputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "quad_fluid_output_hatch_tiny"));
        }

        ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL = new BlockNineFluidInputHatch();
        ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "nine_fluid_input_hatch_normal"));
        event.getRegistry().register(ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileNineFluidInputHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "nine_fluid_input_hatch_normal"));
        } else {
            GameRegistry.registerTileEntity(TileNineFluidInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "nine_fluid_input_hatch_normal"));
        }

        ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL = new BlockNineFluidOutputHatch();
        ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "nine_fluid_output_hatch_normal"));
        event.getRegistry().register(ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL);
        if (Mods.MEKANISM.isPresent()) {
            GameRegistry.registerTileEntity(TileNineFluidOutputHatchMekanism.class,
                new ResourceLocation(Tags.MOD_ID, "nine_fluid_output_hatch_normal"));
        } else {
            GameRegistry.registerTileEntity(TileNineFluidOutputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "nine_fluid_output_hatch_normal"));
        }

        if (CompatMods.isFluxCompatLoaded()) {
            GameRegistry.registerTileEntity(TileFluxInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "flux_input_hatch"));
            GameRegistry.registerTileEntity(TileFluxOutputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "flux_output_hatch"));
        }

        if (CompatMods.isAeEnergyCompatLoaded()) {
            ModBlocks.ME_ENERGY_INPUT_BUS = new BlockMEEnergyInputBus();
            ModBlocks.ME_ENERGY_INPUT_BUS.setRegistryName(new ResourceLocation(Tags.MOD_ID, "me_energy_input_bus"));
            event.getRegistry().register(ModBlocks.ME_ENERGY_INPUT_BUS);

            ModBlocks.ME_ENERGY_OUTPUT_BUS = new BlockMEEnergyOutputBus();
            ModBlocks.ME_ENERGY_OUTPUT_BUS.setRegistryName(new ResourceLocation(Tags.MOD_ID, "me_energy_output_bus"));
            event.getRegistry().register(ModBlocks.ME_ENERGY_OUTPUT_BUS);

            GameRegistry.registerTileEntity(TileMEEnergyInputBus.class,
                new ResourceLocation(Tags.MOD_ID, "me_energy_input_bus"));
            GameRegistry.registerTileEntity(TileMEEnergyOutputBus.class,
                new ResourceLocation(Tags.MOD_ID, "me_energy_output_bus"));
        }

        if (CompatMods.isAeManaCompatLoaded()) {
            ModBlocks.ME_MANA_INPUT_BUS = new BlockMEManaInputBus();
            ModBlocks.ME_MANA_INPUT_BUS.setRegistryName(new ResourceLocation(Tags.MOD_ID, "me_mana_input_bus"));
            event.getRegistry().register(ModBlocks.ME_MANA_INPUT_BUS);

            ModBlocks.ME_MANA_OUTPUT_BUS = new BlockMEManaOutputBus();
            ModBlocks.ME_MANA_OUTPUT_BUS.setRegistryName(new ResourceLocation(Tags.MOD_ID, "me_mana_output_bus"));
            event.getRegistry().register(ModBlocks.ME_MANA_OUTPUT_BUS);

            GameRegistry.registerTileEntity(TileMEManaInputBus.class,
                new ResourceLocation(Tags.MOD_ID, "me_mana_input_bus"));
            GameRegistry.registerTileEntity(TileMEManaOutputBus.class,
                new ResourceLocation(Tags.MOD_ID, "me_mana_output_bus"));
        }

        if (CompatMods.isAeItemCompatLoaded()) {
            ModBlocks.ME_PATTERN_PROVIDER_II =
                new BlockMEPatternProviderII();
            ModBlocks.ME_PATTERN_PROVIDER_II.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_pattern_provider_ii"));
            event.getRegistry().register(ModBlocks.ME_PATTERN_PROVIDER_II);
            GameRegistry.registerTileEntity(TileMEPatternProviderII.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_pattern_provider_ii"));

            ModBlocks.ME_CHANNEL_INPUT_HATCH =
                new BlockMEChannelInputHatch();
            ModBlocks.ME_CHANNEL_INPUT_HATCH.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_channel_input_hatch"));
            event.getRegistry().register(ModBlocks.ME_CHANNEL_INPUT_HATCH);
            GameRegistry.registerTileEntity(TileMEChannelInputHatch.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_channel_input_hatch"));

            ModBlocks.ME_ORE_DICT_INPUT_BUS = new BlockMEOreDictInputBus();
            ModBlocks.ME_ORE_DICT_INPUT_BUS.setRegistryName(
                new ResourceLocation(Tags.MOD_ID, "me_ore_dict_input_bus"));
            event.getRegistry().register(ModBlocks.ME_ORE_DICT_INPUT_BUS);
            GameRegistry.registerTileEntity(TileMEOreDictInputBus.class,
                new ResourceLocation(Tags.MOD_ID, "me_ore_dict_input_bus"));

            ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS =
                new BlockMEItemInventoryInputBus();
            ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_item_inventory_input_bus"));
            event.getRegistry().register(
                ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS);
            GameRegistry.registerTileEntity(
                TileMEItemInventoryInputBus.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_item_inventory_input_bus"));

            ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS =
                new BlockMEFluidInventoryInputBus();
            ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_fluid_inventory_input_bus"));
            event.getRegistry().register(
                ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS);
            GameRegistry.registerTileEntity(
                TileMEFluidInventoryInputBus.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_fluid_inventory_input_bus"));
        }

        if (CompatMods.isAeGasCompatLoaded()) {
            ModBlocks.ME_GAS_INVENTORY_INPUT_BUS =
                new BlockMEGasInventoryInputBus();
            ModBlocks.ME_GAS_INVENTORY_INPUT_BUS.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_gas_inventory_input_bus"));
            event.getRegistry().register(
                ModBlocks.ME_GAS_INVENTORY_INPUT_BUS);
            GameRegistry.registerTileEntity(
                TileMEGasInventoryInputBus.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_gas_inventory_input_bus"));

            ModBlocks.ME_INPUT_ASSEMBLY = new BlockMEInputAssembly();
            ModBlocks.ME_INPUT_ASSEMBLY.setRegistryName(
                new ResourceLocation(Tags.MOD_ID, "me_input_assembly"));
            event.getRegistry().register(ModBlocks.ME_INPUT_ASSEMBLY);
            GameRegistry.registerTileEntity(TileMEInputAssembly.class,
                new ResourceLocation(Tags.MOD_ID, "me_input_assembly"));

            ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY =
                new BlockMEInventoryInputAssembly();
            ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_inventory_input_assembly"));
            event.getRegistry().register(ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY);
            GameRegistry.registerTileEntity(TileMEInventoryInputAssembly.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_inventory_input_assembly"));

            ModBlocks.ME_OUTPUT_ASSEMBLY = new BlockMEOutputAssembly();
            ModBlocks.ME_OUTPUT_ASSEMBLY.setRegistryName(
                new ResourceLocation(Tags.MOD_ID, "me_output_assembly"));
            event.getRegistry().register(ModBlocks.ME_OUTPUT_ASSEMBLY);
            GameRegistry.registerTileEntity(TileMEOutputAssembly.class,
                new ResourceLocation(Tags.MOD_ID, "me_output_assembly"));

            ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY =
                new BlockMEFullExposureAssembly();
            ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_full_exposure_assembly"));
            event.getRegistry().register(ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
            GameRegistry.registerTileEntity(TileMEFullExposureAssembly.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_full_exposure_assembly"));
        }
    }

    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        ModItems.ATTACHMENT_CONSTRUCT_TOOL = new ItemAttachmentConstructTool();
        ModItems.ATTACHMENT_CONSTRUCT_TOOL.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "attachment_construct_tool"));
        ModItems.ATTACHMENT_CONSTRUCT_TOOL.setTranslationKey(
            "mmce_complement.attachment_construct_tool");
        event.getRegistry().register(ModItems.ATTACHMENT_CONSTRUCT_TOOL);

        if (CompatMods.isFluxCompatLoaded()) {
            ItemBlockMachineComponent inItem = new ItemBlockMachineComponent(ModBlocks.FLUX_INPUT_HATCH);
            inItem.setRegistryName(ModBlocks.FLUX_INPUT_HATCH.getRegistryName());
            setMachineCreativeTab(inItem);
            event.getRegistry().register(inItem);

            ItemBlockMachineComponent outItem = new ItemBlockMachineComponent(ModBlocks.FLUX_OUTPUT_HATCH);
            outItem.setRegistryName(ModBlocks.FLUX_OUTPUT_HATCH.getRegistryName());
            setMachineCreativeTab(outItem);
            event.getRegistry().register(outItem);
        }

        ItemBlockMachineComponentCustomName blockCasingItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.BLOCK_CASING);
        blockCasingItem.setRegistryName(ModBlocks.BLOCK_CASING.getRegistryName());
        setMachineCreativeTab(blockCasingItem);
        event.getRegistry().register(blockCasingItem);

        ItemBlockMachineComponent machineGlassItem = new ItemBlockMachineComponent(ModBlocks.MACHINE_GLASS);
        machineGlassItem.setRegistryName(ModBlocks.MACHINE_GLASS.getRegistryName());
        setMachineCreativeTab(machineGlassItem);
        event.getRegistry().register(machineGlassItem);

        ItemBlockMachineComponentCustomName threadHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.THREAD_HATCH);
        threadHatchItem.setRegistryName(ModBlocks.THREAD_HATCH.getRegistryName());
        setMachineCreativeTab(threadHatchItem);
        event.getRegistry().register(threadHatchItem);

        ItemBlockMachineComponentCustomName overclockHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.OVERCLOCK_HATCH);
        overclockHatchItem.setRegistryName(ModBlocks.OVERCLOCK_HATCH.getRegistryName());
        setMachineCreativeTab(overclockHatchItem);
        event.getRegistry().register(overclockHatchItem);

        ItemBlockMachineComponentCustomName acceleratorHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.ACCELERATOR_HATCH);
        acceleratorHatchItem.setRegistryName(ModBlocks.ACCELERATOR_HATCH.getRegistryName());
        setMachineCreativeTab(acceleratorHatchItem);
        event.getRegistry().register(acceleratorHatchItem);

        ItemBlockMachineComponent batchHatchItem =
            new ItemBlockMachineComponent(ModBlocks.BATCH_HATCH);
        batchHatchItem.setRegistryName(ModBlocks.BATCH_HATCH.getRegistryName());
        setMachineCreativeTab(batchHatchItem);
        event.getRegistry().register(batchHatchItem);

        ItemBlockMachineComponent redstoneControlHatchItem =
            new ItemBlockMachineComponent(ModBlocks.REDSTONE_CONTROL_HATCH);
        redstoneControlHatchItem.setRegistryName(
            ModBlocks.REDSTONE_CONTROL_HATCH.getRegistryName());
        setMachineCreativeTab(redstoneControlHatchItem);
        event.getRegistry().register(redstoneControlHatchItem);

        ItemBlockMachineComponent redstoneSignalInputHatchItem =
            new ItemBlockMachineComponent(ModBlocks.REDSTONE_SIGNAL_INPUT_HATCH);
        redstoneSignalInputHatchItem.setRegistryName(
            ModBlocks.REDSTONE_SIGNAL_INPUT_HATCH.getRegistryName());
        setMachineCreativeTab(redstoneSignalInputHatchItem);
        event.getRegistry().register(redstoneSignalInputHatchItem);

        ItemBlockMachineComponent redstoneSignalOutputHatchItem =
            new ItemBlockMachineComponent(ModBlocks.REDSTONE_SIGNAL_OUTPUT_HATCH);
        redstoneSignalOutputHatchItem.setRegistryName(
            ModBlocks.REDSTONE_SIGNAL_OUTPUT_HATCH.getRegistryName());
        setMachineCreativeTab(redstoneSignalOutputHatchItem);
        event.getRegistry().register(redstoneSignalOutputHatchItem);

        ItemBlockMachineComponentCustomName liquidEnergizerHatchItem =
            new ItemBlockMachineComponentCustomName(
                ModBlocks.LIQUID_ENERGIZER_HATCH);
        liquidEnergizerHatchItem.setRegistryName(
            ModBlocks.LIQUID_ENERGIZER_HATCH.getRegistryName());
        setMachineCreativeTab(liquidEnergizerHatchItem);
        event.getRegistry().register(liquidEnergizerHatchItem);

        ItemBlockMachineComponent filteredItemOutputHatchItem =
            new ItemBlockMachineComponent(
                ModBlocks.FILTERED_ITEM_OUTPUT_HATCH);
        filteredItemOutputHatchItem.setRegistryName(
            ModBlocks.FILTERED_ITEM_OUTPUT_HATCH.getRegistryName());
        setMachineCreativeTab(filteredItemOutputHatchItem);
        event.getRegistry().register(filteredItemOutputHatchItem);

        ItemBlockMachineComponent filteredFluidOutputHatchItem =
            new ItemBlockMachineComponent(
                ModBlocks.FILTERED_FLUID_OUTPUT_HATCH);
        filteredFluidOutputHatchItem.setRegistryName(
            ModBlocks.FILTERED_FLUID_OUTPUT_HATCH.getRegistryName());
        setMachineCreativeTab(filteredFluidOutputHatchItem);
        event.getRegistry().register(filteredFluidOutputHatchItem);

        ItemBlockMachineComponentCustomName dataItemInputHatchItem =
            new ItemBlockMachineComponentCustomName(
                ModBlocks.DATA_INPUT_ASSEMBLY_HATCH);
        dataItemInputHatchItem.setRegistryName(
            ModBlocks.DATA_INPUT_ASSEMBLY_HATCH.getRegistryName());
        setMachineCreativeTab(dataItemInputHatchItem);
        event.getRegistry().register(dataItemInputHatchItem);

        ItemBlockMachineComponentCustomName inputAssemblyHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.INPUT_ASSEMBLY_HATCH);
        inputAssemblyHatchItem.setRegistryName(ModBlocks.INPUT_ASSEMBLY_HATCH.getRegistryName());
        setMachineCreativeTab(inputAssemblyHatchItem);
        event.getRegistry().register(inputAssemblyHatchItem);

        ItemBlockMachineComponentCustomName outputAssemblyHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.OUTPUT_ASSEMBLY_HATCH);
        outputAssemblyHatchItem.setRegistryName(
            ModBlocks.OUTPUT_ASSEMBLY_HATCH.getRegistryName());
        setMachineCreativeTab(outputAssemblyHatchItem);
        event.getRegistry().register(outputAssemblyHatchItem);

        ItemBlockMachineComponentCustomName selfCycleAssemblyHatchItem =
            new ItemBlockMachineComponentCustomName(
                ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH);
        selfCycleAssemblyHatchItem.setRegistryName(
            ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH.getRegistryName());
        setMachineCreativeTab(selfCycleAssemblyHatchItem);
        event.getRegistry().register(selfCycleAssemblyHatchItem);

        ItemBlockMachineComponentCustomName quadFluidHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY);
        quadFluidHatchItem.setRegistryName(
            ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY.getRegistryName());
        setMachineCreativeTab(quadFluidHatchItem);
        event.getRegistry().register(quadFluidHatchItem);

        ItemBlockMachineComponentCustomName quadFluidOutputHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY);
        quadFluidOutputHatchItem.setRegistryName(
            ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY.getRegistryName());
        setMachineCreativeTab(quadFluidOutputHatchItem);
        event.getRegistry().register(quadFluidOutputHatchItem);

        ItemBlockMachineComponentCustomName nineFluidHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL);
        nineFluidHatchItem.setRegistryName(ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL.getRegistryName());
        setMachineCreativeTab(nineFluidHatchItem);
        event.getRegistry().register(nineFluidHatchItem);

        ItemBlockMachineComponentCustomName nineFluidOutputHatchItem =
            new ItemBlockMachineComponentCustomName(ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL);
        nineFluidOutputHatchItem.setRegistryName(ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL.getRegistryName());
        setMachineCreativeTab(nineFluidOutputHatchItem);
        event.getRegistry().register(nineFluidOutputHatchItem);

        if (CompatMods.isAeEnergyCompatLoaded()) {
            ItemBlockMEMachineComponent inBusItem = new ItemBlockMEMachineComponent(ModBlocks.ME_ENERGY_INPUT_BUS);
            inBusItem.setRegistryName(ModBlocks.ME_ENERGY_INPUT_BUS.getRegistryName());
            setMachineCreativeTab(inBusItem);
            event.getRegistry().register(inBusItem);

            ItemBlockMEMachineComponent outBusItem = new ItemBlockMEMachineComponent(ModBlocks.ME_ENERGY_OUTPUT_BUS);
            outBusItem.setRegistryName(ModBlocks.ME_ENERGY_OUTPUT_BUS.getRegistryName());
            setMachineCreativeTab(outBusItem);
            event.getRegistry().register(outBusItem);
        }

        if (CompatMods.isAeManaCompatLoaded()) {
            ItemBlockMEMachineComponent inBusItem = new ItemBlockMEMachineComponent(ModBlocks.ME_MANA_INPUT_BUS);
            inBusItem.setRegistryName(ModBlocks.ME_MANA_INPUT_BUS.getRegistryName());
            setMachineCreativeTab(inBusItem);
            event.getRegistry().register(inBusItem);

            ItemBlockMEMachineComponent outBusItem = new ItemBlockMEMachineComponent(ModBlocks.ME_MANA_OUTPUT_BUS);
            outBusItem.setRegistryName(ModBlocks.ME_MANA_OUTPUT_BUS.getRegistryName());
            setMachineCreativeTab(outBusItem);
            event.getRegistry().register(outBusItem);
        }

        if (CompatMods.isAeItemCompatLoaded()
            && ModBlocks.ME_ORE_DICT_INPUT_BUS != null) {
            registerMEItemBlock(event, ModBlocks.ME_PATTERN_PROVIDER_II);
            registerMEItemBlock(event, ModBlocks.ME_CHANNEL_INPUT_HATCH);
            ItemBlockMEMachineComponent mineralItem =
                new ItemBlockMEMachineComponent(ModBlocks.ME_ORE_DICT_INPUT_BUS);
            mineralItem.setRegistryName(
                ModBlocks.ME_ORE_DICT_INPUT_BUS.getRegistryName());
            setMachineCreativeTab(mineralItem);
            event.getRegistry().register(mineralItem);

            registerMEItemBlock(event,
                ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS);
            registerMEItemBlock(event,
                ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS);
        }

        if (CompatMods.isAeGasCompatLoaded()
            && ModBlocks.ME_GAS_INVENTORY_INPUT_BUS != null) {
            registerMEItemBlock(event,
                ModBlocks.ME_GAS_INVENTORY_INPUT_BUS);
            registerMEItemBlock(event, ModBlocks.ME_INPUT_ASSEMBLY);
            registerMEItemBlock(event, ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY);
            registerMEItemBlock(event, ModBlocks.ME_OUTPUT_ASSEMBLY);
            registerMEItemBlock(event, ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
        }
    }

    private static void registerMEItemBlock(RegistryEvent.Register<Item> event,
                                            Block block) {
        ItemBlockMEMachineComponent item =
            new ItemBlockMEMachineComponent(block);
        item.setRegistryName(block.getRegistryName());
        setMachineCreativeTab(item);
        event.getRegistry().register(item);
    }

    /**
     * Keep the invocation owner as Minecraft's Item class so the production
     * reobfuscation maps setCreativeTab even for MMCE ItemBlock subclasses.
     */
    private static void setMachineCreativeTab(Item item) {
        item.setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ModItems.ATTACHMENT_CONSTRUCT_TOOL, 0,
            new ModelResourceLocation(ModItems.ATTACHMENT_CONSTRUCT_TOOL.getRegistryName(), "inventory"));
        if (CompatMods.isFluxCompatLoaded()) {
            registerBlockItemModel(ModBlocks.FLUX_INPUT_HATCH);
            registerBlockItemModel(ModBlocks.FLUX_OUTPUT_HATCH);
        }
        registerBlockItemModel(ModBlocks.MACHINE_GLASS);
        registerBlockCasingItemModels();
        registerThreadHatchItemModels();
        registerOverclockHatchItemModels();
        registerAcceleratorHatchItemModels();
        registerBlockItemModel(ModBlocks.BATCH_HATCH);
        registerBlockItemModel(ModBlocks.REDSTONE_CONTROL_HATCH);
        registerBlockItemModel(ModBlocks.REDSTONE_SIGNAL_INPUT_HATCH);
        registerBlockItemModel(ModBlocks.REDSTONE_SIGNAL_OUTPUT_HATCH);
        registerLiquidEnergizerHatchItemModels();
        registerBlockItemModel(ModBlocks.FILTERED_ITEM_OUTPUT_HATCH);
        registerBlockItemModel(ModBlocks.FILTERED_FLUID_OUTPUT_HATCH);
        registerDataInputAssemblyItemModels();
        registerInputAssemblyItemModels();
        registerOutputAssemblyItemModels();
        registerBlockItemModel(ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH);
        registerQuadFluidHatchItemModels();
        registerQuadFluidOutputHatchItemModels();
        registerNineFluidHatchItemModels();
        registerNineFluidOutputHatchItemModels();
        if (CompatMods.isAeEnergyCompatLoaded()) {
            registerBlockItemModel(ModBlocks.ME_ENERGY_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_ENERGY_OUTPUT_BUS);
        }
        if (CompatMods.isAeItemCompatLoaded()) {
            registerBlockItemModel(ModBlocks.ME_PATTERN_PROVIDER_II);
            registerBlockItemModel(ModBlocks.ME_CHANNEL_INPUT_HATCH);
            registerBlockItemModel(ModBlocks.ME_ORE_DICT_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS);
        }
        if (CompatMods.isAeGasCompatLoaded()) {
            registerBlockItemModel(ModBlocks.ME_GAS_INVENTORY_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_INPUT_ASSEMBLY);
            registerBlockItemModel(ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY);
            registerBlockItemModel(ModBlocks.ME_OUTPUT_ASSEMBLY);
            registerBlockItemModel(ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
        }
        if (CompatMods.isAeManaCompatLoaded()) {
            registerBlockItemModel(ModBlocks.ME_MANA_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_MANA_OUTPUT_BUS);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockCasingItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.BLOCK_CASING);
        if (item == null) {
            return;
        }
        BlockCasing.CasingType[] variants = BlockCasing.CasingType.values();
        for (int meta = 0; meta < variants.length; meta++) {
            ModelLoader.setCustomModelResourceLocation(item, meta,
                    new ModelResourceLocation(ModBlocks.BLOCK_CASING.getRegistryName(),
                            "casing=" + variants[meta].getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerLiquidEnergizerHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.LIQUID_ENERGIZER_HATCH);
        if (item == null) {
            return;
        }
        for (FluidHatchSize size : FluidHatchSize.values()) {
            ModelLoader.setCustomModelResourceLocation(item, size.ordinal(),
                new ModelResourceLocation(
                    ModBlocks.LIQUID_ENERGIZER_HATCH.getRegistryName(),
                    "size=" + size.getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerDataInputAssemblyItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.DATA_INPUT_ASSEMBLY_HATCH);
        if (item == null) {
            return;
        }
        for (DataInputAssemblyTier tier : DataInputAssemblyTier.values()) {
            ModelLoader.setCustomModelResourceLocation(item,
                BlockDataItemInputHatch.assemblyMetaForTier(tier),
                new ModelResourceLocation(
                    ModBlocks.DATA_INPUT_ASSEMBLY_HATCH.getRegistryName(),
                    "tier=" + tier.getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerInputAssemblyItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.INPUT_ASSEMBLY_HATCH);
        if (item == null) return;
        for (DataInputAssemblyTier tier : DataInputAssemblyTier.values()) {
            ModelLoader.setCustomModelResourceLocation(item,
                BlockDataItemInputHatch.assemblyMetaForTier(tier),
                new ModelResourceLocation(ModBlocks.INPUT_ASSEMBLY_HATCH.getRegistryName(),
                    "tier=" + tier.getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerOutputAssemblyItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.OUTPUT_ASSEMBLY_HATCH);
        if (item == null) return;
        for (DataInputAssemblyTier tier : DataInputAssemblyTier.values()) {
            ModelLoader.setCustomModelResourceLocation(item,
                BlockItemOutputAssemblyHatch.outputMetaForTier(tier),
                new ModelResourceLocation(ModBlocks.OUTPUT_ASSEMBLY_HATCH.getRegistryName(),
                    "tier=" + tier.getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerThreadHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.THREAD_HATCH);
        if (item == null) {
            return;
        }
        BlockThreadHatch.ThreadHatchType[] variants = BlockThreadHatch.ThreadHatchType.values();
        for (int meta = 0; meta < variants.length; meta++) {
            ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(ModBlocks.THREAD_HATCH.getRegistryName(),
                    "tier=" + variants[meta].getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerOverclockHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.OVERCLOCK_HATCH);
        if (item == null) {
            return;
        }
        BlockOverclockHatch.OverclockHatchType[] variants =
            BlockOverclockHatch.OverclockHatchType.values();
        for (int meta = 0; meta < variants.length; meta++) {
            ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(ModBlocks.OVERCLOCK_HATCH.getRegistryName(),
                    "tier=" + variants[meta].getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerAcceleratorHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.ACCELERATOR_HATCH);
        if (item == null) {
            return;
        }
        BlockAcceleratorHatch.AcceleratorHatchType[] variants =
            BlockAcceleratorHatch.AcceleratorHatchType.values();
        for (int meta = 0; meta < variants.length; meta++) {
            ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(ModBlocks.ACCELERATOR_HATCH.getRegistryName(),
                    "tier=" + variants[meta].getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerQuadFluidHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY);
        if (item == null) {
            return;
        }
        for (FluidHatchSize size : FluidHatchSize.values()) {
            ModelLoader.setCustomModelResourceLocation(item, size.ordinal(),
                new ModelResourceLocation(ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY.getRegistryName(),
                    "size=" + size.getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerQuadFluidOutputHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY);
        if (item == null) {
            return;
        }
        for (FluidHatchSize size : FluidHatchSize.values()) {
            ModelLoader.setCustomModelResourceLocation(item, size.ordinal(),
                new ModelResourceLocation(ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY.getRegistryName(),
                    "size=" + size.getName()));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerNineFluidHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL);
        if (item == null) {
            return;
        }
        for (FluidHatchSize size : FluidHatchSize.values()) {
            if (size.ordinal() >= FluidHatchSize.NORMAL.ordinal()) {
                ModelLoader.setCustomModelResourceLocation(item, size.ordinal(),
                    new ModelResourceLocation(ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL.getRegistryName(),
                        "size=" + size.getName()));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerNineFluidOutputHatchItemModels() {
        Item item = Item.getItemFromBlock(ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL);
        if (item == null) {
            return;
        }
        for (FluidHatchSize size : FluidHatchSize.values()) {
            if (size.ordinal() >= FluidHatchSize.NORMAL.ordinal()) {
                ModelLoader.setCustomModelResourceLocation(item, size.ordinal(),
                    new ModelResourceLocation(ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL.getRegistryName(),
                        "size=" + size.getName()));
            }
        }
    }
}
