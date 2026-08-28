package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.RecipeRegistry;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRecipeThread;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies MMCE's search task with a module-state-filtered registry snapshot. */
@Mixin(value = MachineRecipeThread.class, remap = false)
public abstract class MixinMachineRecipeThread {

    @Shadow
    private TileMachineController controller;

    @Redirect(method = "createRecipeSearchTask", at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/crafting/RecipeRegistry;getRecipesFor(Lhellfirepvp/modularmachinery/common/machine/DynamicMachine;)Ljava/lang/Iterable;"))
    private Iterable<MachineRecipe> mmceComplement$getModuleFilteredRecipes(
        DynamicMachine machine) {
        if (controller instanceof AttachmentController) {
            Iterable<MachineRecipe> candidates = ((AttachmentController) controller)
                .mmceComplement$getModuleRecipeCandidates();
            if (candidates != null) {
                return candidates;
            }
        }
        return RecipeRegistry.getRecipesFor(machine);
    }
}
