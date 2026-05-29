package net.edwin.mmcecomplement.compat.ae.block;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockMEEnergyInputBus extends BlockMEEnergyBusBase {

    public BlockMEEnergyInputBus() {
        setTranslationKey("mmce_complement.me_energy_input_bus");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileMEEnergyInputBus();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileMEEnergyInputBus();
    }

    @Override
    protected void openGui(EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        if (te instanceof TileMEEnergyInputBus) {
            player.openGui(MMCEComplement.instance, MMCEComplement.GUI_ME_ENERGY_INPUT_BUS, world, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}