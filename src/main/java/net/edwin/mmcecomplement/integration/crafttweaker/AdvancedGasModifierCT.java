package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import github.kasuminova.mmce.common.helper.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

/** CraftTweaker function used by RecipePrimer#addGasModifier. */
@ZenRegister
@ZenClass("mods.modularmachinery.AdvancedGasModifier")
@FunctionalInterface
public interface AdvancedGasModifierCT {
    IIngredient apply(IMachineController controller, IIngredient stack);
}
