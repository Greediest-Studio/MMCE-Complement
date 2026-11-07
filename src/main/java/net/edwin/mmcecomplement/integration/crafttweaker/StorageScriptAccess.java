package net.edwin.mmcecomplement.integration.crafttweaker;

import github.kasuminova.mmce.common.helper.IMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;

/** Native operations on the same authoritative handlers used by hatches and ME. */
final class StorageScriptAccess {
    private StorageScriptAccess() { }

    static StorageController storage(IMachineController controller) {
        Object tile = controller.getController();
        return tile instanceof StorageController
            && ((StorageController) tile).mmceComplement$isStorageController()
            ? (StorageController) tile : null;
    }

    static StorageController available(IMachineController controller) {
        TileMultiblockMachineController tile = controller.getController();
        net.minecraft.tileentity.TileEntity entity = tile;
        // CraftTweaker may also call these from a client or an async event.
        // World mutation and handler initialization belong on the server thread.
        return tile != null && !entity.isInvalid() && tile.isStructureFormed()
            && entity.getWorld() != null && !entity.getWorld().isRemote
            && entity.getWorld().getMinecraftServer() != null
            && entity.getWorld().getMinecraftServer().isCallingFromMinecraftThread()
            ? storage(controller) : null;
    }

    static ItemStackHandler items(StorageController storage) {
        return storage == null ? null : storage.mmceComplement$getStorageInventory();
    }

    static IFluidHandler fluids(StorageController storage, int slot) {
        if (storage == null || !storage.mmceComplement$hasFluidStorage()) return null;
        if (slot == -1) return storage.mmceComplement$getFluidStorage();
        return slot >= 0 && slot < storage.mmceComplement$getFluidStorageSlots()
            ? storage.mmceComplement$getFluidTank(slot) : null;
    }

    static boolean matches(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty()
            && first.isItemEqual(second) && ItemStack.areItemStackTagsEqual(first, second);
    }

    static ItemStack itemAt(ItemStackHandler handler, int slot) {
        if (handler == null || slot < 0 || slot >= handler.getSlots()) return ItemStack.EMPTY;
        synchronized (handler) { return handler.getStackInSlot(slot).copy(); }
    }

    static long countItem(ItemStackHandler handler, ItemStack request) {
        if (handler == null || request.isEmpty()) return 0;
        synchronized (handler) {
            long count = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stored = handler.getStackInSlot(i);
                if (matches(stored, request)) count += stored.getCount();
            }
            return count;
        }
    }

    /** slot -1 visits all slots. Invalid indices never fall back to all slots. */
    static int transferItem(ItemStackHandler handler, ItemStack request, int slot,
                            boolean insert, boolean simulate) {
        if (handler == null || request.isEmpty() || request.getCount() <= 0
            || slot < -1 || slot >= handler.getSlots()) return 0;
        synchronized (handler) {
            int left = request.getCount();
            int start = slot == -1 ? 0 : slot;
            int end = slot == -1 ? handler.getSlots() : slot + 1;
            // Fill matching stacks before allocating empty ones.
            for (int pass = 0; pass < (insert ? 2 : 1); pass++) {
                for (int i = start; i < end && left > 0; i++) {
                    ItemStack stored = handler.getStackInSlot(i);
                    if (insert) {
                        if (pass == 0 ? !matches(stored, request) : !stored.isEmpty()) continue;
                        ItemStack portion = request.copy();
                        portion.setCount(left);
                        left = handler.insertItem(i, portion, simulate).getCount();
                    } else if (matches(stored, request)) {
                        left -= handler.extractItem(i, left, simulate).getCount();
                    }
                }
            }
            return request.getCount() - left;
        }
    }

    static FluidStack fluidAt(IFluidHandler handler) {
        if (handler == null) return null;
        synchronized (handler) {
            net.minecraftforge.fluids.capability.IFluidTankProperties[] tanks = handler.getTankProperties();
            if (tanks.length == 0) return null;
            FluidStack stack = tanks[0].getContents();
            return stack == null ? null : stack.copy();
        }
    }

    static long countFluid(IFluidHandler handler, FluidStack request) {
        if (handler == null || request == null) return 0;
        synchronized (handler) {
            long amount = 0;
            for (net.minecraftforge.fluids.capability.IFluidTankProperties tank : handler.getTankProperties()) {
                FluidStack stored = tank.getContents();
                if (stored != null && stored.isFluidEqual(request)) amount += stored.amount;
            }
            return amount;
        }
    }

    static int transferFluid(IFluidHandler handler, FluidStack request, boolean insert, boolean simulate) {
        if (handler == null || request == null || request.amount <= 0) return 0;
        if (insert) return handler.fill(request.copy(), !simulate);
        FluidStack drained = handler.drain(request.copy(), !simulate);
        return drained == null ? 0 : drained.amount;
    }
}
