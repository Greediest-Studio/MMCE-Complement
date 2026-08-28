package net.edwin.mmcecomplement.mechannel;

import net.edwin.mmcecomplement.compat.CompatMods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;

import javax.annotation.Nullable;

/** Component marker for the AE network connection sharing hatch. */
public class ComponentTypeMEConnectionShare extends ComponentType {
    @Nullable
    @Override
    public String requiresModid() {
        return CompatMods.MODID_AE2;
    }
}
