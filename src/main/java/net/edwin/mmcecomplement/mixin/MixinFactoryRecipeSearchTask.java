package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.common.concurrent.FactoryRecipeSearchTask;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.ModuleRecipeConditions;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds attachment gating to MMCE's native factory candidate check. */
@Mixin(value = FactoryRecipeSearchTask.class, remap = false)
public abstract class MixinFactoryRecipeSearchTask {

    @Shadow
    private TileFactoryController factory;

    @Inject(method = "canCheck", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModuleCandidate(
        MachineRecipe recipe, CallbackInfoReturnable<Boolean> cir) {
        if (!(recipe instanceof ModuleRecipeData)
            || !(factory instanceof AttachmentController)) {
            return;
        }
        ModuleRecipeData moduleRecipe = (ModuleRecipeData) recipe;
        if (ModuleRecipeConditions.hasRestrictions(moduleRecipe)
            && ModuleRecipeConditions.evaluate(moduleRecipe,
                ((AttachmentController) factory)
                    .mmceComplement$getActiveAttachmentModules())
                != ModuleRecipeConditions.Failure.NONE) {
            cir.setReturnValue(false);
        }
    }
}
