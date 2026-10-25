package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.catalyst.CatalystTypes;
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
import net.edwin.mmcecomplement.compat.ae.block.BlockMEChannelInputHatch;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEConnectionShareHatch;
import net.edwin.mmcecomplement.compat.ae.block.BlockAEStorageAccessPort;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.block.BlockAcceleratorHatch;
import net.edwin.mmcecomplement.block.BlockBatchHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneControlHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneSignalInputHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneSignalOutputHatch;
import net.edwin.mmcecomplement.block.BlockMachineSignalInputHatch;
import net.edwin.mmcecomplement.block.BlockMachineSignalOutputHatch;
import net.edwin.mmcecomplement.block.BlockMachineSignalCable;
import net.edwin.mmcecomplement.block.BlockMachineGlass;
import net.edwin.mmcecomplement.block.BlockOverclockHatch;
import net.edwin.mmcecomplement.block.BlockThreadHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidOutputHatch;
import net.edwin.mmcecomplement.block.BlockNineFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockNineFluidOutputHatch;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.AeEnergyRegistryCompat;
import net.edwin.mmcecomplement.compat.ae.AeGasRegistryCompat;
import net.edwin.mmcecomplement.compat.ae.AeFullExposureRegistryCompat;
import net.edwin.mmcecomplement.compat.ae.AeManaRegistryCompat;
import net.edwin.mmcecomplement.compat.flux.FluxRegistryCompat;
import net.edwin.mmcecomplement.compat.mekanism.MekanismTileRegistryCompat;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.init.ModItems;
import net.edwin.mmcecomplement.item.ItemAttachmentConstructTool;
import net.edwin.mmcecomplement.item.ItemBlockMachineSignalCable;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneControlHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalInputHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalOutputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalInputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalOutputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalCable;
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
import net.edwin.mmcecomplement.tile.TileStorageController;
import net.edwin.mmcecomplement.registry.StorageControllerRegistry;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFluidInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEChannelInputHatch;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.item.ItemBlockMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEConnectionShareHatch;
import net.edwin.mmcecomplement.compat.ae.tile.TileAEStorageAccessPort;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import hellfirepvp.modularmachinery.common.item.ItemBlockMEMachineComponent;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponentCustomName;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.minecraftforge.event.RegistryEvent.MissingMappings;

