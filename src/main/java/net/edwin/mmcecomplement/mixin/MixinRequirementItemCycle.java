package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.edwin.mmcecomplement.cycle.CycleRuntime;
import net.edwin.mmcecomplement.filter.FilteredOutputPriority;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Exposes the active item IO context to the self-cycle handlers. */
@Mixin(value = RequirementItem.class, remap = false)
public abstract class MixinRequirementItemCycle {
    @ModifyVariable(method = {
        "startCrafting", "finishCrafting", "canStartCrafting",
        "getMaxParallelism"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<ProcessingComponent<?>> mmceComplement$prioritizeFilteredItemOutput(
        List<ProcessingComponent<?>> components) {
        return FilteredOutputPriority.prioritize(components);
    }

    @Inject(method = "copyComponents", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$copyFilteredItemHandlers(
        List<ProcessingComponent<?>> components,
        CallbackInfoReturnable<List<ProcessingComponent<?>>> cir) {
        if (FilteredOutputPriority.containsFiltered(components)) {
            cir.setReturnValue(FilteredOutputPriority.copyItems(components));
        }
    }

    @Inject(method = "startCrafting", at = @At("HEAD"))
    private void mmceComplement$enterCycleItemInput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.enter(context);
    }

    @Inject(method = "startCrafting", at = @At("RETURN"))
    private void mmceComplement$exitCycleItemInput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.exit();
    }

    @Inject(method = "finishCrafting", at = @At("HEAD"))
    private void mmceComplement$enterCycleItemOutput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.enter(context);
    }

    @Inject(method = "finishCrafting", at = @At("RETURN"))
    private void mmceComplement$exitCycleItemOutput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.exit();
    }
}
