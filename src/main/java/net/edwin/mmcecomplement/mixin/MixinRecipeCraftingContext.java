package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.batch.BatchController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Allows a Batch Hatch to batch recipes that did not opt into normal parallelism. */
@Mixin(value = RecipeCraftingContext.class, remap = false)
public abstract class MixinRecipeCraftingContext {

    @Shadow
    private TileMultiblockMachineController controller;

    @Redirect(
        method = "canStartCrafting()Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext$CraftingCheckResult;",
        at = @At(value = "INVOKE",
            target = "Lhellfirepvp/modularmachinery/common/crafting/MachineRecipe;isParallelized()Z"))
    private boolean mmceComplement$enableBatchParallelism(MachineRecipe recipe) {
        return recipe.isParallelized()
            || controller instanceof BatchController
            && ((BatchController) controller).mmceComplement$getMaxBatchTime() > 0;
    }
}
