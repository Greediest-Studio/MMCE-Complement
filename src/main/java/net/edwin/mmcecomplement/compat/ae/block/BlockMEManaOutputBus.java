package net.edwin.mmcecomplement.compat.ae.block;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaOutputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockMEManaOutputBus extends BlockMEEnergyBusBase {

    public BlockMEManaOutputBus() {
        setTranslationKey("mmce_complement.me_mana_output_bus");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileMEManaOutputBus();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileMEManaOutputBus();
    }

    @Override
    protected void openGui(EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        if (te instanceof TileMEManaOutputBus) {
            player.openGui(MMCEComplement.instance, MMCEComplement.GUI_ME_MANA_OUTPUT_BUS, world, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
