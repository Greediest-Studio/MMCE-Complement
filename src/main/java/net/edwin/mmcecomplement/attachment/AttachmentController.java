package net.edwin.mmcecomplement.attachment;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;

import java.util.Set;

/** Runtime attachment state exposed by MMCE machine controllers. */
public interface AttachmentController {

    Set<String> mmceComplement$getActiveAttachmentModules();

    boolean mmceComplement$isAttachmentModuleActive(String id);

    ModuleRecipeConditions.Failure mmceComplement$getModuleRecipeFailure(
        ModuleRecipeData recipe);

    /**
     * MMCE-priority-ordered recipes valid for the published module state, or
     * {@code null} when the unmodified registry iterable is the fast path.
     */
    Iterable<MachineRecipe> mmceComplement$getModuleRecipeCandidates();

}
