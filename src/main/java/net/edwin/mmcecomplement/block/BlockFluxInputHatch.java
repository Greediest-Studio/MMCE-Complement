package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Block for the Wireless Flux Input Hatch.
 *
 * <p>Visually mirrors the Modular Machinery Energy Input Hatch (uses the standard
 * MM colorable component overlay rendering). Right-clicking opens the Flux Networks
 * style GUI provided by {@link net.edwin.mmcecomplement.MMCEComplement#GUI_FLUX_INPUT_HATCH}.
 */
public class BlockFluxInputHatch extends BlockMachineComponent {

    public BlockFluxInputHatch() {
        super(Material.IRON);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setTranslationKey("mmce_complement.flux_input_hatch");
        setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileFluxInputHatch) {
                player.openGui(net.edwin.mmcecomplement.MMCEComplement.instance,
                        net.edwin.mmcecomplement.MMCEComplement.GUI_FLUX_INPUT_HATCH,
                        world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    @Nonnull
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * {@link net.minecraft.block.BlockContainer} defaults to {@code INVISIBLE} which
     * is why the block was rendering as nothing. Force a normal model render.
     */
    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileFluxInputHatch();
    }

    /**
     * {@link hellfirepvp.modularmachinery.common.block.BlockMachineComponent} hardcodes
     * this method to {@code new TileColorableMachineComponent()} (it does NOT delegate
     * to {@link #createNewTileEntity(World, int)}), so we must override it explicitly
     * to make sure our hatch tile is what actually gets placed in the world.
     */
    @Override
    @Nullable
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new TileFluxInputHatch();
    }
}
