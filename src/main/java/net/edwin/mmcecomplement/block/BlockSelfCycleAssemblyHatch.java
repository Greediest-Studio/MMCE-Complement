package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.base.Mods;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.compat.mekanism.SelfCycleAssemblyHatchMekanismFactory;
import net.edwin.mmcecomplement.config.ModConfig;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Single-tier bidirectional assembly for self-cycling recipes. */
public class BlockSelfCycleAssemblyHatch extends BlockItemInputAssemblyHatch {
    public BlockSelfCycleAssemblyHatch() {
        super();
        setTranslationKey("mmce_complement.self_cycle_assembly_hatch");
        setDefaultState(getDefaultState().withProperty(
            TIER, DataInputAssemblyTier.HUGE));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
                                    net.minecraft.block.state.IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY,
                                    float hitZ) {
        if (!world.isRemote && world.getTileEntity(pos)
            instanceof TileSelfCycleAssemblyHatch) {
            player.openGui(MMCEComplement.instance,
                MMCEComplement.GUI_SELF_CYCLE_ASSEMBLY_HATCH,
                world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        DataInputAssemblyTier tier = DataInputAssemblyTier.HUGE;
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.self_cycle_assembly_hatch.tip.slots",
            tier.getItemSlots()));
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.self_cycle_assembly_hatch.tip.fluids",
            tier.getFluidTanks(), ModConfig.getInputAssemblyCapacity(tier)));
        if (Mods.MEKANISM.isPresent()) tooltip.add(TextFormatting.GRAY
            + I18n.format("tooltip.fluidhatch.tank.mek"));
        tooltip.add(I18n.format(
            "tile.mmce_complement.self_cycle_assembly_hatch.tip.cycle"));
        tooltip.add(I18n.format(
            "tile.mmce_complement.self_cycle_assembly_hatch.tip.priority"));
        tooltip.add(I18n.format("tooltip.groupinput.block"));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this, 1, 0));
    }

    @Override public int damageDropped(
        net.minecraft.block.state.IBlockState state) { return 0; }

    @Override public int getMetaFromState(
        net.minecraft.block.state.IBlockState state) { return 0; }

    @Override public net.minecraft.block.state.IBlockState getStateFromMeta(
        int meta) {
        return getDefaultState().withProperty(TIER, DataInputAssemblyTier.HUGE);
    }

    @Override public String getIdentifierForMeta(int meta) {
        return "self_cycle";
    }

    @Override public Iterable<net.minecraft.block.state.IBlockState>
    getValidStates() {
        return Collections.singletonList(getDefaultState().withProperty(
            TIER, DataInputAssemblyTier.HUGE));
    }

    @Override public String getBlockStateName(
        net.minecraft.block.state.IBlockState state) { return "huge"; }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return createCycleTile();
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
        @Nonnull net.minecraft.block.state.IBlockState state) {
        return createCycleTile();
    }

    private static TileEntity createCycleTile() {
        return Mods.MEKANISM.isPresent()
            ? SelfCycleAssemblyHatchMekanismFactory.create()
            : new TileSelfCycleAssemblyHatch();
    }
}
