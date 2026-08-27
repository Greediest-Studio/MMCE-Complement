package net.edwin.mmcecomplement.compat.ae.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEGasInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEGasInventoryInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/** MMCE gas input bus backed by full-inventory pull semantics. */
public class BlockMEGasInventoryInputBus extends BlockMEGasInputBus {

    public BlockMEGasInventoryInputBus() {
        setTranslationKey("mmce_complement.me_gas_inventory_input_bus");
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEGasInventoryInputBus();
    }
}
