package net.edwin.mmcecomplement.gas;

import github.kasuminova.mmce.common.helper.IMachineController;
import mekanism.api.gas.GasStack;

/** Runtime counterpart of the CraftTweaker advanced gas modifier. */
@FunctionalInterface
public interface AdvancedGasModifier {
    GasStack apply(IMachineController controller, GasStack stack);
}
