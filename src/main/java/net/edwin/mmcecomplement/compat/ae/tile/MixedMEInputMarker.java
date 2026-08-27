package net.edwin.mmcecomplement.compat.ae.tile;

import mekanism.api.gas.GasStack;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

/** Encodes fluid and gas ghost entries in an item-backed mixed slot. */
public final class MixedMEInputMarker {

    public static final int TYPE_ITEM = 1;
    public static final int TYPE_FLUID = 2;
    public static final int TYPE_GAS = 3;

    private static final String TAG_ROOT = "mmceComplementMixedMEInput";
    private static final String TAG_TYPE = "type";
    private static final String TAG_RESOURCE = "resource";

    private MixedMEInputMarker() { }

    public static int getType(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()
            || !stack.getTagCompound().hasKey(TAG_ROOT, 10)) {
            return stack.isEmpty() ? 0 : TYPE_ITEM;
        }
        int type = stack.getTagCompound().getCompoundTag(TAG_ROOT)
            .getInteger(TAG_TYPE);
        return type == TYPE_FLUID || type == TYPE_GAS ? type : TYPE_ITEM;
    }

    public static boolean isResourceMarker(ItemStack stack) {
        int type = getType(stack);
        return type == TYPE_FLUID || type == TYPE_GAS;
    }

    public static ItemStack fluid(FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) return ItemStack.EMPTY;
        NBTTagCompound resource = new NBTTagCompound();
        FluidStack normalized = fluid.copy();
        normalized.amount = Math.max(1, normalized.amount);
        normalized.writeToNBT(resource);
        return marker(TYPE_FLUID, resource);
    }

    public static ItemStack gas(GasStack gas) {
        if (gas == null || gas.amount <= 0 || gas.getGas() == null) {
            return ItemStack.EMPTY;
        }
        NBTTagCompound resource = new NBTTagCompound();
        GasStack normalized = gas.copy();
        normalized.amount = Math.max(1, normalized.amount);
        normalized.write(resource);
        return marker(TYPE_GAS, resource);
    }

    private static ItemStack marker(int type, NBTTagCompound resource) {
        ItemStack result = new ItemStack(Items.PAPER);
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger(TAG_TYPE, type);
        root.setTag(TAG_RESOURCE, resource);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(TAG_ROOT, root);
        result.setTagCompound(tag);
        return result;
    }

    @Nullable
    public static FluidStack getFluid(ItemStack marker) {
        if (getType(marker) != TYPE_FLUID) return null;
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(resource(marker));
        return fluid == null || fluid.amount <= 0 ? null : fluid;
    }

    @Nullable
    public static GasStack getGas(ItemStack marker) {
        if (getType(marker) != TYPE_GAS) return null;
        GasStack gas = GasStack.readFromNBT(resource(marker));
        return gas == null || gas.amount <= 0 ? null : gas;
    }

    public static ItemStack sanitize(ItemStack marker) {
        int type = getType(marker);
        if (type == TYPE_FLUID) return fluid(getFluid(marker));
        if (type == TYPE_GAS) return gas(getGas(marker));
        if (marker.isEmpty()) return ItemStack.EMPTY;
        ItemStack item = marker.copy();
        item.setCount(Math.max(1, item.getCount()));
        return item;
    }

    public static long getAmount(ItemStack marker) {
        int type = getType(marker);
        if (type == TYPE_FLUID) {
            FluidStack fluid = getFluid(marker);
            return fluid == null ? 0L : fluid.amount;
        }
        if (type == TYPE_GAS) {
            GasStack gas = getGas(marker);
            return gas == null ? 0L : gas.amount;
        }
        return marker.isEmpty() ? 0L : marker.getCount();
    }

    public static ItemStack withAmount(ItemStack marker, long amount) {
        int safe = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, amount));
        int type = getType(marker);
        if (type == TYPE_FLUID) {
            FluidStack fluid = getFluid(marker);
            if (fluid == null) return ItemStack.EMPTY;
            fluid.amount = safe;
            return fluid(fluid);
        }
        if (type == TYPE_GAS) {
            GasStack gas = getGas(marker);
            if (gas == null) return ItemStack.EMPTY;
            gas.amount = safe;
            return gas(gas);
        }
        if (marker.isEmpty()) return ItemStack.EMPTY;
        ItemStack item = marker.copy();
        item.setCount(safe);
        return item;
    }

    private static NBTTagCompound resource(ItemStack marker) {
        return marker.getTagCompound().getCompoundTag(TAG_ROOT)
            .getCompoundTag(TAG_RESOURCE);
    }
}
