package net.edwin.mmcecomplement.compat.ae.block;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyOutputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockMEEnergyOutputBus extends BlockMEEnergyBusBase {

    public BlockMEEnergyOutputBus() {
        setTranslationKey("mmce_complement.me_energy_output_bus");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileMEEnergyOutputBus();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileMEEnergyOutputBus();
    }

    @Override
    protected void openGui(EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        if (te instanceof TileMEEnergyOutputBus) {
            player.openGui(MMCEComplement.instance, MMCEComplement.GUI_ME_ENERGY_OUTPUT_BUS, world, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}