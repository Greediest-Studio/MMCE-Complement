package net.edwin.mmcecomplement.fluid;

import github.kasuminova.mmce.common.helper.IMachineController;
import net.minecraftforge.fluids.FluidStack;

/** Runtime counterpart of the CraftTweaker advanced fluid modifier. */
@FunctionalInterface
public interface AdvancedFluidModifier {
    FluidStack apply(IMachineController controller,
                     FluidStack stack);
}
