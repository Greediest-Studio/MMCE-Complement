package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import net.edwin.mmcecomplement.attachment.ModuleRecipeConditions;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

/** CraftTweaker additions for restricting a recipe by active attachment modules. */
@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class RecipePrimerExpansion {

    private RecipePrimerExpansion() {
    }

    @ZenMethod
    public static RecipePrimer withModule(RecipePrimer primer, String[] moduleIds) {
        ModuleRecipeConditions.addRequired((ModuleRecipeData) (Object) primer, moduleIds);
        return primer;
    }

    @ZenMethod
    public static RecipePrimer withoutModule(RecipePrimer primer, String[] moduleIds) {
        ModuleRecipeConditions.addForbidden((ModuleRecipeData) (Object) primer, moduleIds);
        return primer;
    }
}
