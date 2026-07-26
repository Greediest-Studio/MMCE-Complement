package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.batch.BatchController;
import net.edwin.mmcecomplement.batch.BatchRecipeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the recipe-specific batch factor before MMCE rounds duration to ticks. */
@Mixin(value = RecipeModifier.class, remap = false)
public abstract class MixinRecipeModifier {

    @Inject(method = "applyModifiers(Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;Lhellfirepvp/modularmachinery/common/crafting/requirement/type/RequirementType;Lhellfirepvp/modularmachinery/common/machine/IOType;FZ)F",
        at = @At("RETURN"), cancellable = true)
    private static void mmceComplement$applyBatchDuration(
        RecipeCraftingContext context,
        RequirementType<?, ?> type,
        IOType ioType,
        float value,
        boolean chance,
        CallbackInfoReturnable<Float> cir) {
        if (context == null || type != RequirementTypesMM.REQUIREMENT_DURATION || chance) {
            return;
        }
        ActiveMachineRecipe activeRecipe = context.getActiveRecipe();
        TileMultiblockMachineController controller = context.getMachineController();
        if (!(activeRecipe instanceof BatchRecipeData)
            || !(controller instanceof BatchController)) {
            return;
        }

        float theoreticalDuration = cir.getReturnValue();
        int maxBatchTime = ((BatchController) controller).mmceComplement$getMaxBatchTime();
        int factor = ((BatchRecipeData) activeRecipe)
            .mmceComplement$getOrCalculateBatchFactor(theoreticalDuration, maxBatchTime);
        if (factor <= 1) {
            return;
        }
        double batched = (double) theoreticalDuration * factor;
        cir.setReturnValue(batched >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) batched);
    }
}
