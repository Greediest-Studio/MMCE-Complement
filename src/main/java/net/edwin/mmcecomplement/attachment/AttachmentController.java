package net.edwin.mmcecomplement.attachment;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Collection;
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

    /** Candidate attachment regions whose chunks should receive block notifications. */
    Collection<StructureBoundingBox> mmceComplement$getAttachmentBounds();

    /**
     * Marks the attachment area as changed.  The controller performs the
     * actual pattern match on its next server tick (or at the 40 tick
     * fallback). The block event index only invokes this for chunks intersecting
     * a module bound; the controller then checks the exact three-dimensional bound.
     */
    void mmceComplement$markAttachmentDirty(BlockPos changedPosition);

}
