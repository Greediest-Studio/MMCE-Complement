package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.block.BlockCustomName;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockVariants;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.mekanism.QuadFluidHatchMekanismFactory;
import net.edwin.mmcecomplement.tile.TileQuadFluidOutputHatch;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/** Four-slot fluid output hatch with the same eight sizes as MMCE. */
public class BlockQuadFluidOutputHatch extends BlockMachineComponent
    implements BlockCustomName, BlockVariants {

    public static final PropertyEnum<FluidHatchSize> SIZE =
        PropertyEnum.create("size", FluidHatchSize.class);

    public BlockQuadFluidOutputHatch() {
        super(Material.IRON);
        Block block = this;
        block.setHardness(2.0F);
        block.setResistance(10.0F);
        super.setSoundType(SoundType.METAL);
        block.setHarvestLevel("pickaxe", 1);
        block.setTranslationKey("mmce_complement.quad_fluid_output_hatch");
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setDefaultState(getDefaultState().withProperty(SIZE, FluidHatchSize.TINY));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileQuadFluidOutputHatch) {
                TileQuadFluidOutputHatch hatch = (TileQuadFluidOutputHatch) tile;
                ItemStack held = player.getHeldItem(hand);
                if (!held.isEmpty() && FluidUtil.getFluidHandler(held) != null) {
                    FluidUtil.interactWithFluidHandler(player, hand, hatch);
                    hatch.markForUpdateSync();
                } else {
                    player.openGui(MMCEComplement.instance,
                        MMCEComplement.GUI_QUAD_FLUID_OUTPUT_HATCH,
                        world, pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        FluidHatchSize size = FluidHatchSize.values()[MathHelper.clamp(
            stack.getMetadata(), 0, FluidHatchSize.values().length - 1)];
        int totalCapacity = size.getSize();
        int capacity = TileQuadFluidOutputHatch.capacityForTotal(totalCapacity);
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.quad_fluid_output_hatch.tip.capacity",
            totalCapacity, capacity));
        if (Mods.MEKANISM.isPresent()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.fluidhatch.tank.mek"));
        }
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.quad_fluid_output_hatch.tip.duplicate"));
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.quad_fluid_output_hatch.tip.mixed"));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (FluidHatchSize size : FluidHatchSize.values()) {
            items.add(new ItemStack(this, 1, size.ordinal()));
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(SIZE).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        FluidHatchSize[] values = FluidHatchSize.values();
        return getDefaultState().withProperty(SIZE,
            values[MathHelper.clamp(meta, 0, values.length - 1)]);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, SIZE);
    }

    @Override
    public String getIdentifierForMeta(int meta) {
        return FluidHatchSize.values()[MathHelper.clamp(
            meta, 0, FluidHatchSize.values().length - 1)].getName();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> states = new LinkedList<>();
        for (FluidHatchSize size : FluidHatchSize.values()) {
            states.add(getDefaultState().withProperty(SIZE, size));
        }
        return states;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return state.getValue(SIZE).getName();
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
        return createHatchTile(FluidHatchSize.values()[MathHelper.clamp(
            meta, 0, FluidHatchSize.values().length - 1)]);
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return createHatchTile(state.getValue(SIZE));
    }

    private static TileEntity createHatchTile(FluidHatchSize size) {
        if (Mods.MEKANISM.isPresent()) {
            return QuadFluidHatchMekanismFactory.createOutput(size);
        }
        return new TileQuadFluidOutputHatch(size);
    }
}
