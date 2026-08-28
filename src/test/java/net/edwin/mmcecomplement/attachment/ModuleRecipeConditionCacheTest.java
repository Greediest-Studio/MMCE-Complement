package net.edwin.mmcecomplement.attachment;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleRecipeConditionCacheTest {

    @Test
    void evaluatesConditionsWhenRebuiltAndOnlyReadsSnapshotAfterwards() {
        CountingRecipe allowed = new CountingRecipe("cooling", null);
        CountingRecipe blocked = new CountingRecipe("compression", null);
        ModuleRecipeConditionCache cache = new ModuleRecipeConditionCache();

        cache.rebuild(Arrays.asList(allowed, blocked), Collections.singleton("cooling"));
        allowed.resetReads();
        blocked.resetReads();

        assertEquals(ModuleRecipeConditions.Failure.NONE, cache.get(allowed));
        assertEquals(ModuleRecipeConditions.Failure.MISSING_REQUIRED, cache.get(blocked));
        assertEquals(0, allowed.getReads());
        assertEquals(0, blocked.getReads());

    }

    @Test
    void missingRestrictedRecipeFailsClosedUntilNextStructureSnapshot() {
        ModuleRecipeConditionCache cache = new ModuleRecipeConditionCache();
        CountingRecipe recipe = new CountingRecipe(null, "heater");

        assertEquals(ModuleRecipeConditions.Failure.MISSING_REQUIRED, cache.get(recipe));

        cache.rebuild(Collections.singletonList(recipe), Collections.emptySet());
        assertEquals(ModuleRecipeConditions.Failure.NONE, cache.get(recipe));

        cache.clear();
        assertEquals(ModuleRecipeConditions.Failure.MISSING_REQUIRED, cache.get(recipe));
    }

    private static final class CountingRecipe implements ModuleRecipeData {

        private final Set<String> required = new LinkedHashSet<>();
        private final Set<String> forbidden = new LinkedHashSet<>();
        private int reads;

        private CountingRecipe(String required, String forbidden) {
            if (required != null) {
                this.required.add(required);
            }
            if (forbidden != null) {
                this.forbidden.add(forbidden);
            }
        }

        @Override
        public Set<String> mmceComplement$getRequiredModules() {
            reads++;
            return required;
        }

        @Override
        public Set<String> mmceComplement$getForbiddenModules() {
            reads++;
            return forbidden;
        }

        private void resetReads() {
            reads = 0;
        }

        private int getReads() {
            return reads;
        }
    }
}
