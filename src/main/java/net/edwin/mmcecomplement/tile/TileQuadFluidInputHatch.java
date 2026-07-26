package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import net.edwin.mmcecomplement.fluid.QuadTankRouting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.FluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Four isolated input tanks exposed as one MMCE fluid-input component.
 *
 * <p>A material may occupy only one tank. If its existing tank is full, a
 * second tank is deliberately not allocated for it.</p>
 */
public class TileQuadFluidInputHatch extends TileColorableMachineComponent
    implements MachineComponentTile, IFluidHandler {

    public static final int TANK_COUNT = 4;
    private static final String TAG_TANK_PREFIX = "tank";

    protected final HybridTank[] tanks = new HybridTank[TANK_COUNT];
    private final FluidHatchSize hatchSize;
    private final int perTankCapacity;

    public TileQuadFluidInputHatch() {
        this(FluidHatchSize.TINY);
    }

    public TileQuadFluidInputHatch(FluidHatchSize hatchSize) {
        this.hatchSize = hatchSize == null ? FluidHatchSize.TINY : hatchSize;
        this.perTankCapacity = capacityForTotal(this.hatchSize.getSize());
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = createTank(perTankCapacity);
            tanks[i].setCanFill(true);
            tanks[i].setCanDrain(true);
        }
    }

    /** Returns ceil(total / 4), with a defensive lower bound for bad configs. */
    public static int capacityForTotal(int totalCapacity) {
        return (int) Math.max(1L, ((long) totalCapacity + TANK_COUNT - 1L) / TANK_COUNT);
    }

    protected HybridTank createTank(int capacity) {
        return new HybridTank(capacity) {
            @Override
            protected void onContentsChanged() {
                TileQuadFluidInputHatch.this.onTankContentsChanged();
            }
        };
    }

    protected void onTankContentsChanged() {
        markNoUpdateSync();
    }

    public int getPerTankCapacity() {
        return perTankCapacity;
    }

    public FluidHatchSize getHatchSize() {
        return hatchSize;
    }

    public HybridTank getTank(int index) {
        if (index < 0 || index >= tanks.length) {
            throw new IndexOutOfBoundsException("Tank index: " + index);
        }
        return tanks[index];
    }

    /**
     * Handler used by a clicked GUI column. It targets that column while still
     * applying the hatch-wide isolation rule (unlike exposing the raw tank).
     */
    public IFluidHandler getTankInteractionHandler(final int index) {
        getTank(index); // Validate eagerly, before the packet mutates inventory.
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                return new IFluidTankProperties[] {
                    new FluidTankProperties(tanks[index].getFluid(),
                        perTankCapacity, true, true)
                };
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                return fillSelectedTank(index, resource, doFill);
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                return drainSelectedTank(index, resource, doDrain);
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                return drainSelectedTank(index, maxDrain, doDrain);
            }
        };
    }

    private synchronized int fillSelectedTank(int index, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canInsertFluid(resource)) {
            return 0;
        }
        for (int i = 0; i < tanks.length; i++) {
            FluidStack stored = tanks[i].getFluid();
            if (stored != null && stored.amount > 0 && stored.isFluidEqual(resource)) {
                return i == index ? tanks[index].fill(resource, doFill) : 0;
            }
        }
        return isTankOccupied(index) ? 0 : tanks[index].fill(resource, doFill);
    }

    @Nullable
    private synchronized FluidStack drainSelectedTank(
        int index, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }
        FluidStack stored = tanks[index].getFluid();
        return stored != null && stored.amount > 0 && stored.isFluidEqual(resource)
            ? tanks[index].drain(resource, doDrain)
            : null;
    }

    @Nullable
    private synchronized FluidStack drainSelectedTank(
        int index, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        FluidStack stored = tanks[index].getFluid();
        return stored == null || stored.amount <= 0
            ? null
            : tanks[index].drain(maxDrain, doDrain);
    }

    protected boolean isTankOccupied(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid != null && fluid.amount > 0;
    }

    /** Hook used by the Mekanism implementation for cross fluid/gas isolation. */
    protected boolean canInsertFluid(@Nonnull FluidStack resource) {
        return true;
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canInsertFluid(resource)) {
            return 0;
        }

        boolean[] occupied = new boolean[tanks.length];
        boolean[] matching = new boolean[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            occupied[i] = isTankOccupied(i);
            FluidStack stored = tanks[i].getFluid();
            matching[i] = stored != null && stored.amount > 0
                && stored.isFluidEqual(resource);
        }
        int target = QuadTankRouting.findFillTarget(occupied, matching);
        return target < 0 ? 0 : tanks[target].fill(resource, doFill);
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }
        for (HybridTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored != null && stored.amount > 0 && stored.isFluidEqual(resource)) {
                return tank.drain(resource, doDrain);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        for (HybridTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored != null && stored.amount > 0) {
                return tank.drain(maxDrain, doDrain);
            }
        }
        return null;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] properties = new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            properties[i] = new FluidTankProperties(
                tanks[i].getFluid(), perTankCapacity, true, true);
        }
        return properties;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return TileQuadFluidInputHatch.this;
            }
        };
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(compound.getCompoundTag(TAG_TANK_PREFIX + i));
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) {
            NBTTagCompound tankTag = new NBTTagCompound();
            tanks[i].writeToNBT(tankTag);
            writeExtraTankNBT(i, tankTag);
            compound.setTag(TAG_TANK_PREFIX + i, tankTag);
        }
    }

    protected void writeExtraTankNBT(int index, NBTTagCompound tankTag) {
        // Mekanism subclass stores the gas half of each hybrid tank here.
    }

    /** Neutral display API, keeping the client GUI independent from Mekanism. */
    @Nullable
    public String getStoredDisplayName(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid == null || fluid.amount <= 0 ? null : fluid.getLocalizedName();
    }

    public int getStoredAmount(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid == null ? 0 : Math.max(0, fluid.amount);
    }

    public int getStoredTint(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid == null ? 0xFFFFFF : fluid.getFluid().getColor(fluid);
    }

    @Nullable
    public ResourceLocation getStoredTexture(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid == null ? null : fluid.getFluid().getStill(fluid);
    }

    public boolean isGas(int index) {
        return false;
    }
}
