package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
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

/** Exposes the active fluid/gas IO context to self-cycle handlers. */
@Mixin(value = RequirementFluid.class, remap = false)
public abstract class MixinRequirementFluidCycle {
    @ModifyVariable(method = {
        "startCrafting", "finishCrafting", "canStartCrafting",
        "getMaxParallelism"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<ProcessingComponent<?>> mmceComplement$prioritizeFilteredFluidOutput(
        List<ProcessingComponent<?>> components) {
        return FilteredOutputPriority.prioritize(components);
    }

    @Inject(method = "copyComponents", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$copyFilteredFluidHandlers(
        List<ProcessingComponent<?>> components,
        CallbackInfoReturnable<List<ProcessingComponent<?>>> cir) {
        if (FilteredOutputPriority.containsFiltered(components)) {
            cir.setReturnValue(FilteredOutputPriority.copyFluids(components));
        }
    }

    @Inject(method = "startCrafting", at = @At("HEAD"))
    private void mmceComplement$enterCycleFluidInput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.enter(context);
    }

    @Inject(method = "startCrafting", at = @At("RETURN"))
    private void mmceComplement$exitCycleFluidInput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.exit();
    }

    @Inject(method = "finishCrafting", at = @At("HEAD"))
    private void mmceComplement$enterCycleFluidOutput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.enter(context);
    }

    @Inject(method = "finishCrafting", at = @At("RETURN"))
    private void mmceComplement$exitCycleFluidOutput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.exit();
    }
}
