package net.edwin.mmcecomplement.compat.ae.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEFluidInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFluidInventoryInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/** MMCE fluid input bus backed by full-inventory pull semantics. */
public class BlockMEFluidInventoryInputBus extends BlockMEFluidInputBus {

    public BlockMEFluidInventoryInputBus() {
        setTranslationKey("mmce_complement.me_fluid_inventory_input_bus");
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEFluidInventoryInputBus();
    }
}
