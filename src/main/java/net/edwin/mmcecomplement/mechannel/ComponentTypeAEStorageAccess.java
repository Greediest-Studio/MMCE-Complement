package net.edwin.mmcecomplement.mechannel;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import net.edwin.mmcecomplement.compat.CompatMods;

/** Structure component marker; deliberately has no recipe requirement. */
public final class ComponentTypeAEStorageAccess extends ComponentType {
    @Override public String requiresModid() { return CompatMods.getLoadedAe2ModId(); }
}
