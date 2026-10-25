package net.edwin.mmcecomplement.variant;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Server-thread transfers. Never remove stock that the destination cannot hold. */
public final class StorageTransfer {
    private StorageTransfer() { }

    /** Moves slots in ascending order and reports input left behind for lack of room. */
    public static boolean items(IItemHandler source, IItemHandler destination) {
        return items(source, destination, () -> { });
    }

    public static boolean items(IItemHandler source, IItemHandler destination, Runnable changed) {
        if (source == destination) return false;
        boolean blocked = false;
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack offered = source.extractItem(slot, Integer.MAX_VALUE, true);
            if (offered.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, offered.copy(), true);
            int accepted = offered.getCount() - remainder.getCount();
            if (accepted > 0) {
                ItemStack extracted = source.extractItem(slot, accepted, false);
                ItemStack rejected = ItemHandlerHelper.insertItemStacked(destination, extracted, false);
                if (extracted.getCount() > rejected.getCount()) changed.run();
                // Handlers can impose a smaller real transfer than their simulation.
                // Restore that remainder instead of discarding it.
                if (!rejected.isEmpty()) {
                    blocked = true;
                    ItemStack unrestored = source.insertItem(slot, rejected, false);
                    if (!unrestored.isEmpty()) {
                        throw new IllegalStateException("Storage transfer source refused its own remainder");
                    }
                }
            }
            blocked |= accepted < offered.getCount();
        }
        return blocked;
    }

    /** Tries every fluid type rather than letting a blocked first tank hide later tanks. */
    public static boolean fluids(IFluidHandler source, IFluidHandler destination) {
        return fluids(source, destination, () -> { });
    }

    public static boolean fluids(IFluidHandler source, IFluidHandler destination, Runnable changed) {
        if (source == destination) return false;
        boolean blocked = false;
        for (IFluidTankProperties properties : source.getTankProperties()) {
            FluidStack contents = properties.getContents();
            if (contents == null || contents.amount <= 0) continue;
            FluidStack offered = source.drain(contents.copy(), false);
            if (offered == null || offered.amount <= 0) continue;
            int accepted = Math.min(offered.amount, destination.fill(offered.copy(), false));
            if (accepted > 0) {
                FluidStack request = offered.copy();
                request.amount = accepted;
                FluidStack extracted = source.drain(request, true);
                if (extracted != null && extracted.amount > 0) {
                    int inserted = destination.fill(extracted.copy(), true);
                    if (inserted > 0) changed.run();
                    if (inserted < extracted.amount) {
                        blocked = true;
                        extracted.amount -= inserted;
                        if (source.fill(extracted, true) != extracted.amount) {
                            throw new IllegalStateException("Storage fluid source refused its own remainder");
                        }
                    }
                }
            }
            blocked |= accepted < offered.amount;
        }
        return blocked;
    }
}
