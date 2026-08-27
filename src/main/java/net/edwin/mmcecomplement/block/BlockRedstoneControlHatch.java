package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.edwin.mmcecomplement.redstone.RedstoneControlLogic;
import net.edwin.mmcecomplement.tile.TileRedstoneControlHatch;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** A structure component that pauses its formed machine at a signal threshold. */
public class BlockRedstoneControlHatch extends BlockMachineComponent {

    public static final PropertyBool DISABLED = PropertyBool.create("disabled");

    public BlockRedstoneControlHatch() {
        super(Material.IRON);
        Block block = this;
        block.setHardness(2.0F);
        block.setResistance(10.0F);
        super.setSoundType(SoundType.METAL);
        block.setHarvestLevel("pickaxe", 1);
        block.setTranslationKey("mmce_complement.redstone_control_hatch");
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setDefaultState(blockState.getBaseState().withProperty(DISABLED, false));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileRedstoneControlHatch) {
                TileRedstoneControlHatch hatch = (TileRedstoneControlHatch) tile;
                int threshold = player.isSneaking()
                    ? RedstoneControlLogic.MIN_THRESHOLD
                    : RedstoneControlLogic.nextThreshold(
                        hatch.getShutdownThreshold());
                hatch.setShutdownThreshold(threshold);
                player.sendStatusMessage(new TextComponentTranslation(
                    "message.mmce_complement.redstone_control_hatch.threshold",
                    threshold), true);
            }
        }
        return true;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        syncDisabledState(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        syncDisabledState(world, pos);
    }

    /** Let redstone dust visually and electrically connect to every side. */
    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world,
                                      BlockPos pos, @Nullable EnumFacing side) {
        return true;
    }

    public static void syncDisabledState(World world, BlockPos pos) {
        if (world == null || world.isRemote || !world.isBlockLoaded(pos)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        TileEntity tile = world.getTileEntity(pos);
        if (!(state.getBlock() instanceof BlockRedstoneControlHatch)
            || !(tile instanceof TileRedstoneControlHatch)) {
            return;
        }
        // DISABLED is render-only. Keeping the stored state unchanged prevents
        // MMCE's structure matcher from treating a visual mode change as a
        // different structure block.
        world.notifyBlockUpdate(pos, state, state, 2);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world,
                                      BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        boolean disabled = tile instanceof TileRedstoneControlHatch
            && ((TileRedstoneControlHatch) tile).isShutdownSignalActive();
        return state.withProperty(DISABLED, disabled);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DISABLED, false);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DISABLED);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.format(
            "tile.mmce_complement.redstone_control_hatch.tip.shutdown"));
        tooltip.add(I18n.format(
            "tile.mmce_complement.redstone_control_hatch.tip.configure"));
        tooltip.add(I18n.format(
            "tile.mmce_complement.redstone_control_hatch.tip.reset"));
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
        return new TileRedstoneControlHatch();
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileRedstoneControlHatch();
    }
}
