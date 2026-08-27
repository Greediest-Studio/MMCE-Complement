package net.edwin.mmcecomplement.block;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalOutputHatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Redstone data source. */
public class BlockRedstoneSignalOutputHatch extends BlockRedstoneInterfaceHatch {

    public BlockRedstoneSignalOutputHatch() {
        super("mmce_complement.redstone_signal_output_hatch",
            MMCEComplement.GUI_REDSTONE_SIGNAL_OUTPUT_HATCH,
            "tile.mmce_complement.redstone_signal_output_hatch.tip");
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos,
                            EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileRedstoneSignalOutputHatch
            ? ((TileRedstoneSignalOutputHatch) tile).getOutputSignal()
            : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos,
                              EnumFacing side) {
        return getWeakPower(state, world, pos, side);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileRedstoneSignalOutputHatch();
    }
}
