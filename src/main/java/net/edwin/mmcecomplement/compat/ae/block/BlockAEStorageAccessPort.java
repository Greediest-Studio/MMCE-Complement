package net.edwin.mmcecomplement.compat.ae.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent;
import net.edwin.mmcecomplement.compat.ae.tile.TileAEStorageAccessPort;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockAEStorageAccessPort extends BlockMEMachineComponent {
    public BlockAEStorageAccessPort() { setTranslationKey("mmce_complement.ae_storage_access_port"); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                              EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) { return false; }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileAEStorageAccessPort(); }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new TileAEStorageAccessPort(); }
}
