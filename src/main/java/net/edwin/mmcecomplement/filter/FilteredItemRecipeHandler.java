package net.edwin.mmcecomplement.filter;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Item handler whose recipe insertion must bypass MMCE's direct slot writes. */
public interface FilteredItemRecipeHandler extends IItemHandlerModifiable {
    int insertFilteredOutput(ItemStack stack, int amount, boolean simulate);
    FilteredItemRecipeHandler copyForSimulation();
}
