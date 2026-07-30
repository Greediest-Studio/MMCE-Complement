package net.edwin.mmcecomplement.compat.mekanism;

import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.cycle.CycleRuntime;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Optional Mekanism-gas quotas associated with the common cycle context. */
final class CycleGasRuntime {
    private static final Map<RecipeCraftingContext,
        IdentityHashMap<TileSelfCycleAssemblyHatch, Map<Integer, Quota>>> STATES =
        Collections.synchronizedMap(new WeakHashMap<>());

    private CycleGasRuntime() { }

    static void record(TileSelfCycleAssemblyHatch tile, int slot,
                       GasStack extracted) {
        RecipeCraftingContext context = CycleRuntime.currentContext();
        if (context == null || extracted == null || extracted.amount <= 0) return;
        IdentityHashMap<TileSelfCycleAssemblyHatch, Map<Integer, Quota>> byTile =
            STATES.computeIfAbsent(context, ignored -> new IdentityHashMap<>());
        Map<Integer, Quota> slots = byTile.computeIfAbsent(tile,
            ignored -> new HashMap<>());
        Quota quota = slots.get(slot);
        if (quota == null || !quota.stack.isGasEqual(extracted)) {
            slots.put(slot, new Quota(extracted.copy(), extracted.amount));
        } else {
            quota.remaining += extracted.amount;
        }
    }

    static int allowance(TileSelfCycleAssemblyHatch tile, int slot,
                         GasStack offered) {
        RecipeCraftingContext context = CycleRuntime.currentContext();
        IdentityHashMap<TileSelfCycleAssemblyHatch, Map<Integer, Quota>> byTile =
            context == null ? null : STATES.get(context);
        Map<Integer, Quota> slots = byTile == null ? null : byTile.get(tile);
        Quota quota = slots == null ? null : slots.get(slot);
        return quota != null && quota.stack.isGasEqual(offered)
            ? quota.remaining : 0;
    }

    static void consume(TileSelfCycleAssemblyHatch tile, int slot, int amount) {
        RecipeCraftingContext context = CycleRuntime.currentContext();
        IdentityHashMap<TileSelfCycleAssemblyHatch, Map<Integer, Quota>> byTile =
            context == null ? null : STATES.get(context);
        Map<Integer, Quota> slots = byTile == null ? null : byTile.get(tile);
        Quota quota = slots == null ? null : slots.get(slot);
        if (quota != null) quota.remaining = Math.max(0,
            quota.remaining - Math.max(0, amount));
    }

    private static final class Quota {
        private final GasStack stack;
        private int remaining;
        private Quota(GasStack stack, int remaining) {
            this.stack = stack;
            this.remaining = remaining;
        }
    }
}
