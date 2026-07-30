package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.base.Mods;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.compat.mekanism.ItemInputAssemblyHatchMekanismFactory;
import net.edwin.mmcecomplement.config.ModConfig;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Five-tier item/fluid/gas input assembly without a data interface. */
public class BlockItemInputAssemblyHatch extends BlockDataItemInputHatch {

    public BlockItemInputAssemblyHatch() {
        super();
        setTranslationKey("mmce_complement.input_assembly_hatch");
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, net.minecraft.block.state.IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileItemInputAssemblyHatch) {
            player.openGui(MMCEComplement.instance, MMCEComplement.GUI_INPUT_ASSEMBLY_HATCH,
                world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        DataInputAssemblyTier tier = tierFromAssemblyMeta(stack.getMetadata());
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.input_assembly_hatch.tip.slots", tier.getItemSlots()));
        tooltip.add(TextFormatting.GRAY + I18n.format(
            "tile.mmce_complement.input_assembly_hatch.tip.fluids",
            tier.getFluidTanks(),
            ModConfig.getInputAssemblyCapacity(tier)));
        if (Mods.MEKANISM.isPresent()) {
            tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.fluidhatch.tank.mek"));
        }
        tooltip.add(I18n.format("tooltip.groupinput.block"));
    }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return createHatchTile(tierFromAssemblyMeta(meta));
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull net.minecraft.block.state.IBlockState state) {
        return createHatchTile(state.getValue(TIER));
    }

    private static TileEntity createHatchTile(DataInputAssemblyTier tier) {
        if (Mods.MEKANISM.isPresent()) {
            return ItemInputAssemblyHatchMekanismFactory.create(tier);
        }
        return new TileItemInputAssemblyHatch(tier);
    }
}
