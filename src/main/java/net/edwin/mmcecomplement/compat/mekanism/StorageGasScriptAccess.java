package net.edwin.mmcecomplement.compat.mekanism;

import crafttweaker.api.item.IIngredient;
import mekanism.api.gas.GasStack;
import mekanism.common.integration.crafttweaker.gas.CraftTweakerGasStack;

/** Loaded only for a real Mekanism gas handler. */
public final class StorageGasScriptAccess {
    private StorageGasScriptAccess() { }

    public static IIngredient stack(Object handler, int slot) {
        synchronized (handler) {
            GasStack stack = ((StorageGasHandler) handler).getStack(slot);
            return stack == null ? null : new CraftTweakerGasStack(stack.copy());
        }
    }

    public static long count(Object handler, IIngredient ingredient) {
        if (ingredient == null || !(ingredient.getInternal() instanceof GasStack)) return 0;
        GasStack request = (GasStack) ingredient.getInternal();
        synchronized (handler) {
            StorageGasHandler gas = (StorageGasHandler) handler;
            long amount = 0;
            for (int i = 0; i < gas.getSlots(); i++) {
                GasStack stored = gas.getStack(i);
                if (stored != null && stored.isGasEqual(request)) amount += stored.amount;
            }
            return amount;
        }
    }

    public static int transfer(Object handler, IIngredient ingredient, int slot, boolean insert, boolean simulate) {
        if (ingredient == null || !(ingredient.getInternal() instanceof GasStack)) return 0;
        GasStack request = ((GasStack) ingredient.getInternal()).copy();
        if (request.amount <= 0) return 0;
        StorageGasHandler gas = (StorageGasHandler) handler;
        if (slot < -1 || slot >= gas.getSlots()) return 0;
        if (insert) return slot == -1 ? gas.receiveGas(null, request, !simulate)
            : gas.receiveGasInSlot(slot, request, !simulate);
        GasStack drawn = slot == -1 ? gas.drawGas(request, !simulate)
            : gas.drawGasInSlot(slot, request, !simulate);
        return drawn == null ? 0 : drawn.amount;
    }
}
