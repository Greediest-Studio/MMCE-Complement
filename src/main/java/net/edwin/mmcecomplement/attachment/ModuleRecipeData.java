package net.edwin.mmcecomplement.attachment;

import java.util.Set;

/** Module restrictions attached to a prepared or built MMCE recipe. */
public interface ModuleRecipeData {

    Set<String> mmceComplement$getRequiredModules();

    Set<String> mmceComplement$getForbiddenModules();
}
