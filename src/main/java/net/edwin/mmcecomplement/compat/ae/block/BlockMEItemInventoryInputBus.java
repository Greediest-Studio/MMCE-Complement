package net.edwin.mmcecomplement.compat.ae.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEItemInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEItemInventoryInputBus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** MMCE item input bus backed by full-inventory pull semantics. */
public class BlockMEItemInventoryInputBus extends BlockMEItemInputBus {

    public BlockMEItemInventoryInputBus() {
        setTranslationKey("mmce_complement.me_item_inventory_input_bus");
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEItemInventoryInputBus();
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos,
                                          IBlockState state, float chance,
                                          int fortune) {
        // breakBlock writes both inventories and the pull state into one drop.
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        Item item = Item.getItemFromBlock(this);
        if (item != null) {
            ItemStack drop = new ItemStack(item);
            if (tile instanceof TileMEItemInventoryInputBus) {
                TileMEItemInventoryInputBus bus =
                    (TileMEItemInventoryInputBus) tile;
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("inventory", bus.getInternalInventory().writeNBT());
                tag.setTag("configInventory",
                    bus.getConfigInventory().writeNBT());
                tag.setBoolean(TileMEItemInventoryInputBus.TAG_ACTIVE_PULL,
                    bus.isActivePull());
                tag.setLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE,
                    bus.getPermanentReserve());
                drop.setTagCompound(tag);
            }
            spawnAsEntity(world, pos, drop);
        }
        world.removeTileEntity(pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                @Nullable EntityLivingBase placer,
                                ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        NBTTagCompound tag = stack.getTagCompound();
        TileEntity tile = world.getTileEntity(pos);
        if (tag != null && tile instanceof TileMEItemInventoryInputBus) {
            TileMEItemInventoryInputBus bus =
                (TileMEItemInventoryInputBus) tile;
            bus.setActivePull(
                tag.getBoolean(TileMEItemInventoryInputBus.TAG_ACTIVE_PULL));
            bus.setPermanentReserve(
                tag.getLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE));
        }
    }
}
