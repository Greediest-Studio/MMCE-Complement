package net.edwin.mmcecomplement.compat.ae.block;

import appeng.api.implementations.items.IMemoryCard;
import github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent;
import github.kasuminova.mmce.common.tile.SettingsTransfer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockMEEnergyBusBase extends BlockMEMachineComponent {

    @Override
    public boolean onBlockActivated(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state,
                                    @Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof SettingsTransfer) {
                SettingsTransfer settingsTransfer = (SettingsTransfer) te;
                ItemStack heldItem = playerIn.getHeldItem(hand);
                if (heldItem.getItem() instanceof IMemoryCard) {
                    IMemoryCard memoryCard = (IMemoryCard) heldItem.getItem();
                    if (handleSettingsTransfer(settingsTransfer, memoryCard, playerIn, heldItem)) {
                        return true;
                    }
                }
            }
            openGui(playerIn, worldIn, pos, te);
        }
        return true;
    }

    protected abstract void openGui(EntityPlayer player, World world, BlockPos pos, TileEntity te);
}