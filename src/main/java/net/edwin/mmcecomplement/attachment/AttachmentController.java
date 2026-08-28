package net.edwin.mmcecomplement.attachment;

import java.util.Set;

/** Runtime attachment state exposed by MMCE machine controllers. */
public interface AttachmentController {

    Set<String> mmceComplement$getActiveAttachmentModules();

    boolean mmceComplement$isAttachmentModuleActive(String id);

    ModuleRecipeConditions.Failure mmceComplement$getModuleRecipeFailure(
        ModuleRecipeData recipe);

}
