package net.edwin.mmcecomplement.catalyst;

import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;

/** Bridges catalyst requirements to the per-recipe crafting context counter. */
public final class CatalystRuntime {
    private CatalystRuntime() { }
    public interface Context {
        boolean tryAcquireCatalyst(int max);
        void releaseCatalyst();
    }
    public static boolean tryAcquire(RecipeCraftingContext context, int max) {
        return max > 0 && (!(context instanceof Context) || ((Context) context).tryAcquireCatalyst(max));
    }
    public static void release(RecipeCraftingContext context) {
        if (context instanceof Context) ((Context) context).releaseCatalyst();
    }
}
