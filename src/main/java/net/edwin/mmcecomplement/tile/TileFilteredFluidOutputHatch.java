package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.edwin.mmcecomplement.filter.FilteredFluidHandler;
import net.edwin.mmcecomplement.filter.FilteredFluidOutputComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/** Single int-capacity fluid output whose recipe destination is ghost-filtered. */
public class TileFilteredFluidOutputHatch extends TileColorableMachineComponent
    implements MachineComponentTile {

    private static final String TAG_STORED = "storedFluid";
    private static final String TAG_FILTER = "filterFluid";

    private final FilteredFluidHandler tank =
        new FilteredFluidHandler(this::markForUpdateSync);
    private final MachineComponent<?> component =
        new FilteredFluidOutputComponent(tank);

    public FilteredFluidHandler getTank() {
        return tank;
    }

    public IFluidHandler getTankInteractionHandler() {
        return tank;
    }

    @Nullable
    public FluidStack getFilter() {
        return tank.getFilter();
    }

    public void setFilter(@Nullable FluidStack stack) {
        tank.setFilter(stack);
    }

    public int getStoredAmount() {
        FluidStack stack = tank.getStored();
        return stack == null ? 0 : Math.max(0, stack.amount);
    }

    public void setClientStoredAmount(int amount) {
        FluidStack stored = tank.getStored();
        if (stored != null) stored.amount = Math.max(0, amount);
        tank.load(stored, tank.getFilter());
    }

    @Nullable
    public String getStoredDisplayName() {
        FluidStack stack = tank.getStored();
        return stack == null ? null : stack.getLocalizedName();
    }

    public int getStoredTint() {
        FluidStack stack = tank.getStored();
        return stack == null ? 0xFFFFFF : stack.getFluid().getColor(stack);
    }

    @Nullable
    public ResourceLocation getStoredTexture() {
        FluidStack stack = tank.getStored();
        return stack == null ? null : stack.getFluid().getStill(stack);
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
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability,
                               @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) tank;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        FluidStack stored = compound.hasKey(TAG_STORED)
            ? FluidStack.loadFluidStackFromNBT(
                compound.getCompoundTag(TAG_STORED)) : null;
        FluidStack filter = compound.hasKey(TAG_FILTER)
            ? FluidStack.loadFluidStackFromNBT(
                compound.getCompoundTag(TAG_FILTER)) : null;
        tank.load(stored, filter);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        FluidStack stored = tank.getStored();
        if (stored != null) {
            compound.setTag(TAG_STORED, stored.writeToNBT(
                new NBTTagCompound()));
        }
        FluidStack filter = tank.getFilter();
        if (filter != null) {
            compound.setTag(TAG_FILTER, filter.writeToNBT(
                new NBTTagCompound()));
        }
    }
}
