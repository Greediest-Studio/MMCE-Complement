package net.edwin.mmcecomplement.compat.ae2s;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.BlockMachineSignalHatch;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.compat.ae.block.BlockStorageInteractionAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileStorageInteractionAssembly;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

public final class Ae2sStorageRegistration {
    public static void registerBlocks(IForgeRegistry<Block> registry) {
        ModBlocks.AE_STORAGE_ACCESS_PORT = register(registry, "ae_storage_access_port", false);
        ModBlocks.STORAGE_INTERACTION_ASSEMBLY = register(registry, "storage_interaction_assembly", true);
        GameRegistry.registerTileEntity(TileStoragePort.class, new ResourceLocation(Tags.MOD_ID, "ae_storage_access_port"));
        GameRegistry.registerTileEntity(TileStorageInteractionAssembly.class, new ResourceLocation(Tags.MOD_ID, "storage_interaction_assembly"));
    }
    private static Block register(IForgeRegistry<Block> registry, String id, boolean pipes) {
        Block block = pipes ? new BlockStorageInteractionAssembly() : new StoragePortBlock(id);
        block.setRegistryName(new ResourceLocation(Tags.MOD_ID, id));
        registry.register(block);
        return block;
    }

    private static final class StoragePortBlock extends BlockMachineSignalHatch {
        StoragePortBlock(String id) { super("mmce_complement." + id); }
        @Override public TileEntity createNewTileEntity(World world, int meta) {
            return new TileStoragePort();
        }
        @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
            TileEntity tile = world.getTileEntity(pos);
            return tile instanceof TileStoragePort && ((TileStoragePort) tile).openGui(player);
        }
        @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileStoragePort) ((TileStoragePort) tile).dropUpgrades();
            super.breakBlock(world, pos, state);
        }
        @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
            net.minecraft.entity.EntityLivingBase entity, net.minecraft.item.ItemStack stack) {
            super.onBlockPlacedBy(world, pos, state, entity, stack);
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileStoragePort && entity instanceof EntityPlayer)
                ((TileStoragePort) tile).bus().getMainNode().setOwningPlayer((EntityPlayer) entity);
        }
    }
}
