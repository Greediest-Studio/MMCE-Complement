package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockCustomName;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockVariants;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * Decorative colorable machine casing block with 6 metadata variants.
 */
public class BlockCasing extends BlockMachineComponent implements BlockCustomName, BlockVariants {

    private static final PropertyEnum<CasingType> CASING = PropertyEnum.create("casing", CasingType.class);

    public BlockCasing() {
        super(Material.IRON);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setTranslationKey("mmce_complement.blockcasing");
        setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setDefaultState(getBlockState().getBaseState().withProperty(CASING, CasingType.META0));
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (CasingType type : CasingType.values()) {
            items.add(new ItemStack(this, 1, type.ordinal()));
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(CASING).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        CasingType[] values = CasingType.values();
        return getDefaultState().withProperty(CASING, values[MathHelper.clamp(meta, 0, values.length - 1)]);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CASING);
    }

    @Override
    public String getIdentifierForMeta(int meta) {
        CasingType[] values = CasingType.values();
        return values[MathHelper.clamp(meta, 0, values.length - 1)].getName();
    }

    @Override
    public Iterable<IBlockState> getValidStates() {
        List<IBlockState> ret = new LinkedList<>();
        for (CasingType type : CasingType.values()) {
            ret.add(getDefaultState().withProperty(CASING, type));
        }
        return ret;
    }

    @Override
    public String getBlockStateName(IBlockState state) {
        return state.getValue(CASING).getName();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    public enum CasingType implements IStringSerializable {

        META0,
        META1,
        META2,
        META3,
        META4,
        META5;

        @Override
        public String getName() {
            return name().toLowerCase();
        }
    }
}