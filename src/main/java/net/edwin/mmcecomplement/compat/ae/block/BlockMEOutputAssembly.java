package net.edwin.mmcecomplement.compat.ae.block;

import appeng.api.implementations.items.IMemoryCard;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemOutputBus;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Block shell for the mixed item/fluid/gas ME output assembly. */
public class BlockMEOutputAssembly extends BlockMEItemOutputBus {

    public BlockMEOutputAssembly() {
        setTranslationKey("mmce_complement.me_output_assembly");
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEOutputAssembly();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
        IBlockState state, EntityPlayer player, EnumHand hand,
        EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileMEOutputAssembly)) return true;
        TileMEOutputAssembly assembly = (TileMEOutputAssembly) tile;
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() instanceof IMemoryCard
            && handleSettingsTransfer(assembly, (IMemoryCard) held.getItem(),
                player, held)) return true;
        player.openGui(MMCEComplement.instance,
            MMCEComplement.GUI_ME_OUTPUT_ASSEMBLY, world,
            pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos,
        IBlockState state, float chance, int fortune) { }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        Item item = Item.getItemFromBlock(this);
        if (item != null) {
            ItemStack drop = new ItemStack(item);
            if (tile instanceof TileMEOutputAssembly) {
                NBTTagCompound tag = new NBTTagCompound();
                ((TileMEOutputAssembly) tile).writeDropNBT(tag);
                drop.setTagCompound(tag);
            }
            spawnAsEntity(world, pos, drop);
        }
        world.removeTileEntity(pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
        @Nullable EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMEOutputAssembly && stack.hasTagCompound()) {
            ((TileMEOutputAssembly) tile).readDropNBT(stack.getTagCompound());
        }
    }
}
