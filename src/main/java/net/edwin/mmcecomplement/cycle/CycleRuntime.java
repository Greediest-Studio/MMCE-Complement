package net.edwin.mmcecomplement.cycle;

import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Per-recipe return quotas for self-cycle assemblies. */
public final class CycleRuntime {
    private static final Map<RecipeCraftingContext,
        IdentityHashMap<TileSelfCycleAssemblyHatch, State>> STATES =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<RecipeCraftingContext> CURRENT =
        new ThreadLocal<>();

    private CycleRuntime() { }

    public static void begin(RecipeCraftingContext context) {
        STATES.put(context, new IdentityHashMap<>());
    }

    public static void clear(RecipeCraftingContext context) {
        STATES.remove(context);
        if (CURRENT.get() == context) CURRENT.remove();
    }

    public static void enter(RecipeCraftingContext context) {
        CURRENT.set(context);
    }

    public static void exit() {
        CURRENT.remove();
    }

    @Nullable
    public static RecipeCraftingContext currentContext() {
        return CURRENT.get();
    }

    public static void recordItem(TileSelfCycleAssemblyHatch tile, int slot,
                                  ItemStack extracted) {
        RecipeCraftingContext context = CURRENT.get();
        if (context == null || extracted.isEmpty()) return;
        State state = state(context, tile, true);
        ItemQuota quota = state.items.get(slot);
        if (quota == null || !sameItem(quota.stack, extracted)) {
            state.items.put(slot, new ItemQuota(extracted.copy(),
                extracted.getCount()));
        } else {
            quota.remaining += extracted.getCount();
        }
    }

    public static int itemAllowance(TileSelfCycleAssemblyHatch tile, int slot,
                                    ItemStack offered) {
        RecipeCraftingContext context = CURRENT.get();
        State state = context == null ? null : state(context, tile, false);
        ItemQuota quota = state == null ? null : state.items.get(slot);
        return quota != null && sameItem(quota.stack, offered)
            ? quota.remaining : 0;
    }

    public static int itemAllowance(TileSelfCycleAssemblyHatch tile, int slot) {
        RecipeCraftingContext context = CURRENT.get();
        State state = context == null ? null : state(context, tile, false);
        ItemQuota quota = state == null ? null : state.items.get(slot);
        return quota == null ? 0 : quota.remaining;
    }

    public static void consumeItemAllowance(TileSelfCycleAssemblyHatch tile,
                                            int slot, int amount) {
        RecipeCraftingContext context = CURRENT.get();
        State state = context == null ? null : state(context, tile, false);
        ItemQuota quota = state == null ? null : state.items.get(slot);
        if (quota != null) quota.remaining = Math.max(0,
            quota.remaining - Math.max(0, amount));
    }

    public static void recordFluid(TileSelfCycleAssemblyHatch tile, int slot,
                                   FluidStack extracted) {
        RecipeCraftingContext context = CURRENT.get();
        if (context == null || extracted == null || extracted.amount <= 0) return;
        State state = state(context, tile, true);
        FluidQuota quota = state.fluids.get(slot);
        if (quota == null || !sameFluid(quota.stack, extracted)) {
            state.fluids.put(slot, new FluidQuota(extracted.copy(),
                extracted.amount));
        } else {
            quota.remaining += extracted.amount;
        }
    }

    public static int fluidAllowance(TileSelfCycleAssemblyHatch tile, int slot,
                                     FluidStack offered) {
        RecipeCraftingContext context = CURRENT.get();
        State state = context == null ? null : state(context, tile, false);
        FluidQuota quota = state == null ? null : state.fluids.get(slot);
        return quota != null && sameFluid(quota.stack, offered)
            ? quota.remaining : 0;
    }

    public static void consumeFluidAllowance(TileSelfCycleAssemblyHatch tile,
                                             int slot, int amount) {
        RecipeCraftingContext context = CURRENT.get();
        State state = context == null ? null : state(context, tile, false);
        FluidQuota quota = state == null ? null : state.fluids.get(slot);
        if (quota != null) quota.remaining = Math.max(0,
            quota.remaining - Math.max(0, amount));
    }

    @Nullable
    private static State state(RecipeCraftingContext context,
                               TileSelfCycleAssemblyHatch tile,
                               boolean create) {
        IdentityHashMap<TileSelfCycleAssemblyHatch, State> byTile =
            STATES.get(context);
        if (byTile == null && create) {
            byTile = new IdentityHashMap<>();
            STATES.put(context, byTile);
        }
        if (byTile == null) return null;
        State state = byTile.get(tile);
        if (state == null && create) {
            state = new State();
            byTile.put(tile, state);
        }
        return state;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty()
            && ItemStack.areItemsEqual(a, b)
            && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static boolean sameFluid(FluidStack a, FluidStack b) {
        return a != null && b != null && a.isFluidEqual(b)
            && java.util.Objects.equals(a.tag, b.tag);
    }

    private static final class State {
        private final Map<Integer, ItemQuota> items = new java.util.HashMap<>();
        private final Map<Integer, FluidQuota> fluids = new java.util.HashMap<>();
    }

    private static final class ItemQuota {
        private final ItemStack stack;
        private int remaining;
        private ItemQuota(ItemStack stack, int remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }

    private static final class FluidQuota {
        private final FluidStack stack;
        private int remaining;
        private FluidQuota(FluidStack stack, int remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }
}
