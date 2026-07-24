package net.edwin.mmcecomplement.attachment;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleRecipeConditionsTest {

    @Test
    void requiresEveryListedModuleAndRejectsAnyForbiddenModule() {
        TestRecipe recipe = new TestRecipe();
        ModuleRecipeConditions.addRequired(recipe, new String[]{"cooling", "compression"});
        ModuleRecipeConditions.addForbidden(recipe, new String[]{"low_pressure"});

        assertEquals(ModuleRecipeConditions.Failure.MISSING_REQUIRED,
            ModuleRecipeConditions.evaluate(recipe, Collections.singleton("cooling")));
        assertEquals(ModuleRecipeConditions.Failure.FORBIDDEN_PRESENT,
            ModuleRecipeConditions.evaluate(recipe,
                Arrays.asList("cooling", "compression", "low_pressure")));
        assertEquals(ModuleRecipeConditions.Failure.NONE,
            ModuleRecipeConditions.evaluate(recipe, Arrays.asList("cooling", "compression")));
    }

    @Test
    void rejectsMainEmptyAndContradictoryModuleRestrictions() {
        TestRecipe recipe = new TestRecipe();
        assertThrows(IllegalArgumentException.class,
            () -> ModuleRecipeConditions.addRequired(recipe, new String[]{"main"}));
        assertThrows(IllegalArgumentException.class,
            () -> ModuleRecipeConditions.addRequired(recipe, new String[]{" "}));

        ModuleRecipeConditions.addRequired(recipe, new String[]{"cooling"});
        assertThrows(IllegalArgumentException.class,
            () -> ModuleRecipeConditions.addForbidden(recipe, new String[]{"cooling"}));
    }

    private static final class TestRecipe implements ModuleRecipeData {

        private final Set<String> required = new LinkedHashSet<>();
        private final Set<String> forbidden = new LinkedHashSet<>();

        @Override
        public Set<String> mmceComplement$getRequiredModules() {
            return required;
        }

        @Override
        public Set<String> mmceComplement$getForbiddenModules() {
            return forbidden;
        }
    }
}
