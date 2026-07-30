package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.edwin.mmcecomplement.filter.FilteredItemHandler;
import net.edwin.mmcecomplement.filter.FilteredItemOutputComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;

/** Single int-capacity item output whose recipe destination is ghost-filtered. */
public class TileFilteredItemOutputHatch extends TileColorableMachineComponent
    implements MachineComponentTile {

    private static final String TAG_STORED = "storedItem";
    private static final String TAG_STORED_COUNT = "storedCount";
    private static final String TAG_FILTER = "filterItem";

    private final FilteredItemHandler inventory =
        new FilteredItemHandler(this::markForUpdateSync);
    private final MachineComponent<?> component =
        new FilteredItemOutputComponent(inventory);

    public FilteredItemHandler getInventory() {
        return inventory;
    }

    public ItemStack getFilter() {
        return inventory.getFilter();
    }

    public void setFilter(ItemStack stack) {
        inventory.setFilter(stack);
    }

    public int getStoredCount() {
        ItemStack stack = inventory.getStackInSlot(0);
        return stack.isEmpty() ? 0 : Math.max(0, stack.getCount());
    }

    /** Client-only window-property update; preserves the synchronized template. */
    public void setClientStoredCount(int count) {
        ItemStack stack = inventory.getStackInSlot(0);
        inventory.load(stack, Math.max(0, count), inventory.getFilter());
    }

    @Override
    public MachineComponent<?> provideComponent() {
        return component;
    }

    @Override
    public boolean canGroupInput() {
        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability,
                                 @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability,
                               @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        ItemStack stored = compound.hasKey(TAG_STORED)
            ? new ItemStack(compound.getCompoundTag(TAG_STORED))
            : ItemStack.EMPTY;
        ItemStack filter = compound.hasKey(TAG_FILTER)
            ? new ItemStack(compound.getCompoundTag(TAG_FILTER))
            : ItemStack.EMPTY;
        int count = compound.hasKey(TAG_STORED_COUNT)
            ? Math.max(0, compound.getInteger(TAG_STORED_COUNT))
            : (stored.isEmpty() ? 0 : Math.max(0, stored.getCount()));
        inventory.load(stored, count, filter);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        ItemStack stored = inventory.getStackInSlot(0);
        if (!stored.isEmpty()) {
            ItemStack template = stored.copy();
            template.setCount(1);
            compound.setTag(TAG_STORED, template.writeToNBT(
                new NBTTagCompound()));
            compound.setInteger(TAG_STORED_COUNT, stored.getCount());
        }
        ItemStack filter = inventory.getFilter();
        if (!filter.isEmpty()) {
            compound.setTag(TAG_FILTER, filter.writeToNBT(
                new NBTTagCompound()));
        }
    }
}
