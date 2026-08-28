package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.ModuleRecipeConditions;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies attachment recipe restrictions at the two recipe lifecycle edges.
 * The recipe search task deliberately knows nothing about module state, and
 * no module state is read while a recipe is doing ordinary work ticks.
 */
@Mixin(value = RecipeThread.class, remap = false)
public abstract class MixinRecipeThread {

    @Shadow
    protected ActiveMachineRecipe activeRecipe;

    @Shadow
    protected CraftingStatus status;

    @Shadow
    protected boolean waitForFinish;

    @Shadow
    protected hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController ctrl;

    @Unique
    private ActiveMachineRecipe mmceComplement$checkedRecipe;

    @Unique
    private boolean mmceComplement$startChecked;

    @Unique
    private boolean mmceComplement$completionChecked;

    @Unique
    private ModuleRecipeConditions.Failure mmceComplement$startFailure =
        ModuleRecipeConditions.Failure.NONE;

    @Unique
    private ModuleRecipeConditions.Failure mmceComplement$completionFailure =
        ModuleRecipeConditions.Failure.NONE;

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesAtRecipeStart(
        CallbackInfoReturnable<CraftingStatus> cir) {
        ActiveMachineRecipe current = activeRecipe;
        if (current == null) {
            mmceComplement$clearChecks();
            return;
        }
        if (current != mmceComplement$checkedRecipe) {
            mmceComplement$checkedRecipe = current;
            mmceComplement$startChecked = false;
            mmceComplement$completionChecked = false;
            mmceComplement$startFailure = ModuleRecipeConditions.Failure.NONE;
            mmceComplement$completionFailure = ModuleRecipeConditions.Failure.NONE;
        }
        if (!mmceComplement$startChecked) {
            mmceComplement$startChecked = true;
            mmceComplement$startFailure = mmceComplement$evaluate(current);
        }
        if (mmceComplement$startFailure != ModuleRecipeConditions.Failure.NONE) {
            cir.setReturnValue(CraftingStatus.failure(
                mmceComplement$startFailure.getMessage()));
            return;
        }
        // A completion-boundary module failure aborts this completed recipe.
        // Keep returning the cached result until MMCE's normal failure path
        // discards the active recipe; importantly, do not re-read modules.
        if (current.isCompleted()
            && mmceComplement$completionChecked
            && mmceComplement$completionFailure
                != ModuleRecipeConditions.Failure.NONE) {
            cir.setReturnValue(CraftingStatus.failure(
                mmceComplement$completionFailure.getMessage()));
        }
    }

    @Inject(method = "onFinished", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesAtRecipeFinish(CallbackInfo ci) {
        ActiveMachineRecipe current = activeRecipe;
        if (current == null) {
            return;
        }
        if (!mmceComplement$completionChecked) {
            mmceComplement$completionChecked = true;
            mmceComplement$completionFailure = mmceComplement$evaluate(current);
        }
        if (mmceComplement$completionFailure != ModuleRecipeConditions.Failure.NONE) {
            // This is a module-gating failure, not an output-backpressure
            // condition. Let MMCE's normal failure path reset the recipe so a
            // later structure refresh can select it again.
            waitForFinish = false;
            status = CraftingStatus.failure(
                mmceComplement$completionFailure.getMessage());
            ci.cancel();
        }
    }

    @Inject(method = "onFinished", at = @At("RETURN"))
    private void mmceComplement$resetChecksAfterSuccessfulFinish(CallbackInfo ci) {
        if (!waitForFinish) {
            mmceComplement$startChecked = false;
            mmceComplement$completionChecked = false;
            mmceComplement$startFailure = ModuleRecipeConditions.Failure.NONE;
            mmceComplement$completionFailure = ModuleRecipeConditions.Failure.NONE;
        }
    }

    @Unique
    private ModuleRecipeConditions.Failure mmceComplement$evaluate(
        ActiveMachineRecipe current) {
        MachineRecipe recipe = current.getRecipe();
        if (!(recipe instanceof ModuleRecipeData)) {
            return ModuleRecipeConditions.Failure.NONE;
        }
        if (!(ctrl instanceof AttachmentController)) {
            return ModuleRecipeConditions.Failure.MISSING_REQUIRED;
        }
        return ((AttachmentController) ctrl).mmceComplement$getModuleRecipeFailure(
            (ModuleRecipeData) recipe);
    }

    @Unique
    private void mmceComplement$clearChecks() {
        mmceComplement$checkedRecipe = null;
        mmceComplement$startChecked = false;
        mmceComplement$completionChecked = false;
        mmceComplement$startFailure = ModuleRecipeConditions.Failure.NONE;
        mmceComplement$completionFailure = ModuleRecipeConditions.Failure.NONE;
    }
}
