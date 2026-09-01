package net.edwin.mmcecomplement.fluid;

import github.kasuminova.mmce.common.helper.IMachineController;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

/** Applies fluid-output modifier chains without mutating the recipe template. */
public final class FluidOutputModifiers {
    private FluidOutputModifiers() {
    }

    @Nullable
    public static FluidStack apply(List<AdvancedFluidModifier> modifiers,
                                   IMachineController controller,
                                   FluidStack template) {
        FluidStack result = template == null ? null : template.copy();
        for (AdvancedFluidModifier modifier : modifiers) {
            if (result == null) {
                break;
            }
            result = modifier.apply(controller, result);
        }
        return result;
    }
}
