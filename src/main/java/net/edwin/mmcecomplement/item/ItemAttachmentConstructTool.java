package net.edwin.mmcecomplement.item;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.item.ItemConstructTool;
import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.edwin.mmcecomplement.attachment.AttachmentSelectionExporter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** MMCE selection tool that can export only blocks outside the formed machine. */
public class ItemAttachmentConstructTool extends ItemConstructTool {

    @Override
    public EnumActionResult onItemUse(EntityPlayer player,
                                      World world,
                                      BlockPos pos,
                                      EnumHand hand,
                                      EnumFacing facing,
                                      float hitX,
                                      float hitY,
                                      float hitZ) {
        if (world.isRemote || !player.isCreative()
            || !world.getMinecraftServer().getPlayerList().canSendCommands(player.getGameProfile())) {
            return EnumActionResult.SUCCESS;
        }

        IBlockState clicked = world.getBlockState(pos);
        if (player.isSneaking() && clicked.getBlock() instanceof BlockController) {
            try {
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileMultiblockMachineController) {
                    AttachmentSelectionExporter.finalizeSelection(
                        clicked.getValue(BlockController.FACING), world, pos, player,
                        (TileMultiblockMachineController) tile);
                } else {
                    PlayerStructureSelectionHelper.finalizeSelection(
                        clicked.getValue(BlockController.FACING), world, pos, player);
                }
            } catch (RuntimeException exception) {
                ModularMachinery.log.error("Failed to export attachment module selection", exception);
                player.sendMessage(new TextComponentTranslation("message.structurebuild.fail"));
            } finally {
                PlayerStructureSelectionHelper.purgeSelection(player);
                PlayerStructureSelectionHelper.sendSelection(player);
            }
            return EnumActionResult.SUCCESS;
        }

        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack,
                               @Nullable World world,
                               List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(I18n.format("tooltip.mmce_complement.attachment_construct_tool.select"));
        tooltip.add(I18n.format("tooltip.mmce_complement.attachment_construct_tool.export"));
    }
}
