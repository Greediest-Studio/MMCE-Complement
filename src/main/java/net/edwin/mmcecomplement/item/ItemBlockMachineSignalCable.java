package net.edwin.mmcecomplement.item;

import hellfirepvp.modularmachinery.common.item.ItemBlockMachineComponent;
import net.edwin.mmcecomplement.tile.TileMachineSignalCable;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.client.resources.I18n;

/** Item form of the signal cable.  The dye is kept in the stack so crafted
 * variants retain their colour when placed. */
public class ItemBlockMachineSignalCable extends ItemBlockMachineComponent {
    public static final String COLOR_TAG = "signalCableColor";

    private final EnumDyeColor defaultColor;
    private final String itemTranslationKey;

    public ItemBlockMachineSignalCable(Block block) {
        this(block, null);
    }

    public ItemBlockMachineSignalCable(Block block, EnumDyeColor defaultColor) {
        super(block);
        this.defaultColor = defaultColor;
        if (defaultColor == null) {
            itemTranslationKey = "mmce_complement.machine_signal_cable";
        } else {
            itemTranslationKey = "mmce_complement.machine_signal_cable_" + defaultColor.getName();
        }
        setTranslationKey(itemTranslationKey);
    }

    /** ItemBlock normally delegates names to its shared block; each cable ID
     * needs its own translation key so the colour appears in tooltips. */
    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item." + itemTranslationKey;
    }

    @Override
    public String getTranslationKey() {
        return "item." + itemTranslationKey;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.format(getTranslationKey(stack) + ".name").trim();
    }

    public EnumDyeColor getDefaultDyeColor() { return defaultColor; }

    public int getPlacedColor(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(COLOR_TAG, 3)) {
            return stack.getTagCompound().getInteger(COLOR_TAG);
        }
        return defaultColor == null ? -1 : defaultColor.getColorValue();
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
                                EnumFacing side, float hitX, float hitY, float hitZ,
                                IBlockState newState) {
        boolean placed = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
        if (placed && !world.isRemote && world.getTileEntity(pos) instanceof TileMachineSignalCable) {
            ((TileMachineSignalCable) world.getTileEntity(pos)).setCableColor(getPlacedColor(stack));
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
        return placed;
    }

    public static ItemStack colored(ItemStack stack, EnumDyeColor color) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        tag.setInteger(COLOR_TAG, color.getColorValue());
        stack.setTagCompound(tag);
        return stack;
    }

    @Override
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(COLOR_TAG, 3)) {
            return stack.getTagCompound().getInteger(COLOR_TAG);
        }
        if (defaultColor != null) return defaultColor.getColorValue();
        return 0xFFFFFF;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
        }
    }
}
