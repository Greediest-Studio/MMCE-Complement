package net.edwin.mmcecomplement.mechannel;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;

/** Central cleanup hook for every MMCE context termination path. */
public final class MEChannelReservationLifecycle {
    private MEChannelReservationLifecycle() { }

    public static void release(RecipeCraftingContext context) {
        if (context == null) {
            return;
        }
        for (ComponentRequirement<?, ?> requirement
            : context.getRequirementBy(ModMEChannelTypes.REQUIREMENT)) {
            if (requirement instanceof RequirementMEChannel) {
                ((RequirementMEChannel) requirement).releaseReservation();
            }
        }
    }
}
