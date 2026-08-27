package net.edwin.mmcecomplement.compat.ae.block;

import github.kasuminova.mmce.common.block.appeng.BlockMEItemInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.minecraft.block.Block;
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

/** ME ore-dictionary input bus with controls beside the normal MMCE GUI. */
public class BlockMEOreDictInputBus extends BlockMEItemInputBus {
    public BlockMEOreDictInputBus() {
        super();
        setTranslationKey("mmce_complement.me_ore_dict_input_bus");
    }

    @Override
    public TileEntity createTileEntity(@Nonnull World world,
                                       @Nonnull IBlockState state) {
        return new TileMEOreDictInputBus();
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos,
                                          IBlockState state, float chance,
                                          int fortune) {
        // breakBlock serializes the ME contents into the dropped item.
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        Item item = Item.getItemFromBlock(this);
        if (item != null) {
            ItemStack drop = new ItemStack(item);
            if (tile instanceof TileMEOreDictInputBus) {
                TileMEOreDictInputBus bus =
                    (TileMEOreDictInputBus) tile;
                net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                tag.setTag("inventory", bus.getInternalInventory().writeNBT());
                tag.setTag("configInventory", bus.getConfigInventory().writeNBT());
                tag.setString("oreDictWhitelist", bus.getWhitelist());
                tag.setString("oreDictBlacklist", bus.getBlacklist());
                tag.setBoolean("oreDictActivePull", bus.isActivePull());
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
        if (tag != null && tile instanceof TileMEOreDictInputBus) {
            TileMEOreDictInputBus bus = (TileMEOreDictInputBus) tile;
            bus.setWhitelist(tag.getString("oreDictWhitelist"));
            bus.setBlacklist(tag.getString("oreDictBlacklist"));
            bus.setActivePull(tag.getBoolean("oreDictActivePull"));
            bus.setPermanentReserve(
                tag.getLong(MEInventoryInputBus.TAG_PERMANENT_RESERVE));
        }
    }
}
