package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.BlockCasing;
import net.edwin.mmcecomplement.block.BlockFluxInputHatch;
import net.edwin.mmcecomplement.block.BlockFluxOutputHatch;
import net.edwin.mmcecomplement.block.BlockAcceleratorHatch;
import net.edwin.mmcecomplement.block.BlockBatchHatch;
import net.edwin.mmcecomplement.block.BlockMachineGlass;
import net.edwin.mmcecomplement.block.BlockOverclockHatch;
import net.edwin.mmcecomplement.block.BlockThreadHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidOutputHatch;
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
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidOutputHatch;
import net.edwin.mmcecomplement.compat.mekanism.TileQuadFluidInputHatchMekanism;
import net.edwin.mmcecomplement.compat.mekanism.TileQuadFluidOutputHatchMekanism;
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
        registerQuadFluidHatchItemModels();
        registerQuadFluidOutputHatchItemModels();
        if (CompatMods.isAeEnergyCompatLoaded()) {
            registerBlockItemModel(ModBlocks.ME_ENERGY_INPUT_BUS);
            registerBlockItemModel(ModBlocks.ME_ENERGY_OUTPUT_BUS);
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
}
