package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.ModuleRecipeConditions;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Collections;

@Mixin(value = ActiveMachineRecipe.class, remap = false)
public abstract class MixinActiveMachineRecipe {

    @Shadow
    @Final
    private MachineRecipe recipe;

    @Inject(method = "canStartCrafting", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesBeforeStart(
        RecipeCraftingContext context,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        mmceComplement$rejectCheckIfNeeded(context.getMachineController(), cir);
    }

    @Inject(method = "canRestartCrafting", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesBeforeRestart(
        RecipeCraftingContext context,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        mmceComplement$rejectCheckIfNeeded(context.getMachineController(), cir);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesWhileRunning(
        TileMultiblockMachineController controller,
        RecipeCraftingContext context,
        CallbackInfoReturnable<CraftingStatus> cir) {
        ModuleRecipeConditions.Failure failure = mmceComplement$evaluate(controller);
        if (failure != ModuleRecipeConditions.Failure.NONE) {
            cir.setReturnValue(CraftingStatus.failure(failure.getMessage()));
        }
    }

    @Unique
    private void mmceComplement$rejectCheckIfNeeded(
        TileMultiblockMachineController controller,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        ModuleRecipeConditions.Failure failure = mmceComplement$evaluate(controller);
        if (failure == ModuleRecipeConditions.Failure.NONE) {
            return;
        }
        RecipeCraftingContext.CraftingCheckResult result =
            new RecipeCraftingContext.CraftingCheckResult();
        result.addError(failure.getMessage());
        cir.setReturnValue(result);
    }

    @Unique
    private ModuleRecipeConditions.Failure mmceComplement$evaluate(
        TileMultiblockMachineController controller) {
        Collection<String> activeModules = controller instanceof AttachmentController
            ? ((AttachmentController) controller).mmceComplement$getActiveAttachmentModules()
            : Collections.emptySet();
        return ModuleRecipeConditions.evaluate((ModuleRecipeData) (Object) recipe, activeModules);
    }
}
