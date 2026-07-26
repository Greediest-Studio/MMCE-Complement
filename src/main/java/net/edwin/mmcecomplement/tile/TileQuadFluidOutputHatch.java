package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import net.edwin.mmcecomplement.fluid.QuadTankRouting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.FluidTankProperties;

import javax.annotation.Nullable;

/**
 * Four-slot fluid output hatch.
 *
 * <p>When a machine pushes a fluid, an existing matching tank with room is
 * preferred. Once every matching tank is full, an empty slot is allocated,
 * which deliberately allows the same fluid to occupy multiple slots.</p>
 */
public class TileQuadFluidOutputHatch extends TileQuadFluidInputHatch {

    public TileQuadFluidOutputHatch() {
        super();
    }

    public TileQuadFluidOutputHatch(FluidHatchSize hatchSize) {
        super(hatchSize);
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canInsertFluid(resource)) {
            return 0;
        }

        boolean[] occupied = new boolean[tanks.length];
        boolean[] matching = new boolean[tanks.length];
        boolean[] hasRoom = new boolean[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            FluidStack stored = tanks[i].getFluid();
            occupied[i] = isTankOccupied(i);
            matching[i] = stored != null && stored.amount > 0
                && stored.isFluidEqual(resource);
            hasRoom[i] = tanks[i].getFluidAmount() < getPerTankCapacity();
        }
        int target = QuadTankRouting.findOutputFillTarget(occupied, matching, hasRoom);
        return target < 0 ? 0 : tanks[target].fill(resource, doFill);
    }

    /**
     * GUI/container interaction targets the selected slot, while machine
     * output uses the routing above. This keeps manual bucket interaction
     * deterministic and prevents filling a different visible slot.
     */
    @Override
    public IFluidHandler getTankInteractionHandler(final int index) {
        getTank(index);
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                return new IFluidTankProperties[] {
                    new FluidTankProperties(tanks[index].getFluid(),
                        getPerTankCapacity(), true, true)
                };
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                if (resource == null || resource.amount <= 0
                        || !canInsertFluid(resource)) {
                    return 0;
                }
                FluidStack stored = tanks[index].getFluid();
                if (stored != null && stored.amount > 0
                        && !stored.isFluidEqual(resource)) {
                    return 0;
                }
                return tanks[index].fill(resource, doFill);
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                if (resource == null || resource.amount <= 0) {
                    return null;
                }
                FluidStack stored = tanks[index].getFluid();
                return stored != null && stored.amount > 0
                        && stored.isFluidEqual(resource)
                    ? tanks[index].drain(resource, doDrain) : null;
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                if (maxDrain <= 0 || !isTankOccupied(index)) {
                    return null;
                }
                return tanks[index].drain(maxDrain, doDrain);
            }
        };
    }

    @Override
    public boolean canGroupInput() {
        return false;
    }

    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponent.FluidHatch(IOType.OUTPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return TileQuadFluidOutputHatch.this;
            }
        };
    }
}
