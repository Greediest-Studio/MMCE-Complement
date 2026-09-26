package net.edwin.mmcecomplement.compat.mmcea.block;

import github.alecsio.mmceaddons.common.hatch.vanilla.BlockBiomeProviderInput;
import net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableBiomeProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Biome detector hatch whose snapshot can be overridden by a binding tool. */
public class BlockConfigurableBiomeProvider extends BlockBiomeProviderInput {

    public BlockConfigurableBiomeProvider() {
        setTranslationKey("mmce_complement.configurable_biome_provider");
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileConfigurableBiomeProvider();
    }
}
