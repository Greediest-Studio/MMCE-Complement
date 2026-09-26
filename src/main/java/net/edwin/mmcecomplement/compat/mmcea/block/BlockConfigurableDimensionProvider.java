package net.edwin.mmcecomplement.compat.mmcea.block;

import github.alecsio.mmceaddons.common.hatch.vanilla.BlockDimensionProviderInput;
import net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableDimensionProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Dimension detector hatch whose snapshot can be overridden by a binding tool. */
public class BlockConfigurableDimensionProvider extends BlockDimensionProviderInput {

    public BlockConfigurableDimensionProvider() {
        setTranslationKey("mmce_complement.configurable_dimension_provider");
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileConfigurableDimensionProvider();
    }
}
