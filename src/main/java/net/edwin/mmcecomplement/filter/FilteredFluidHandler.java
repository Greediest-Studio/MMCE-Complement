package net.edwin.mmcecomplement.filter;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/** One-tank, int-max fluid store with an exact ghost-fluid filter. */
public final class FilteredFluidHandler implements FilteredFluidRecipeHandler {

    public static final int CAPACITY = Integer.MAX_VALUE;

    @Nullable private FluidStack stored;
    @Nullable private FluidStack filter;
    private final Runnable changed;

    public FilteredFluidHandler(Runnable changed) {
        this.changed = changed == null ? () -> { } : changed;
    }

    private FilteredFluidHandler(@Nullable FluidStack stored,
                                 @Nullable FluidStack filter) {
        this.changed = () -> { };
        this.stored = copy(stored);
        this.filter = normalizedFilter(filter);
    }

    @Nullable
    public FluidStack getStored() {
        return copy(stored);
    }

    @Nullable
    public FluidStack getFilter() {
        return copy(filter);
    }

    public void setFilter(@Nullable FluidStack stack) {
        FluidStack normalized = normalizedFilter(stack);
        if (same(filter, normalized)) return;
        filter = normalized;
        changed.run();
    }

    public boolean accepts(@Nullable FluidStack stack) {
        return filter != null && stack != null && filter.isFluidEqual(stack);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] {
            new FluidTankProperties(copy(stored), CAPACITY, true, true)
        };
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !accepts(resource)
            || (stored != null && stored.amount > 0
                && !stored.isFluidEqual(resource))) {
            return 0;
        }
        int current = stored == null ? 0 : Math.max(0, stored.amount);
        int accepted = (int) Math.min((long) resource.amount,
            (long) CAPACITY - current);
        if (doFill && accepted > 0) {
            if (stored == null || stored.amount <= 0) {
                stored = resource.copy();
                stored.amount = accepted;
            } else {
                stored.amount += accepted;
            }
            changed.run();
        }
        return accepted;
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(FluidStack resource,
                                         boolean doDrain) {
        if (resource == null || resource.amount <= 0 || stored == null
            || stored.amount <= 0 || !stored.isFluidEqual(resource)) {
            return null;
        }
        return drain(resource.amount, doDrain);
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0 || stored == null || stored.amount <= 0) return null;
        int amount = Math.min(maxDrain, stored.amount);
        FluidStack result = stored.copy();
        result.amount = amount;
        if (doDrain) {
            stored.amount -= amount;
            if (stored.amount <= 0) stored = null;
            changed.run();
        }
        return result;
    }

    @Override
    public FilteredFluidRecipeHandler copyForSimulation() {
        return new FilteredFluidHandler(stored, filter);
    }

    public void load(@Nullable FluidStack storedStack,
                     @Nullable FluidStack filterStack) {
        stored = copy(storedStack);
        filter = normalizedFilter(filterStack);
    }

    @Nullable
    private static FluidStack normalizedFilter(@Nullable FluidStack stack) {
        FluidStack result = copy(stack);
        if (result != null) result.amount = 1;
        return result;
    }

    @Nullable
    private static FluidStack copy(@Nullable FluidStack stack) {
        return stack == null || stack.amount <= 0 ? null : stack.copy();
    }

    private static boolean same(@Nullable FluidStack first,
                                @Nullable FluidStack second) {
        return first == null ? second == null
            : second != null && first.isFluidEqual(second);
    }
}
