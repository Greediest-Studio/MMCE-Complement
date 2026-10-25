package net.edwin.mmcecomplement.attachment;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import net.minecraft.util.math.BlockPos;

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

    /**
     * Marks the attachment area as changed.  The controller performs the
     * actual pattern match on its next server tick (or at the 40 tick
     * fallback), keeping Forge block events cheap while making replacements
     * visible immediately.
     */
    void mmceComplement$markAttachmentDirty(BlockPos changedPosition);

}
