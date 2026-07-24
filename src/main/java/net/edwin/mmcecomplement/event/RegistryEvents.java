package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.BlockCasing;
import net.edwin.mmcecomplement.block.BlockFluxInputHatch;
import net.edwin.mmcecomplement.block.BlockFluxOutputHatch;
import net.edwin.mmcecomplement.block.BlockMachineGlass;
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
}
