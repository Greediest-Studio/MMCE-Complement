package net.edwin.mmcecomplement.compat.ae.block;

import appeng.api.implementations.items.IMemoryCard;
import github.kasuminova.mmce.common.block.appeng.BlockMEItemInputBus;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
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

/** Block shell for the sixteen-channel item/fluid/gas ME input assembly. */
public class BlockMEInputAssembly extends BlockMEItemInputBus {

    public BlockMEInputAssembly() {
        setTranslationKey("mmce_complement.me_input_assembly");
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEInputAssembly();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos,
        IBlockState state, EntityPlayer player, EnumHand hand,
        EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileMEInputAssembly)) return true;
        TileMEInputAssembly assembly = (TileMEInputAssembly) tile;
        ItemStack held = player.getHeldItem(hand);
        Item heldItem = held.getItem();
        if (heldItem instanceof IMemoryCard
            && handleSettingsTransfer(assembly, (IMemoryCard) heldItem,
                player, held)) {
            return true;
        }
        player.openGui(MMCEComplement.instance,
            MMCEComplement.GUI_ME_INPUT_ASSEMBLY, world,
            pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos,
        IBlockState state, float chance, int fortune) {
        // breakBlock emits the single NBT-bearing component drop.
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        Item item = Item.getItemFromBlock(this);
        if (item != null) {
            ItemStack drop = new ItemStack(item);
            if (tile instanceof TileMEInputAssembly) {
                NBTTagCompound tag = new NBTTagCompound();
                ((TileMEInputAssembly) tile).writeDropNBT(tag);
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
        NBTTagCompound tag = stack.getTagCompound();
        TileEntity tile = world.getTileEntity(pos);
        if (tag != null && tile instanceof TileMEInputAssembly) {
            ((TileMEInputAssembly) tile).readDropNBT(tag);
        }
    }
}
