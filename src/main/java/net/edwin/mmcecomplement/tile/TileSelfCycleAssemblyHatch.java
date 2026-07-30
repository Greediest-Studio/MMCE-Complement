package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.cycle.CycleComponentHandler;
import net.edwin.mmcecomplement.cycle.CycleItemRecipeHandler;
import net.edwin.mmcecomplement.cycle.CycleRuntime;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;

/** Huge-tier item/fluid assembly that returns resources consumed by its recipe. */
public class TileSelfCycleAssemblyHatch extends TileItemInputAssemblyHatch {
    private final IItemHandlerModifiable cycleItemInput =
        new CycleItemInputHandler();
    private final IItemHandlerModifiable cycleItemOutput =
        new CycleItemOutputHandler();
    private IFluidHandler cycleFluidInput;
    private IFluidHandler cycleFluidOutput;

    private final MachineComponent.ItemBus itemInputProvider =
        new MachineComponent.ItemBus(IOType.INPUT) {
            @Override public IItemHandlerModifiable getContainerProvider() {
                return cycleItemInput;
            }
            @Override public long getGroupID() {
                return TileSelfCycleAssemblyHatch.this.getGroupId();
            }
        };
    private final MachineComponent.ItemBus itemOutputProvider =
        new MachineComponent.ItemBus(IOType.OUTPUT) {
            @Override public IItemHandlerModifiable getContainerProvider() {
                return cycleItemOutput;
            }
            @Override public long getGroupID() {
                return TileSelfCycleAssemblyHatch.this.getGroupId();
            }
        };
    private final MachineComponent.FluidHatch fluidInputProvider =
        new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override public IFluidHandler getContainerProvider() {
                return getCycleFluidInputHandler();
            }
            @Override public long getGroupID() {
                return TileSelfCycleAssemblyHatch.this.getGroupId();
            }
        };
    private final MachineComponent.FluidHatch fluidOutputProvider =
        new MachineComponent.FluidHatch(IOType.OUTPUT) {
            @Override public IFluidHandler getContainerProvider() {
                return getCycleFluidOutputHandler();
            }
            @Override public long getGroupID() {
                return TileSelfCycleAssemblyHatch.this.getGroupId();
            }
        };

    public TileSelfCycleAssemblyHatch() {
        super(DataInputAssemblyTier.HUGE);
    }

    @Override
    public MachineComponent.ItemBus provideComponent() {
        return itemInputProvider;
    }

    @Override
    public MachineComponent.FluidHatch getFluidProvider() {
        return fluidInputProvider;
    }

    public MachineComponent.ItemBus getItemOutputProvider() {
        return itemOutputProvider;
    }

    public MachineComponent.FluidHatch getFluidOutputProvider() {
        return fluidOutputProvider;
    }

    protected IFluidHandler createCycleFluidInputHandler() {
        return new CycleFluidInputHandler();
    }

    protected IFluidHandler createCycleFluidOutputHandler() {
        return new CycleFluidOutputHandler();
    }

    protected final IFluidHandler getCycleFluidInputHandler() {
        if (cycleFluidInput == null) cycleFluidInput =
            createCycleFluidInputHandler();
        return cycleFluidInput;
    }

    protected final IFluidHandler getCycleFluidOutputHandler() {
        if (cycleFluidOutput == null) cycleFluidOutput =
            createCycleFluidOutputHandler();
        return cycleFluidOutput;
    }

    private abstract class DelegatingItemHandler
        implements IItemHandlerModifiable, CycleComponentHandler {
        protected IOInventory inventory() { return getInventory(); }
        @Override public int getSlots() { return inventory().getSlots(); }
        @Override public int getSlotLimit(int slot) {
            return inventory().getSlotLimit(slot);
        }
    }

    private final class CycleItemInputHandler extends DelegatingItemHandler {
        @Override public Mode mmceComplement$getCycleMode() { return Mode.INPUT; }
        @Override public ItemStack getStackInSlot(int slot) {
            return inventory().getStackInSlot(slot);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack,
                                              boolean simulate) {
            return inventory().insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount,
                                               boolean simulate) {
            ItemStack extracted = inventory().extractItem(slot, amount, simulate);
            if (!simulate) CycleRuntime.recordItem(
                TileSelfCycleAssemblyHatch.this, slot, extracted);
            return extracted;
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            ItemStack before = inventory().getStackInSlot(slot).copy();
            inventory().setStackInSlot(slot, stack);
            if (CycleRuntime.currentContext() == null || before.isEmpty()) return;
            int afterCount = !stack.isEmpty()
                && ItemStack.areItemsEqual(before, stack)
                && ItemStack.areItemStackTagsEqual(before, stack)
                ? stack.getCount() : 0;
            int extracted = before.getCount() - afterCount;
            if (extracted > 0) {
                ItemStack removed = before.copy();
                removed.setCount(extracted);
                CycleRuntime.recordItem(TileSelfCycleAssemblyHatch.this,
                    slot, removed);
            }
        }
    }

    private final class CycleItemOutputHandler extends DelegatingItemHandler
        implements CycleItemRecipeHandler {
        private final ItemStack blocker = new ItemStack(Blocks.BEDROCK, 64);
        @Override public Mode mmceComplement$getCycleMode() { return Mode.OUTPUT; }
        @Override public ItemStack getStackInSlot(int slot) {
            // Recipe output-space simulation must never treat this hatch as a
            // general buffer. Actual quota-controlled insertion still works.
            return CycleRuntime.currentContext() == null
                ? blocker : inventory().getStackInSlot(slot);
        }
        @Override public ItemStack insertItem(int slot, ItemStack offered,
                                              boolean simulate) {
            if (offered.isEmpty()) return ItemStack.EMPTY;
            int allowance = CycleRuntime.itemAllowance(
                TileSelfCycleAssemblyHatch.this, slot, offered);
            if (allowance <= 0) return offered;
            int attempted = Math.min(allowance, offered.getCount());
            ItemStack limited = offered.copy();
            limited.setCount(attempted);
            ItemStack limitedRemainder = inventory().insertItem(
                slot, limited, simulate);
            int inserted = attempted - limitedRemainder.getCount();
            if (!simulate && inserted > 0) CycleRuntime.consumeItemAllowance(
                TileSelfCycleAssemblyHatch.this, slot, inserted);
            if (inserted >= offered.getCount()) return ItemStack.EMPTY;
            ItemStack remainder = offered.copy();
            remainder.shrink(inserted);
            return remainder;
        }
        @Override public ItemStack extractItem(int slot, int amount,
                                               boolean simulate) {
            return ItemStack.EMPTY;
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            // MMCE bulk output is redirected to the exact insertion method.
        }

        @Override
        public int mmceComplement$insertCycleOutput(ItemStack stack,
                                                    int amount) {
            if (stack.isEmpty() || amount <= 0
                || CycleRuntime.currentContext() == null) return 0;
            int inserted = 0;
            for (int slot = 0; slot < getSlots() && inserted < amount; slot++) {
                ItemStack offered = stack.copy();
                offered.setCount(amount - inserted);
                ItemStack remainder = insertItem(slot, offered, false);
                inserted += offered.getCount() - remainder.getCount();
            }
            return inserted;
        }
    }

    private final class CycleFluidInputHandler
        implements IFluidHandler, CycleComponentHandler {
        @Override public Mode mmceComplement$getCycleMode() { return Mode.INPUT; }
        @Override public IFluidTankProperties[] getTankProperties() {
            return TileSelfCycleAssemblyHatch.this.getTankProperties();
        }
        @Override public int fill(FluidStack resource, boolean doFill) {
            return TileSelfCycleAssemblyHatch.this.fill(resource, doFill);
        }
        @Nullable @Override public FluidStack drain(FluidStack resource,
                                                     boolean doDrain) {
            int[] before = tankAmounts();
            FluidStack result = TileSelfCycleAssemblyHatch.this.drain(
                resource, doDrain);
            if (doDrain && result != null) recordFluidChanges(before, result);
            return result;
        }
        @Nullable @Override public FluidStack drain(int maxDrain,
                                                     boolean doDrain) {
            int[] before = tankAmounts();
            FluidStack result = TileSelfCycleAssemblyHatch.this.drain(
                maxDrain, doDrain);
            if (doDrain && result != null) recordFluidChanges(before, result);
            return result;
        }
    }

    private final class CycleFluidOutputHandler
        implements IFluidHandler, CycleComponentHandler {
        @Override public Mode mmceComplement$getCycleMode() { return Mode.OUTPUT; }
        @Override public IFluidTankProperties[] getTankProperties() {
            IFluidTankProperties[] blocked =
                new IFluidTankProperties[getTankCount()];
            FluidStack dummy = new FluidStack(
                net.minecraftforge.fluids.FluidRegistry.WATER,
                getPerTankCapacity());
            for (int i = 0; i < blocked.length; i++) blocked[i] =
                new FluidTankProperties(dummy, getPerTankCapacity(),
                    false, false);
            return blocked;
        }
        @Override public int fill(FluidStack offered, boolean doFill) {
            if (offered == null || offered.amount <= 0) return 0;
            for (int slot = 0; slot < getTankCount(); slot++) {
                int allowance = CycleRuntime.fluidAllowance(
                    TileSelfCycleAssemblyHatch.this, slot, offered);
                if (allowance <= 0) continue;
                FluidStack limited = offered.copy();
                limited.amount = Math.min(allowance, offered.amount);
                int filled = getTank(slot).fill(limited, doFill);
                if (doFill && filled > 0) CycleRuntime.consumeFluidAllowance(
                    TileSelfCycleAssemblyHatch.this, slot, filled);
                return filled;
            }
            return 0;
        }
        @Nullable @Override public FluidStack drain(FluidStack resource,
                                                     boolean doDrain) {
            return null;
        }
        @Nullable @Override public FluidStack drain(int maxDrain,
                                                     boolean doDrain) {
            return null;
        }
    }

    private int[] tankAmounts() {
        int[] result = new int[getTankCount()];
        for (int i = 0; i < result.length; i++) result[i] =
            getTank(i).getFluidAmount();
        return result;
    }

    private void recordFluidChanges(int[] before, FluidStack resource) {
        for (int i = 0; i < before.length; i++) {
            int extracted = before[i] - getTank(i).getFluidAmount();
            if (extracted <= 0) continue;
            FluidStack perSlot = resource.copy();
            perSlot.amount = extracted;
            CycleRuntime.recordFluid(this, i, perSlot);
        }
    }
}
