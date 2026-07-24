package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.PreparedRecipe;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Mixin(value = MachineRecipe.class, remap = false)
public abstract class MixinMachineRecipe implements ModuleRecipeData {

    @Unique
    private final Set<String> mmceComplement$requiredModules = new LinkedHashSet<>();

    @Unique
    private final Set<String> mmceComplement$forbiddenModules = new LinkedHashSet<>();

    @Inject(method = "<init>(Lhellfirepvp/modularmachinery/common/crafting/PreparedRecipe;)V",
        at = @At("RETURN"))
    private void mmceComplement$copyModuleConditions(PreparedRecipe prepared, CallbackInfo ci) {
        if (prepared instanceof ModuleRecipeData) {
            mmceComplement$copyFrom((ModuleRecipeData) prepared);
        }
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void mmceComplement$copyModuleConditionsToClone(
        Function<ResourceLocation, ResourceLocation> registryNameOperator,
        ResourceLocation owningMachine,
        List<RecipeModifier> modifiers,
        CallbackInfoReturnable<MachineRecipe> cir) {
        MachineRecipe copy = cir.getReturnValue();
        if (copy instanceof ModuleRecipeData) {
            ModuleRecipeData target = (ModuleRecipeData) (Object) copy;
            target.mmceComplement$getRequiredModules().addAll(mmceComplement$requiredModules);
            target.mmceComplement$getForbiddenModules().addAll(mmceComplement$forbiddenModules);
        }
    }

    @Unique
    private void mmceComplement$copyFrom(ModuleRecipeData source) {
        mmceComplement$requiredModules.addAll(source.mmceComplement$getRequiredModules());
        mmceComplement$forbiddenModules.addAll(source.mmceComplement$getForbiddenModules());
    }

    @Override
    public Set<String> mmceComplement$getRequiredModules() {
        return mmceComplement$requiredModules;
    }

    @Override
    public Set<String> mmceComplement$getForbiddenModules() {
        return mmceComplement$forbiddenModules;
    }
}
