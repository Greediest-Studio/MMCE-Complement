package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(value = RecipePrimer.class, remap = false)
public abstract class MixinRecipePrimer implements ModuleRecipeData {

    @Unique
    private final Set<String> mmceComplement$requiredModules = new LinkedHashSet<>();

    @Unique
    private final Set<String> mmceComplement$forbiddenModules = new LinkedHashSet<>();

    @Override
    public Set<String> mmceComplement$getRequiredModules() {
        return mmceComplement$requiredModules;
    }

    @Override
    public Set<String> mmceComplement$getForbiddenModules() {
        return mmceComplement$forbiddenModules;
    }
}
