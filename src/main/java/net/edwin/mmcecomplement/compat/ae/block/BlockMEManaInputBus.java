package net.edwin.mmcecomplement.compat.ae.block;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockMEManaInputBus extends BlockMEEnergyBusBase {

    public BlockMEManaInputBus() {
        setTranslationKey("mmce_complement.me_mana_input_bus");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileMEManaInputBus();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileMEManaInputBus();
    }

    @Override
    protected void openGui(EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        if (te instanceof TileMEManaInputBus) {
            player.openGui(MMCEComplement.instance, MMCEComplement.GUI_ME_MANA_INPUT_BUS, world, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
