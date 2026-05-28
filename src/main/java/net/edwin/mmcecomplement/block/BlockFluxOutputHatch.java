package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
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
 * Block for the Wireless Flux Output Hatch.
 *
 * <p>Mirror of {@link BlockFluxInputHatch} — same MMCE colorable shell, same
 * Flux-Networks-style GUI, but binds to a {@link TileFluxOutputHatch} that
 * pushes its stored buffer into the connected Flux Network.
 */
public class BlockFluxOutputHatch extends BlockMachineComponent {

    public BlockFluxOutputHatch() {
        super(Material.IRON);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setTranslationKey("mmce_complement.flux_output_hatch");
        setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileFluxOutputHatch) {
                player.openGui(net.edwin.mmcecomplement.MMCEComplement.instance,
                        net.edwin.mmcecomplement.MMCEComplement.GUI_FLUX_OUTPUT_HATCH,
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
        return new TileFluxOutputHatch();
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new TileFluxOutputHatch();
    }
}
