package net.edwin.mmcecomplement.mechannel;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import net.edwin.mmcecomplement.compat.CompatMods;

import javax.annotation.Nullable;

/** MMCE component type supplied by an ME Channel Input Hatch. */
public class ComponentTypeMEChannel extends ComponentType {

    @Nullable
    @Override
    public String requiresModid() {
        return CompatMods.MODID_AE2;
    }
}
