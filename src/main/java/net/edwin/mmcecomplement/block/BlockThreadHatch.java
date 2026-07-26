package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockCustomName;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockVariants;
import net.edwin.mmcecomplement.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

/**
 * A colorable machine component that increases a factory controller's normal
 * recipe-thread limit. If a formed structure contains several thread hatches,
 * only the highest tier is used.
 */
public class BlockThreadHatch extends BlockMachineComponent implements BlockCustomName, BlockVariants {

    public static final PropertyEnum<ThreadHatchType> TIER =
        PropertyEnum.create("tier", ThreadHatchType.class);

    public BlockThreadHatch() {
        super(Material.IRON);
        Block block = this;
        block.setHardness(2.0F);
        block.setResistance(10.0F);
        super.setSoundType(SoundType.METAL);
        block.setHarvestLevel("pickaxe", 1);
        block.setTranslationKey("mmce_complement.thread_hatch");
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    /** Mirrors MMCE's Upgrade Bus tooltip style while showing live config values. */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        int meta = MathHelper.clamp(stack.getMetadata(), 0, ThreadHatchType.values().length - 1);
        double multiplier = ModConfig.threadHatch.getMultipliers()[meta];
        tooltip.add(I18n.format("tile.mmce_complement.thread_hatch.tip",
            formatMultiplier(multiplier)));
        tooltip.add(I18n.format("tile.mmce_complement.thread_hatch.tip.normal_threads_only"));
        tooltip.add(I18n.format(ModConfig.threadHatch.allowStacking
            ? "tile.mmce_complement.thread_hatch.tip.stacking"
            : "tile.mmce_complement.thread_hatch.tip.highest_only"));
    }

    @SideOnly(Side.CLIENT)
    private static String formatMultiplier(double multiplier) {
        return BigDecimal.valueOf(multiplier).stripTrailingZeros().toPlainString();
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (ThreadHatchType type : ThreadHatchType.values()) {
            items.add(new ItemStack(this, 1, type.ordinal()));
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TIER).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        ThreadHatchType[] values = ThreadHatchType.values();
        return ((Block) this).getDefaultState().withProperty(
            TIER, values[MathHelper.clamp(meta, 0, values.length - 1)]);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TIER);
    }

    @Override
    public String getIdentifierForMeta(int meta) {
        return ThreadHatchType.fromMeta(meta).getName();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> states = new LinkedList<>();
        for (ThreadHatchType type : ThreadHatchType.values()) {
            states.add(((Block) this).getDefaultState().withProperty(TIER, type));
        }
        return states;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return state.getValue(TIER).getName();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    public static int getTier(IBlockState state) {
        return state.getValue(TIER).ordinal() + 1;
    }

    public enum ThreadHatchType implements IStringSerializable {
        MK1,
        MK2,
        MK3,
        MK4,
        MK5,
        MK6;

        public static ThreadHatchType fromMeta(int meta) {
            ThreadHatchType[] values = values();
            return values[MathHelper.clamp(meta, 0, values.length - 1)];
        }

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }
}
