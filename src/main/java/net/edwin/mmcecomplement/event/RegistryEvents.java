package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.BlockFluxInputHatch;
import net.edwin.mmcecomplement.block.BlockFluxOutputHatch;
import net.edwin.mmcecomplement.init.ModBlocks;
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
import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;

/**
 * Handles Forge registry events for MMCE Complement.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RegistryEvents {

    private RegistryEvents() {}

    @SubscribeEvent
    public static void onBlockRegister(RegistryEvent.Register<Block> event) {
        ModBlocks.FLUX_INPUT_HATCH = new BlockFluxInputHatch();
        ModBlocks.FLUX_INPUT_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "flux_input_hatch"));
        event.getRegistry().register(ModBlocks.FLUX_INPUT_HATCH);

        ModBlocks.FLUX_OUTPUT_HATCH = new BlockFluxOutputHatch();
        ModBlocks.FLUX_OUTPUT_HATCH.setRegistryName(new ResourceLocation(Tags.MOD_ID, "flux_output_hatch"));
        event.getRegistry().register(ModBlocks.FLUX_OUTPUT_HATCH);

        GameRegistry.registerTileEntity(TileFluxInputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "flux_input_hatch"));
        GameRegistry.registerTileEntity(TileFluxOutputHatch.class,
                new ResourceLocation(Tags.MOD_ID, "flux_output_hatch"));
    }

    @SubscribeEvent
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        ItemBlockMachineComponent inItem = new ItemBlockMachineComponent(ModBlocks.FLUX_INPUT_HATCH);
        inItem.setRegistryName(ModBlocks.FLUX_INPUT_HATCH.getRegistryName());
        inItem.setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
        event.getRegistry().register(inItem);

        ItemBlockMachineComponent outItem = new ItemBlockMachineComponent(ModBlocks.FLUX_OUTPUT_HATCH);
        outItem.setRegistryName(ModBlocks.FLUX_OUTPUT_HATCH.getRegistryName());
        outItem.setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
        event.getRegistry().register(outItem);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        registerBlockItemModel(ModBlocks.FLUX_INPUT_HATCH);
        registerBlockItemModel(ModBlocks.FLUX_OUTPUT_HATCH);
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }
}
