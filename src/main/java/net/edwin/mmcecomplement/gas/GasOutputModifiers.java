package net.edwin.mmcecomplement.gas;

import github.kasuminova.mmce.common.helper.IMachineController;
import mekanism.api.gas.GasStack;

import javax.annotation.Nullable;
import java.util.List;

/** Applies gas-output modifier chains without mutating the recipe template. */
public final class GasOutputModifiers {
    private GasOutputModifiers() {
    }

    @Nullable
    public static GasStack apply(List<AdvancedGasModifier> modifiers,
                                 IMachineController controller,
                                 GasStack template) {
        GasStack result = template == null ? null : template.copy();
        for (AdvancedGasModifier modifier : modifiers) {
            if (result == null) {
                break;
            }
            result = modifier.apply(controller, result);
        }
        return result;
    }
}
