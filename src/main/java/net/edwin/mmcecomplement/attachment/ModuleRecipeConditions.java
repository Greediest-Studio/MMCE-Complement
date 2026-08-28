package net.edwin.mmcecomplement.attachment;

import java.util.Collection;
import java.util.Set;

/** Shared validation and runtime evaluation for CraftTweaker module restrictions. */
public final class ModuleRecipeConditions {

    public static final String MISSING_MODULE_MESSAGE =
        "craftcheck.failure.mmce_complement.missing_module";
    public static final String FORBIDDEN_MODULE_MESSAGE =
        "craftcheck.failure.mmce_complement.forbidden_module";

    private ModuleRecipeConditions() {
    }

    public static void addRequired(ModuleRecipeData recipe, String[] moduleIds) {
        add(recipe.mmceComplement$getRequiredModules(),
            recipe.mmceComplement$getForbiddenModules(), moduleIds, "withModule");
    }

    public static void addForbidden(ModuleRecipeData recipe, String[] moduleIds) {
        add(recipe.mmceComplement$getForbiddenModules(),
            recipe.mmceComplement$getRequiredModules(), moduleIds, "withoutModule");
    }

    private static void add(Set<String> destination,
                            Set<String> opposite,
                            String[] moduleIds,
                            String method) {
        if (moduleIds == null) {
            throw new IllegalArgumentException(method + " module ID array cannot be null");
        }
        for (String moduleId : moduleIds) {
            if (moduleId == null || moduleId.trim().isEmpty()) {
                throw new IllegalArgumentException(method + " module ID cannot be empty");
            }
            String normalized = moduleId.trim();
            if (AttachmentResolver.MAIN.equals(normalized)) {
                throw new IllegalArgumentException(
                    method + " only accepts attachment module IDs; 'main' is reserved");
            }
            if (opposite.contains(normalized)) {
                throw new IllegalArgumentException("Module '" + normalized
                    + "' cannot be both required and forbidden by one recipe");
            }
            destination.add(normalized);
        }
    }

    public static Failure evaluate(ModuleRecipeData recipe,
                                   Collection<String> activeModules) {
        if (!activeModules.containsAll(recipe.mmceComplement$getRequiredModules())) {
            return Failure.MISSING_REQUIRED;
        }
        for (String forbidden : recipe.mmceComplement$getForbiddenModules()) {
            if (activeModules.contains(forbidden)) {
                return Failure.FORBIDDEN_PRESENT;
            }
        }
        return Failure.NONE;
    }

    public static boolean hasRestrictions(ModuleRecipeData recipe) {
        return !recipe.mmceComplement$getRequiredModules().isEmpty()
            || !recipe.mmceComplement$getForbiddenModules().isEmpty();
    }

    public enum Failure {
        NONE(null),
        MISSING_REQUIRED(MISSING_MODULE_MESSAGE),
        FORBIDDEN_PRESENT(FORBIDDEN_MODULE_MESSAGE);

        private final String message;

        Failure(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
