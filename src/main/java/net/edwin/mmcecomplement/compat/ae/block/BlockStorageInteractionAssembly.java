package net.edwin.mmcecomplement.compat.ae.block;

import net.edwin.mmcecomplement.compat.ae.tile.TileStorageInteractionAssembly;
import net.edwin.mmcecomplement.block.BlockMachineSignalHatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/** Plain pipe-access component: no GUI, ME node, or multipart host. */
public class BlockStorageInteractionAssembly extends BlockMachineSignalHatch {
    public BlockStorageInteractionAssembly() {
        super("mmce_complement.storage_interaction_assembly");
    }

    @Override
    public boolean onBlockActivated(@Nonnull World world, @Nonnull BlockPos pos,
                                    @Nonnull IBlockState state,
                                    @Nonnull EntityPlayer player,
                                    @Nonnull EnumHand hand,
                                    @Nonnull EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        return false;
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileStorageInteractionAssembly) ((TileStorageInteractionAssembly) tile).dropLegacyUpgrades();
        super.breakBlock(world, pos, state);
    }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileStorageInteractionAssembly();
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileStorageInteractionAssembly();
    }
}
