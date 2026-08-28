package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import net.edwin.mmcecomplement.cycle.CycleRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Exposes Mekanism gas recipe IO without linking common code to its API. */
@Pseudo
@Mixin(targets =
    "hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas",
    remap = false)
public abstract class MixinRequirementGasCycle {
    @Inject(method = "startCrafting", at = @At("HEAD"))
    private void mmceComplement$enterCycleGasInput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.enter(context);
    }

    @Inject(method = "startCrafting", at = @At("RETURN"))
    private void mmceComplement$exitCycleGasInput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.exit();
    }

    @Inject(method = "finishCrafting", at = @At("HEAD"))
    private void mmceComplement$enterCycleGasOutput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.enter(context);
    }

    @Inject(method = "finishCrafting", at = @At("RETURN"))
    private void mmceComplement$exitCycleGasOutput(
        List<ProcessingComponent<?>> components, RecipeCraftingContext context,
        ResultChance chance, CallbackInfo ci) {
        CycleRuntime.exit();
    }
}
