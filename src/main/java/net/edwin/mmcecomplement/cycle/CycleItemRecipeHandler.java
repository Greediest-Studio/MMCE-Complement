package net.edwin.mmcecomplement.cycle;

import net.minecraft.item.ItemStack;

/** Exact item insertion path used by MMCE's bulk item-output helper. */
public interface CycleItemRecipeHandler extends CycleComponentHandler {
    int mmceComplement$insertCycleOutput(ItemStack stack, int amount);
}
