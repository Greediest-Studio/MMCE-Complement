package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementCatalyst;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.edwin.mmcecomplement.catalyst.CatalystLimitAware;
import net.edwin.mmcecomplement.catalyst.CatalystRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Applies RecipePrimer#setMaxCatalyst to MMCE's native item catalyst. */
@Mixin(value = RequirementCatalyst.class, remap = false)
public abstract class MixinRequirementCatalystLimit implements CatalystLimitAware {
    @Shadow protected boolean isRequired;
    @Unique private int mmceComplement$maxCatalyst = Integer.MAX_VALUE;
    @Unique private boolean mmceComplement$attemptReserved;

    @Override public void setMaxCatalyst(int max) { mmceComplement$maxCatalyst = Math.max(0, max); }
    @Override public int getMaxCatalyst() { return mmceComplement$maxCatalyst; }

    @Inject(method = "canStartCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;)Lhellfirepvp/modularmachinery/common/crafting/helper/CraftCheck;",
        at = @At("HEAD"), cancellable = true)
    private void mmceComplement$limitCheck(List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, CallbackInfoReturnable<CraftCheck> cir) {
        if (!isRequired) {
            if (!CatalystRuntime.tryAcquire(context, mmceComplement$maxCatalyst)) {
                cir.setReturnValue(CraftCheck.skipComponent());
                return;
            }
            mmceComplement$attemptReserved = true;
        }
    }

    @Inject(method = "canStartCrafting(Ljava/util/List;Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;)Lhellfirepvp/modularmachinery/common/crafting/helper/CraftCheck;",
        at = @At("RETURN"))
    private void mmceComplement$releaseFailedCheck(List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, CallbackInfoReturnable<CraftCheck> cir) {
        if (mmceComplement$attemptReserved && !isRequired) CatalystRuntime.release(context);
        mmceComplement$attemptReserved = false;
    }

    @Inject(method = "getMaxParallelism", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$limitParallel(List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, int max, CallbackInfoReturnable<Integer> cir) {
        if (!isRequired) {
            if (!CatalystRuntime.tryAcquire(context, mmceComplement$maxCatalyst)) {
                cir.setReturnValue(max);
                return;
            }
            mmceComplement$attemptReserved = true;
        }
    }

    @Inject(method = "getMaxParallelism", at = @At("RETURN"))
    private void mmceComplement$releaseFailedParallel(List<ProcessingComponent<?>> components,
        RecipeCraftingContext context, int max, CallbackInfoReturnable<Integer> cir) {
        if (mmceComplement$attemptReserved && !isRequired) CatalystRuntime.release(context);
        mmceComplement$attemptReserved = false;
    }

    @Inject(method = "deepCopyModified", at = @At("RETURN"))
    private void mmceComplement$copyLimit(List<RecipeModifier> modifiers,
        CallbackInfoReturnable<RequirementCatalyst> cir) {
        ((CatalystLimitAware) cir.getReturnValue()).setMaxCatalyst(mmceComplement$maxCatalyst);
    }
}
