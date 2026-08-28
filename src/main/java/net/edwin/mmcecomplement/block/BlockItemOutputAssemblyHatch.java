package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockCustomName;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import hellfirepvp.modularmachinery.common.block.BlockVariants;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.compat.mekanism.ItemOutputAssemblyHatchMekanismFactory;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.config.ModConfig;
import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/** Five-tier item/fluid output assembly without a smart data interface. */
public class BlockItemOutputAssemblyHatch extends BlockMachineComponent
    implements BlockCustomName, BlockVariants {

    public static final PropertyEnum<DataInputAssemblyTier> TIER =
        PropertyEnum.create("tier", DataInputAssemblyTier.class);

    public BlockItemOutputAssemblyHatch() {
        super(Material.IRON);
        Block block = this;
        block.setHardness(2.0F);
        block.setResistance(10.0F);
        super.setSoundType(SoundType.METAL);
        block.setHarvestLevel("pickaxe", 1);
        block.setTranslationKey("mmce_complement.output_assembly_hatch");
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
        setDefaultState(getDefaultState().withProperty(TIER, DataInputAssemblyTier.NORMAL));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileItemOutputAssemblyHatch) {
            TileItemOutputAssemblyHatch hatch = (TileItemOutputAssemblyHatch) world.getTileEntity(pos);
            if (!player.getHeldItem(hand).isEmpty()
                && FluidUtil.getFluidHandler(player.getHeldItem(hand)) != null) {
                FluidUtil.interactWithFluidHandler(player, hand, hatch);
                hatch.markForUpdateSync();
            } else {
                player.openGui(MMCEComplement.instance, MMCEComplement.GUI_OUTPUT_ASSEMBLY_HATCH,
                    world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileItemOutputAssemblyHatch) {
            TileItemOutputAssemblyHatch hatch = (TileItemOutputAssemblyHatch) tile;
            for (int slot = 0; slot < hatch.getInventory().getSlots(); slot++) {
                ItemStack stack = hatch.getInventory().getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    spawnAsEntity(world, pos, stack);
                    hatch.getInventory().setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        DataInputAssemblyTier tier = tierFromOutputMeta(stack.getMetadata());
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.output_assembly_hatch.tip.slots", tier.getItemSlots()));
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.output_assembly_hatch.tip.fluids",
            tier.getFluidTanks(), ModConfig.getInputAssemblyCapacity(tier)));
        if (CompatMods.isMekanismCompatLoaded()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.fluidhatch.tank.mek"));
        }
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.output_assembly_hatch.tip.duplicate"));
    }

    @Override public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        // Keep the creative-tab order intuitive (smallest to largest).  The
        // shared input tier enum predates this block and uses legacy metadata
        // values, so output assemblies use a local, contiguous mapping.
        for (DataInputAssemblyTier tier : DataInputAssemblyTier.values()) {
            items.add(new ItemStack(this, 1, outputMetaForTier(tier)));
        }
    }
    public static int outputMetaForTier(DataInputAssemblyTier tier) {
        return tier == DataInputAssemblyTier.SMALL ? 0 : tier == DataInputAssemblyTier.NORMAL ? 1
            : tier == DataInputAssemblyTier.BIG ? 2 : tier == DataInputAssemblyTier.HUGE ? 3 : 4;
    }
    public static DataInputAssemblyTier tierFromOutputMeta(int meta) {
        switch (Math.max(0, Math.min(4, meta))) {
            case 0: return DataInputAssemblyTier.SMALL;
            case 1: return DataInputAssemblyTier.NORMAL;
            case 2: return DataInputAssemblyTier.BIG;
            case 3: return DataInputAssemblyTier.HUGE;
            default: return DataInputAssemblyTier.LUDICROUS;
        }
    }
    @Override public int damageDropped(IBlockState state) { return getMetaFromState(state); }
    @Override public int getMetaFromState(IBlockState state) { return outputMetaForTier(state.getValue(TIER)); }
    @Override public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TIER, tierFromOutputMeta(meta));
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, TIER); }
    @Override public String getIdentifierForMeta(int meta) { return tierFromOutputMeta(meta).getName(); }
    @Override public Iterable<IBlockState> getValidStates() {
        List<IBlockState> states = new LinkedList<>();
        for (DataInputAssemblyTier tier : DataInputAssemblyTier.values()) states.add(getDefaultState().withProperty(TIER, tier));
        return states;
    }
    @Override public String getBlockStateName(IBlockState state) { return state.getValue(TIER).getName(); }
    @Override @Nonnull public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Override @Nonnull public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override @Nullable public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return createHatchTile(tierFromOutputMeta(meta));
    }
    @Override @Nullable public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return createHatchTile(state.getValue(TIER));
    }
    private static TileEntity createHatchTile(DataInputAssemblyTier tier) {
        return CompatMods.isMekanismCompatLoaded()
            ? ItemOutputAssemblyHatchMekanismFactory.create(tier)
            : new TileItemOutputAssemblyHatch(tier);
    }
}
