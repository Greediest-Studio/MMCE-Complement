package net.edwin.mmcecomplement.gas;

import java.util.List;

/** Mixin interface carried by MMCE Mekanism gas requirements. */
public interface GasModifierRequirement {
    void mmceComplement$addGasModifier(AdvancedGasModifier modifier);

    List<AdvancedGasModifier> mmceComplement$getGasModifiers();
}
