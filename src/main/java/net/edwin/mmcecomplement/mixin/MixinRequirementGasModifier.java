package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.gas.AdvancedGasModifier;
import net.edwin.mmcecomplement.gas.GasModifierRequirement;
import net.edwin.mmcecomplement.gas.GasOutputModifiers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Adds controller-aware output transformation to MMCE gas requirements. */
@Pseudo
@Mixin(targets =
    "hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas",
    remap = false)
public abstract class MixinRequirementGasModifier
    implements GasModifierRequirement {

    @Shadow @Final public GasStack required;

    @Unique
    private final List<AdvancedGasModifier> mmceComplement$gasModifiers =
        new ArrayList<>();

    @Override
    public void mmceComplement$addGasModifier(AdvancedGasModifier modifier) {
        if (modifier != null) {
            mmceComplement$gasModifiers.add(modifier);
        }
    }

    @Override
    public List<AdvancedGasModifier> mmceComplement$getGasModifiers() {
        return mmceComplement$gasModifiers;
    }

    @Inject(method = "deepCopyModified(Ljava/util/List;)"
        + "Lhellfirepvp/modularmachinery/common/crafting/requirement/"
        + "RequirementGas;", at = @At("RETURN"))
    private void mmceComplement$copyGasModifiers(
        List<RecipeModifier> modifiers,
        CallbackInfoReturnable<RequirementGas> cir) {
        GasModifierRequirement copy =
            (GasModifierRequirement) (Object) cir.getReturnValue();
        copy.mmceComplement$getGasModifiers()
            .addAll(mmceComplement$gasModifiers);
    }

    @Inject(method = "doGasIOInternal", at = @At("HEAD"),
        cancellable = true)
    private void mmceComplement$applyGasModifiers(
        List<ProcessingComponent<?>> components,
        RecipeCraftingContext context,
        int maxParallelism,
        CallbackInfoReturnable<Integer> cir) {
        RequirementGas requirement = (RequirementGas) (Object) this;
        if (requirement.getActionType() != IOType.OUTPUT
            || mmceComplement$gasModifiers.isEmpty()) {
            return;
        }

        GasStack output = GasOutputModifiers.apply(
            mmceComplement$gasModifiers,
            (IMachineController) context.getMachineController(), required);
        if (output == null || output.amount <= 0) {
            cir.setReturnValue(maxParallelism);
            return;
        }

        long singleAmount = Math.round(RecipeModifier.applyModifiers(
            context, requirement, (double) output.amount, false));
        if (singleAmount <= 0L) {
            cir.setReturnValue(maxParallelism);
            return;
        }

        long totalAmount = mmceComplement$saturatingMultiply(
            singleAmount, maxParallelism);
        List<IExtendedGasHandler> handlers =
            HybridFluidUtils.castGasHandlerComponents(components);
        long transferred = HybridFluidUtils.doSimulateDrainOrFill(
            output, handlers, totalAmount, IOType.OUTPUT);
        if (transferred < singleAmount) {
            cir.setReturnValue(0);
            return;
        }

        HybridFluidUtils.doDrainOrFill(
            output, transferred, handlers, IOType.OUTPUT);
        cir.setReturnValue((int) Math.min(Integer.MAX_VALUE,
            transferred / singleAmount));
    }

    @Unique
    private static long mmceComplement$saturatingMultiply(long amount,
                                                           int parallelism) {
        if (parallelism <= 0 || amount <= 0L) {
            return 0L;
        }
        return amount > Long.MAX_VALUE / parallelism
            ? Long.MAX_VALUE
            : amount * parallelism;
    }
}