/**
 * Handles Forge registry events for MMCE Complement.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RegistryEvents {

    private RegistryEvents() {}

    /** Handles legacy generic controller blocks already present in a world. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onStorageControllerRightClick(
        PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld() == null || event.getEntityPlayer() == null) return;
        if (event.getEntityPlayer().isSneaking()) {
            // Preserve vanilla's sneaking interaction path (including block
            // placement) for legacy storage controller blocks as well.
            return;
        }
        net.minecraft.tileentity.TileEntity tile = event.getWorld()
            .getTileEntity(event.getPos());
        // Newly registered storage blocks handle activation themselves. Let
        // Block.onBlockActivated run so Forge/vanilla can complete its normal
        // interaction path and avoid cancelling the hand animation.
        if (tile != null && (tile.getBlockType()
            instanceof net.edwin.mmcecomplement.block.BlockStorageController
            || tile.getBlockType()
            instanceof net.edwin.mmcecomplement.block.BlockStorageFactoryController)) {
            // RightClickBlock is fired before the block callback.  Trigger the
            // local animation here as well, because opening a custom GUI can
            // prevent vanilla's normal interaction path from doing it.
            if (event.getWorld().isRemote) {
                event.getEntityPlayer().swingArm(event.getHand());
            }
            // Explicitly allow the block callback.  A number of integration
            // handlers set USE_BLOCK to DENY for generic MMCE controllers;
            // that would otherwise skip the dedicated storage block entirely.
            event.setUseBlock(Event.Result.ALLOW);
            return;
        }
        if (!(tile instanceof net.edwin.mmcecomplement.variant.StorageController)
            || !((net.edwin.mmcecomplement.variant.StorageController) tile)
                .mmceComplement$isStorageController()) {
            return;
        }
        // The handler consumes the interaction before vanilla can animate it.
        // Trigger the same hand swing explicitly so storage controllers retain
        // the normal right-click feedback.
        event.getEntityPlayer().swingArm(event.getHand());
        if (!event.getWorld().isRemote) {
            int gui = tile instanceof net.edwin.mmcecomplement.tile.TileStorageController
                && ((net.edwin.mmcecomplement.tile.TileStorageController) tile)
                    .isStructureFormed()
                ? net.edwin.mmcecomplement.MMCEComplement.GUI_STORAGE
                : net.edwin.mmcecomplement.MMCEComplement.GUI_STORAGE_UNFORMED;
            event.getEntityPlayer().openGui(net.edwin.mmcecomplement.MMCEComplement.instance,
                gui,
                event.getWorld(), event.getPos().getX(), event.getPos().getY(),
                event.getPos().getZ());
        }
        event.setCanceled(true);
    }

    /** Keep worlds made before storage controllers received their dedicated id loadable. */
    @SubscribeEvent
    public static void onBlockMissingMappings(MissingMappings<Block> event) {
        for (MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            ResourceLocation key = mapping.key;
            String path = key.getPath();
            DynamicMachine machine = null;
            if (path.startsWith("storage_") && path.endsWith("_controller")) {
                String machinePath = path.substring("storage_".length(),
                    path.length() - "_controller".length());
                machine = MachineRegistry.getRegistry().getMachine(
                    new ResourceLocation(key.getNamespace(), machinePath));
            } else if (path.endsWith("_storage_controller")) {
                // Previous builds used a suffix; keep placed blocks/items
                // loadable when upgrading to the storage_ prefix.
                machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(
                    key.getNamespace(), path.substring(0,
                        path.length() - "_storage_controller".length())));
            } else if (path.endsWith("_factory_controller")) {
                machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(
                    key.getNamespace(), path.substring(0,
                        path.length() - "_factory_controller".length())));
            } else if (path.endsWith("_controller")) {
                machine = MachineRegistry.getRegistry().getMachine(new ResourceLocation(
                    key.getNamespace(), path.substring(0,
                        path.length() - "_controller".length())));
            }
            if (!(machine instanceof net.edwin.mmcecomplement.variant.StorageMachine)
                || !((net.edwin.mmcecomplement.variant.StorageMachine) (Object) machine)
                    .mmceComplement$isStorageVariant()) {
                continue;
            }
            // Storage variants deliberately have no factory/integrated
            // controller. Legacy ordinary and factory ids both migrate to
            // the one registered storage controller block.
            Block replacement = BlockController.MACHINE_CONTROLLERS.get(machine);
            if (replacement != null) {
                mapping.remap(replacement);
            }
        }
    }

    @SubscribeEvent
    public static void onComponentTypeRegister(
        RegistryEvent.Register<ComponentType> event) {
        event.getRegistry().register(ModMEChannelTypes.COMPONENT);
        event.getRegistry().register(ModMEChannelTypes.SHARE_COMPONENT);
        event.getRegistry().register(ModMEChannelTypes.STORAGE_ACCESS_COMPONENT);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRequirementTypeRegister(RegistryEvent.Register event) {
        if (event.getGenericType() == RequirementType.class) {
            event.getRegistry().register(ModMEChannelTypes.REQUIREMENT);
            CatalystTypes.register(event.getRegistry());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockRegister(RegistryEvent.Register<Block> event) {
        StorageControllerRegistry.registerBlocks(event.getRegistry());
        // Storage controllers are registered by StorageControllerRegistry;
        // they still need one explicit Forge TileEntity id for NBT packets.
        GameRegistry.registerTileEntity(TileStorageController.class,
            new ResourceLocation(Tags.MOD_ID, "storage_controller"));
        if (CompatMods.isFluxCompatLoaded()) {
            FluxRegistryCompat.registerBlocks(event.getRegistry());
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

        ModBlocks.MACHINE_SIGNAL_INPUT_HATCH = new BlockMachineSignalInputHatch();
        ModBlocks.MACHINE_SIGNAL_INPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "machine_signal_input_hatch"));
        event.getRegistry().register(ModBlocks.MACHINE_SIGNAL_INPUT_HATCH);
        GameRegistry.registerTileEntity(TileMachineSignalInputHatch.class,
            new ResourceLocation(Tags.MOD_ID, "machine_signal_input_hatch"));

        ModBlocks.MACHINE_SIGNAL_OUTPUT_HATCH = new BlockMachineSignalOutputHatch();
        ModBlocks.MACHINE_SIGNAL_OUTPUT_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "machine_signal_output_hatch"));
        event.getRegistry().register(ModBlocks.MACHINE_SIGNAL_OUTPUT_HATCH);
        GameRegistry.registerTileEntity(TileMachineSignalOutputHatch.class,
            new ResourceLocation(Tags.MOD_ID, "machine_signal_output_hatch"));

        ModBlocks.MACHINE_SIGNAL_CABLE_VARIANTS =
            new BlockMachineSignalCable[EnumDyeColor.values().length + 1];
        ModBlocks.MACHINE_SIGNAL_CABLE = new BlockMachineSignalCable();
        ModBlocks.MACHINE_SIGNAL_CABLE.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "machine_signal_cable"));
        event.getRegistry().register(ModBlocks.MACHINE_SIGNAL_CABLE);
        ModBlocks.MACHINE_SIGNAL_CABLE_VARIANTS[0] = ModBlocks.MACHINE_SIGNAL_CABLE;
        for (EnumDyeColor color : EnumDyeColor.values()) {
            BlockMachineSignalCable colored = new BlockMachineSignalCable(color);
            colored.setRegistryName(new ResourceLocation(Tags.MOD_ID,
                "machine_signal_cable_" + color.getName()));
            event.getRegistry().register(colored);
            ModBlocks.MACHINE_SIGNAL_CABLE_VARIANTS[color.getMetadata() + 1] = colored;
        }
        GameRegistry.registerTileEntity(TileMachineSignalCable.class,
            new ResourceLocation(Tags.MOD_ID, "machine_signal_cable"));

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
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileDataItemInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "data_input_assembly_hatch"));
        }

        ModBlocks.INPUT_ASSEMBLY_HATCH = new BlockItemInputAssemblyHatch();
        ModBlocks.INPUT_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "input_assembly_hatch"));
        event.getRegistry().register(ModBlocks.INPUT_ASSEMBLY_HATCH);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileItemInputAssemblyHatch.class,
                new ResourceLocation(Tags.MOD_ID, "input_assembly_hatch"));
        }

        ModBlocks.OUTPUT_ASSEMBLY_HATCH = new BlockItemOutputAssemblyHatch();
        ModBlocks.OUTPUT_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "output_assembly_hatch"));
        event.getRegistry().register(ModBlocks.OUTPUT_ASSEMBLY_HATCH);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileItemOutputAssemblyHatch.class,
                new ResourceLocation(Tags.MOD_ID, "output_assembly_hatch"));
        }

        ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH =
            new BlockSelfCycleAssemblyHatch();
        ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "self_cycle_assembly_hatch"));
        event.getRegistry().register(ModBlocks.SELF_CYCLE_ASSEMBLY_HATCH);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileSelfCycleAssemblyHatch.class,
                new ResourceLocation(Tags.MOD_ID,
                    "self_cycle_assembly_hatch"));
        }

        ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY = new BlockQuadFluidInputHatch();
        ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "quad_fluid_input_hatch_tiny"));
        event.getRegistry().register(ModBlocks.QUAD_FLUID_INPUT_HATCH_TINY);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileQuadFluidInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "quad_fluid_input_hatch_tiny"));
        }

        ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY = new BlockQuadFluidOutputHatch();
        ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "quad_fluid_output_hatch_tiny"));
        event.getRegistry().register(ModBlocks.QUAD_FLUID_OUTPUT_HATCH_TINY);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileQuadFluidOutputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "quad_fluid_output_hatch_tiny"));
        }

        ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL = new BlockNineFluidInputHatch();
        ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "nine_fluid_input_hatch_normal"));
        event.getRegistry().register(ModBlocks.NINE_FLUID_INPUT_HATCH_NORMAL);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileNineFluidInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "nine_fluid_input_hatch_normal"));
        }

        ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL = new BlockNineFluidOutputHatch();
        ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "nine_fluid_output_hatch_normal"));
        event.getRegistry().register(ModBlocks.NINE_FLUID_OUTPUT_HATCH_NORMAL);
        if (!CompatMods.isMekanismCompatLoaded()) {
            GameRegistry.registerTileEntity(TileNineFluidOutputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "nine_fluid_output_hatch_normal"));
        }

        if (CompatMods.isMekanismCompatLoaded()) {
            MekanismTileRegistryCompat.registerTiles();
        }

        if (CompatMods.isAeEnergyCompatLoaded()) {
            AeEnergyRegistryCompat.registerBlocks(event.getRegistry());
        }

        if (CompatMods.isAeManaCompatLoaded()) {
            AeManaRegistryCompat.registerBlocks(event.getRegistry());
        }

        if (CompatMods.isAeItemCompatLoaded()) {
            AeFullExposureRegistryCompat.registerBlock(event.getRegistry());
            ModBlocks.ME_DATA_PATTERN_PROVIDER =
                new BlockMEDataPatternProvider();
            ModBlocks.ME_DATA_PATTERN_PROVIDER.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_data_pattern_provider"));
            event.getRegistry().register(
                ModBlocks.ME_DATA_PATTERN_PROVIDER);
            GameRegistry.registerTileEntity(
                TileMEDataPatternProvider.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_data_pattern_provider"));

            ModBlocks.ME_DATA_PATTERN_PROVIDER_II =
                new BlockMEDataPatternProviderII();
            ModBlocks.ME_DATA_PATTERN_PROVIDER_II.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_data_pattern_provider_ii"));
            event.getRegistry().register(ModBlocks.ME_DATA_PATTERN_PROVIDER_II);
            GameRegistry.registerTileEntity(TileMEDataPatternProviderII.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_data_pattern_provider_ii"));

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

            ModBlocks.ME_CONNECTION_SHARE_HATCH =
                new BlockMEConnectionShareHatch();
            ModBlocks.ME_CONNECTION_SHARE_HATCH.setRegistryName(
                new ResourceLocation(Tags.MOD_ID,
                    "me_connection_share_hatch"));
            event.getRegistry().register(ModBlocks.ME_CONNECTION_SHARE_HATCH);
            GameRegistry.registerTileEntity(TileMEConnectionShareHatch.class,
                new ResourceLocation(Tags.MOD_ID,
                    "me_connection_share_hatch"));

            ModBlocks.AE_STORAGE_ACCESS_PORT = new BlockAEStorageAccessPort();
            ModBlocks.AE_STORAGE_ACCESS_PORT.setRegistryName(new ResourceLocation(Tags.MOD_ID, "ae_storage_access_port"));
            event.getRegistry().register(ModBlocks.AE_STORAGE_ACCESS_PORT);
            GameRegistry.registerTileEntity(TileAEStorageAccessPort.class,
                new ResourceLocation(Tags.MOD_ID, "ae_storage_access_port"));

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
            AeGasRegistryCompat.registerBlocks(event.getRegistry());
        }

        if (CompatMods.isMMCEAddonsLoaded()) {
            registerMMCEAddonBlocks(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        StorageControllerRegistry.registerItems(event.getRegistry());
        ModItems.ATTACHMENT_CONSTRUCT_TOOL = new ItemAttachmentConstructTool();
        ModItems.ATTACHMENT_CONSTRUCT_TOOL.setRegistryName(
            new ResourceLocation(Tags.MOD_ID, "attachment_construct_tool"));
        ModItems.ATTACHMENT_CONSTRUCT_TOOL.setTranslationKey(
            "mmce_complement.attachment_construct_tool");
        event.getRegistry().register(ModItems.ATTACHMENT_CONSTRUCT_TOOL);

        if (CompatMods.isMMCEAddonsLoaded()) {
            registerMMCEAddonItems(event);
        }

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

        registerMachineItem(event, ModBlocks.MACHINE_SIGNAL_INPUT_HATCH);
        registerMachineItem(event, ModBlocks.MACHINE_SIGNAL_OUTPUT_HATCH);
        ModItems.MACHINE_SIGNAL_CABLE_ITEMS = new ItemBlockMachineSignalCable[EnumDyeColor.values().length + 1];
        for (int i = 0; i < ModBlocks.MACHINE_SIGNAL_CABLE_VARIANTS.length; i++) {
            BlockMachineSignalCable block = ModBlocks.MACHINE_SIGNAL_CABLE_VARIANTS[i];
            EnumDyeColor color = i == 0 ? null : EnumDyeColor.byMetadata(i - 1);
            ItemBlockMachineSignalCable cable = new ItemBlockMachineSignalCable(block, color);
            cable.setRegistryName(block.getRegistryName());
            setMachineCreativeTab(cable);
            event.getRegistry().register(cable);
            ModItems.MACHINE_SIGNAL_CABLE_ITEMS[i] = cable;
            OreDictionary.registerOre("machineSignalCable", new ItemStack(cable, 1, 0));
        }

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
            && ModBlocks.ME_DATA_PATTERN_PROVIDER != null) {
            registerMEDataPatternProviderItem(event,
                ModBlocks.ME_DATA_PATTERN_PROVIDER);
        }

        if (CompatMods.isAeItemCompatLoaded()
            && ModBlocks.ME_DATA_PATTERN_PROVIDER_II != null) {
            registerMEDataPatternProviderItem(event,
                ModBlocks.ME_DATA_PATTERN_PROVIDER_II);
        }

        if (CompatMods.isAeItemCompatLoaded()
            && ModBlocks.ME_ORE_DICT_INPUT_BUS != null) {
            registerMEItemBlock(event, ModBlocks.ME_PATTERN_PROVIDER_II);
            registerMEItemBlock(event, ModBlocks.ME_CHANNEL_INPUT_HATCH);
            registerMEItemBlock(event, ModBlocks.ME_CONNECTION_SHARE_HATCH);
            registerMEItemBlock(event, ModBlocks.AE_STORAGE_ACCESS_PORT);
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
        }
        if (CompatMods.isAeItemCompatLoaded()
            && ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY != null) {
            registerMEItemBlock(event, ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
        }
    }

    @SubscribeEvent
    public static void onRecipeRegister(RegistryEvent.Register<IRecipe> event) {
        if (ModItems.MACHINE_SIGNAL_CABLE_ITEMS == null) return;
        String[] dyes = {"White", "Orange", "Magenta", "LightBlue", "Yellow", "Lime", "Pink", "Gray",
            "LightGray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"};
        EnumDyeColor[] colors = EnumDyeColor.values();
        for (int i = 0; i < colors.length; i++) {
            ItemStack output = new ItemStack(ModItems.MACHINE_SIGNAL_CABLE_ITEMS[i + 1], 1);
            ResourceLocation recipeId = new ResourceLocation(Tags.MOD_ID,
                "machine_signal_cable_" + colors[i].getName());
            ShapelessOreRecipe recipe = new ShapelessOreRecipe(
                recipeId, output, "dye" + dyes[i], "machineSignalCable", "machineSignalCable",
                "machineSignalCable", "machineSignalCable", "machineSignalCable",
                "machineSignalCable", "machineSignalCable", "machineSignalCable");
            recipe.setRegistryName(recipeId);
            event.getRegistry().register(recipe);
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

    private static void registerMachineItem(RegistryEvent.Register<Item> event, Block block) {
        ItemBlockMachineComponent item = new ItemBlockMachineComponent(block);
        item.setRegistryName(block.getRegistryName());
        setMachineCreativeTab(item);
        event.getRegistry().register(item);
    }

    private static void registerMEDataPatternProviderItem(
        RegistryEvent.Register<Item> event, Block block) {
        ItemBlockMEDataPatternProvider item =
            new ItemBlockMEDataPatternProvider(block);
        item.setRegistryName(block.getRegistryName());
        setMachineCreativeTab(item);
        event.getRegistry().register(item);
    }

    /**
     * Registers the optional MMCE Addons detector blocks without resolving
     * their Addons superclasses while this event subscriber is loaded. Forge
     * discovers this class even when Addons is absent, so direct references to
     * those classes would crash the client before the compatibility check ran.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerMMCEAddonBlocks(RegistryEvent.Register<Block> event) {
        try {
            Block biome = (Block) Class.forName(
                "net.edwin.mmcecomplement.compat.mmcea.block.BlockConfigurableBiomeProvider")
                .getDeclaredConstructor().newInstance();
            biome.setRegistryName(new ResourceLocation(Tags.MOD_ID,
                "configurable_biome_provider"));
            event.getRegistry().register(biome);
            ModBlocks.CONFIGURABLE_BIOME_PROVIDER = biome;
            GameRegistry.registerTileEntity((Class) Class.forName(
                "net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableBiomeProvider"),
                new ResourceLocation(Tags.MOD_ID, "configurable_biome_provider"));

            Block dimension = (Block) Class.forName(
                "net.edwin.mmcecomplement.compat.mmcea.block.BlockConfigurableDimensionProvider")
                .getDeclaredConstructor().newInstance();
            dimension.setRegistryName(new ResourceLocation(Tags.MOD_ID,
                "configurable_dimension_provider"));
            event.getRegistry().register(dimension);
            ModBlocks.CONFIGURABLE_DIMENSION_PROVIDER = dimension;
            GameRegistry.registerTileEntity((Class) Class.forName(
                "net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableDimensionProvider"),
                new ResourceLocation(Tags.MOD_ID, "configurable_dimension_provider"));
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("MMCE Addons detector registration failed", e);
        }
    }

    /** Registers the optional Addons binding tool through the same isolation boundary. */
    private static void registerMMCEAddonItems(RegistryEvent.Register<Item> event) {
        try {
            Item tool = (Item) Class.forName(
                "net.edwin.mmcecomplement.item.ItemMechanicalBindingTool")
                .getDeclaredConstructor().newInstance();
            tool.setRegistryName(new ResourceLocation(Tags.MOD_ID,
                "mechanical_binding_tool"));
            tool.setTranslationKey("mmce_complement.mechanical_binding_tool");
            event.getRegistry().register(tool);
            ModItems.MECHANICAL_BINDING_TOOL = tool;

            registerMachineBlock(event, ModBlocks.CONFIGURABLE_BIOME_PROVIDER);
            registerMachineBlock(event, ModBlocks.CONFIGURABLE_DIMENSION_PROVIDER);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("MMCE Addons binding tool registration failed", e);
        }
    }

    private static void registerMachineBlock(RegistryEvent.Register<Item> event, Block block) {
        ItemBlockMachineComponent item = new ItemBlockMachineComponent(block);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        for (net.edwin.mmcecomplement.block.BlockStorageController block : StorageControllerRegistry.getControllers()) {
            registerStorageControllerModels(block);
        }
        ModelLoader.setCustomModelResourceLocation(ModItems.ATTACHMENT_CONSTRUCT_TOOL, 0,
            new ModelResourceLocation(ModItems.ATTACHMENT_CONSTRUCT_TOOL.getRegistryName(), "inventory"));
        if (CompatMods.isMMCEAddonsLoaded()) {
            ModelLoader.setCustomModelResourceLocation(ModItems.MECHANICAL_BINDING_TOOL, 0,
                new ModelResourceLocation(ModItems.MECHANICAL_BINDING_TOOL.getRegistryName(), "inventory"));
            registerBlockItemModel(ModBlocks.CONFIGURABLE_BIOME_PROVIDER);
            registerBlockItemModel(ModBlocks.CONFIGURABLE_DIMENSION_PROVIDER);
        }
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
        registerBlockItemModel(ModBlocks.MACHINE_SIGNAL_INPUT_HATCH);
        registerBlockItemModel(ModBlocks.MACHINE_SIGNAL_OUTPUT_HATCH);
        registerMachineSignalCableItemModels();
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
            registerBlockItemModel(ModBlocks.ME_DATA_PATTERN_PROVIDER);
            registerBlockItemModel(ModBlocks.ME_DATA_PATTERN_PROVIDER_II);
            registerBlockItemModel(ModBlocks.ME_PATTERN_PROVIDER_II);
            registerBlockItemModel(ModBlocks.ME_CHANNEL_INPUT_HATCH);
            registerBlockItemModel(ModBlocks.ME_CONNECTION_SHARE_HATCH);
            registerBlockItemModel(ModBlocks.AE_STORAGE_ACCESS_PORT);
            registerBlockItemModel(ModBlocks.ME_ORE_DICT_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS);
        }
        if (CompatMods.isAeGasCompatLoaded()) {
            registerBlockItemModel(ModBlocks.ME_GAS_INVENTORY_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_INPUT_ASSEMBLY);
            registerBlockItemModel(ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY);
            registerBlockItemModel(ModBlocks.ME_OUTPUT_ASSEMBLY);
        }
        if (CompatMods.isAeItemCompatLoaded()
            && ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY != null) {
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

    /**
     * Storage controllers have machine-specific registry ids, while MMCE's
     * controller mesh is shared by every controller.  Point both the held
     * item and world states at that shared model; otherwise Forge searches for
     * nonexistent blockstoragecontroller/<machine-id> model files.
     */
    @SideOnly(Side.CLIENT)
    private static void registerStorageControllerModels(Block block) {
        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(
                    "modularmachinery:blockcontroller", "inventory"));
        }
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(
                IBlockState state) {
                EnumFacing facing = state.getValue(BlockController.FACING);
                boolean formed = state.getValue(BlockController.FORMED);
                String variant = "facing=" + facing.getName()
                    + ",formed=" + formed;
                return new ModelResourceLocation(
                    "modularmachinery:blockcontroller", variant);
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private static void registerMachineSignalCableItemModels() {
        if (ModItems.MACHINE_SIGNAL_CABLE_ITEMS == null) return;
        for (ItemBlockMachineSignalCable item : ModItems.MACHINE_SIGNAL_CABLE_ITEMS) {
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation("mmce_complement:machine_signal_cable", "inventory"));
            }
        }
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


