package net.edwin.mmcecomplement.event;

import hellfirepvp.modularmachinery.common.block.BlockDynamicColor;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Registers block/item color handlers for MMCE Complement machine components.
 *
 * <p>Our hatch blocks use MMCE's tint-indexed model parent, but because they
 * are declared in another mod namespace we must register color handlers on the
 * client ourselves to apply TileColorableMachineComponent's casing color.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class ClientColorEvents {

    private ClientColorEvents() {}

    @SubscribeEvent
    public static void onBlockColors(ColorHandlerEvent.Block event) {
        registerBlock(event, ModBlocks.FLUX_INPUT_HATCH);
        registerBlock(event, ModBlocks.FLUX_OUTPUT_HATCH);
        registerBlock(event, ModBlocks.ME_ENERGY_INPUT_BUS);
        registerBlock(event, ModBlocks.ME_ENERGY_OUTPUT_BUS);
        registerBlock(event, ModBlocks.ME_MANA_INPUT_BUS);
        registerBlock(event, ModBlocks.ME_MANA_OUTPUT_BUS);
    }

    @SubscribeEvent
    public static void onItemColors(ColorHandlerEvent.Item event) {
        registerItem(event, ModBlocks.FLUX_INPUT_HATCH);
        registerItem(event, ModBlocks.FLUX_OUTPUT_HATCH);
        registerItem(event, ModBlocks.ME_ENERGY_INPUT_BUS);
        registerItem(event, ModBlocks.ME_ENERGY_OUTPUT_BUS);
        registerItem(event, ModBlocks.ME_MANA_INPUT_BUS);
        registerItem(event, ModBlocks.ME_MANA_OUTPUT_BUS);
    }

    private static void registerBlock(ColorHandlerEvent.Block event, Block block) {
        if (block == null) {
            return;
        }
        if (!(block instanceof BlockDynamicColor)) {
            return;
        }
        BlockDynamicColor dynamic = (BlockDynamicColor) block;
        event.getBlockColors().registerBlockColorHandler(
                (state, world, pos, tintIndex) -> dynamic.getColorMultiplier(state, world, pos, tintIndex),
                block);
    }

    private static void registerItem(ColorHandlerEvent.Item event, Block block) {
        if (block == null) {
            return;
        }
        if (!(block instanceof BlockDynamicColor)) {
            return;
        }
        BlockDynamicColor dynamic = (BlockDynamicColor) block;
        Item item = Item.getItemFromBlock(block);
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            IBlockAccess world = null;
            BlockPos pos = null;
            return dynamic.getColorMultiplier(block.getDefaultState(), world, pos, tintIndex);
        }, item);
    }
}
