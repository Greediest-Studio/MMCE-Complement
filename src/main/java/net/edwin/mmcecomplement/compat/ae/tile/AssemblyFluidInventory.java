package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.storage.data.IAEFluidStack;
import appeng.fluids.util.AEFluidInventory;
import appeng.fluids.util.AEFluidStack;
import appeng.fluids.util.IAEFluidInventory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Stable assembly tank implementation for both legacy AE2 and the official AE2S shim. */
public final class AssemblyFluidInventory extends AEFluidInventory implements IFluidHandler {
    private int capacity;
    public AssemblyFluidInventory(IAEFluidInventory host, int slots, int capacity) {
        super(host, slots);
        setCapacity(capacity);
    }
    @Override public void setCapacity(int capacity) { this.capacity = Math.max(0, capacity); }
    @Override public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] result = new IFluidTankProperties[getSlots()];
        for (int slot = 0; slot < result.length; slot++) {
            IAEFluidStack stored = getFluidInSlot(slot);
            result[slot] = new FluidTankProperties(stored == null ? null : stored.getFluidStack(), capacity);
        }
        return result;
    }
    @Override public int fill(int slot, FluidStack input, boolean execute) {
        if (slot < 0 || slot >= getSlots() || input == null || input.amount <= 0) return 0;
        IAEFluidStack stored = getFluidInSlot(slot);
        if (stored != null && !stored.getFluidStack().isFluidEqual(input)) return 0;
        long current = stored == null ? 0 : stored.getStackSize();
        int accepted = (int) Math.min(input.amount, Math.max(0L, capacity - current));
        if (execute && accepted > 0) {
            IAEFluidStack updated = AEFluidStack.fromFluidStack(input.copy());
            updated.setStackSize(current + accepted);
            setFluidInSlot(slot, updated);
        }
        return accepted;
    }
    @Override public FluidStack drain(int slot, FluidStack request, boolean execute) {
        if (request == null || slot < 0 || slot >= getSlots()) return null;
        IAEFluidStack stored = getFluidInSlot(slot);
        return stored != null && stored.getFluidStack().isFluidEqual(request) ? drain(slot, request.amount, execute) : null;
    }
    @Override public FluidStack drain(int slot, int amount, boolean execute) {
        if (slot < 0 || slot >= getSlots() || amount <= 0) return null;
        IAEFluidStack stored = getFluidInSlot(slot);
        if (stored == null || stored.getStackSize() <= 0) return null;
        int drained = (int) Math.min(amount, stored.getStackSize());
        FluidStack result = stored.getFluidStack().copy(); result.amount = drained;
        if (execute) {
            IAEFluidStack remainder = stored.copy(); remainder.setStackSize(stored.getStackSize() - drained);
            setFluidInSlot(slot, remainder.getStackSize() <= 0 ? null : remainder);
        }
        return result;
    }
    @Override public int fill(FluidStack input, boolean execute) {
        if (input == null || input.amount <= 0) return 0;
        FluidStack left = input.copy();
        for (int slot = 0; slot < getSlots() && left.amount > 0; slot++) left.amount -= fill(slot, left, execute);
        return input.amount - left.amount;
    }
    @Override public FluidStack drain(FluidStack request, boolean execute) {
        if (request == null || request.amount <= 0) return null;
        FluidStack result = request.copy(); result.amount = 0;
        FluidStack left = request.copy();
        for (int slot = 0; slot < getSlots() && left.amount > 0; slot++) {
            FluidStack drained = drain(slot, left, execute);
            if (drained != null) { result.amount += drained.amount; left.amount -= drained.amount; }
        }
        return result.amount > 0 ? result : null;
    }
    @Override public FluidStack drain(int amount, boolean execute) {
        if (amount <= 0) return null;
        for (int slot = 0; slot < getSlots(); slot++) {
            IAEFluidStack stored = getFluidInSlot(slot);
            if (stored != null && stored.getStackSize() > 0) {
                FluidStack request = stored.getFluidStack().copy(); request.amount = amount;
                return drain(request, execute);
            }
        }
        return null;
    }
}
