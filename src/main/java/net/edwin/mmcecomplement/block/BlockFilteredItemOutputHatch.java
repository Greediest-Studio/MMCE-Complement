package net.edwin.mmcecomplement.block;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.block.BlockMachineComponent;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.tile.TileFilteredItemOutputHatch;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** One-slot item output with an exact ghost filter and int-max capacity. */
public class BlockFilteredItemOutputHatch extends BlockMachineComponent {

    public BlockFilteredItemOutputHatch() {
        super(Material.IRON);
        Block block = this;
        block.setHardness(2.0F);
        block.setResistance(10.0F);
        super.setSoundType(SoundType.METAL);
        block.setHarvestLevel("pickaxe", 1);
        block.setTranslationKey("mmce_complement.filtered_item_output_hatch");
        block.setCreativeTab(CommonProxy.creativeTabModularMachinery);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY,
                                    float hitZ) {
        if (!world.isRemote && world.getTileEntity(pos)
            instanceof TileFilteredItemOutputHatch) {
            player.openGui(MMCEComplement.instance,
                MMCEComplement.GUI_FILTERED_ITEM_OUTPUT_HATCH,
                world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world,
                               @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.format(
            "tile.mmce_complement.filtered_item_output_hatch.tip.1"));
        tooltip.add(I18n.format(
            "tile.mmce_complement.filtered_item_output_hatch.tip.2"));
        tooltip.add(I18n.format(
            "tile.mmce_complement.filtered_output_hatch.tip.capacity"));
    }

    @Nonnull @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Nonnull @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Nullable @Override public TileEntity createNewTileEntity(@Nonnull World world, int meta) { return new TileFilteredItemOutputHatch(); }
    @Nullable @Override public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) { return new TileFilteredItemOutputHatch(); }
}
