package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import net.edwin.mmcecomplement.attachment.ModuleRecipeConditions;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import net.edwin.mmcecomplement.mechannel.RequirementMEChannel;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

/** CraftTweaker additions for restricting a recipe by active attachment modules. */
@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public final class RecipePrimerExpansion {

    private RecipePrimerExpansion() {
    }

    /** Adds a positive, whole-number ME channel input to the recipe. */
    @ZenMethod
    public static RecipePrimer addMEChannelInput(RecipePrimer primer,
                                                  int amount) {
        primer.appendComponent(new RequirementMEChannel(amount));
        return primer;
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
