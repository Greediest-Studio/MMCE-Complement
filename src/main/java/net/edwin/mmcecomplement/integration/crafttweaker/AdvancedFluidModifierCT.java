package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.liquid.ILiquidStack;
import github.kasuminova.mmce.common.helper.IMachineController;
import stanhebben.zenscript.annotations.ZenClass;

/** CraftTweaker function used by RecipePrimer#addFluidModifier. */
@ZenRegister
@ZenClass("mods.modularmachinery.AdvancedFluidModifier")
@FunctionalInterface
public interface AdvancedFluidModifierCT {
    ILiquidStack apply(IMachineController controller, ILiquidStack stack);
}
