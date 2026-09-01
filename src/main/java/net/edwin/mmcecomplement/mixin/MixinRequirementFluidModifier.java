package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import github.kasuminova.mmce.common.helper.IMachineController;
import net.edwin.mmcecomplement.fluid.AdvancedFluidModifier;
import net.edwin.mmcecomplement.fluid.FluidModifierRequirement;
import net.edwin.mmcecomplement.fluid.FluidOutputModifiers;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Adds controller-aware output transformation to MMCE fluid requirements. */
@Mixin(value = RequirementFluid.class, remap = false)
public abstract class MixinRequirementFluidModifier
    implements FluidModifierRequirement {

    @Shadow @Final public FluidStack required;

    @Unique
    private final List<AdvancedFluidModifier> mmceComplement$fluidModifiers =
        new ArrayList<>();

    @Override
    public void mmceComplement$addFluidModifier(
        AdvancedFluidModifier modifier) {
        if (modifier != null) {
            mmceComplement$fluidModifiers.add(modifier);
        }
    }

    @Override
    public List<AdvancedFluidModifier> mmceComplement$getFluidModifiers() {
        return mmceComplement$fluidModifiers;
    }

    @Inject(method = "deepCopyModified(Ljava/util/List;)"
        + "Lhellfirepvp/modularmachinery/common/crafting/requirement/"
        + "RequirementFluid;", at = @At("RETURN"))
    private void mmceComplement$copyFluidModifiers(
        List<RecipeModifier> modifiers,
        CallbackInfoReturnable<RequirementFluid> cir) {
        FluidModifierRequirement copy =
            (FluidModifierRequirement) (Object) cir.getReturnValue();
        copy.mmceComplement$getFluidModifiers()
            .addAll(mmceComplement$fluidModifiers);
    }

    /**
     * MMCE's native implementation copies the recipe fluid after calculating
     * its amount. Intercept the operation only when a scripted modifier is
     * present so both the simulated capacity check and the real insertion see
     * the transformed fluid and amount.
     */
    @Inject(method = "doFluidIOInternal", at = @At("HEAD"),
        cancellable = true)
    private void mmceComplement$applyFluidModifiers(
        List<ProcessingComponent<?>> components,
        RecipeCraftingContext context,
        int maxParallelism,
        CallbackInfoReturnable<Integer> cir) {
        RequirementFluid requirement = (RequirementFluid) (Object) this;
        if (requirement.getActionType() != IOType.OUTPUT
            || mmceComplement$fluidModifiers.isEmpty()) {
            return;
        }

        FluidStack output = FluidOutputModifiers.apply(
            mmceComplement$fluidModifiers,
            (IMachineController) context.getMachineController(),
            required);
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
        List<IFluidHandler> handlers =
            HybridFluidUtils.castFluidHandlerComponents(components);
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
