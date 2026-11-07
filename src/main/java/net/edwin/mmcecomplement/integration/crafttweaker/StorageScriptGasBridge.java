package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.api.item.IIngredient;
import net.edwin.mmcecomplement.variant.StorageController;

/** No Mekanism types in the always-loaded CraftTweaker API. */
final class StorageScriptGasBridge {
    private StorageScriptGasBridge() { }

    static Object handler(StorageController storage) {
        return storage != null && storage.mmceComplement$hasGasStorage()
            ? storage.mmceComplement$getGasStorage() : null;
    }

    static Object call(Object handler, String method, Class<?>[] types, Object... arguments) {
        if (handler == null) return null;
        try {
            return Class.forName("net.edwin.mmcecomplement.compat.mekanism.StorageGasScriptAccess")
                .getMethod(method, types).invoke(null, arguments);
        } catch (ReflectiveOperationException e) {
            // A real transfer may already have happened: never disguise failure as zero.
            throw new IllegalStateException("CraftTweaker gas storage operation failed", e);
        }
    }

    static IIngredient stack(Object handler, int slot) {
        return (IIngredient) call(handler, "stack", new Class<?>[]{Object.class, int.class}, handler, slot);
    }

    static long count(Object handler, IIngredient request) {
        Object result = call(handler, "count", new Class<?>[]{Object.class, IIngredient.class}, handler, request);
        return result == null ? 0 : (Long) result;
    }

    static int transfer(Object handler, IIngredient request, int slot, boolean insert, boolean simulate) {
        Object result = call(handler, "transfer",
            new Class<?>[]{Object.class, IIngredient.class, int.class, boolean.class, boolean.class},
            handler, request, slot, insert, simulate);
        return result == null ? 0 : (Integer) result;
    }
}
