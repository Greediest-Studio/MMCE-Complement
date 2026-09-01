package net.edwin.mmcecomplement.fluid;

import java.util.List;

/** Mixin interface carried by MMCE fluid requirements. */
public interface FluidModifierRequirement {
    void mmceComplement$addFluidModifier(AdvancedFluidModifier modifier);

    List<AdvancedFluidModifier> mmceComplement$getFluidModifiers();
}
