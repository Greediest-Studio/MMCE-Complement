package net.edwin.mmcecomplement.attachment;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Immutable, identity-keyed snapshot of module recipe conditions.
 *
 * <p>The snapshot is rebuilt by the controller's structure-check task after
 * attachment matching has completed. Recipe lifecycle boundaries only read
 * the published result; the asynchronous recipe search never consults it.</p>
 */
public final class ModuleRecipeConditionCache {

    private volatile Map<ModuleRecipeData, ModuleRecipeConditions.Failure> failures =
        Collections.emptyMap();

    private volatile boolean hasRestrictions;

    /**
     * Replaces the complete snapshot. Recipe objects are identity-stable in
     * MMCE's registry, so identity keys avoid invoking addon-defined equality
     * implementations from asynchronous recipe tasks.
     */
    public void rebuild(Iterable<?> recipes,
                        Collection<String> activeModules) {
        IdentityHashMap<ModuleRecipeData, ModuleRecipeConditions.Failure> rebuilt =
            new IdentityHashMap<>();
        boolean restricted = false;
        for (Object candidate : recipes) {
            ModuleRecipeData recipe = (ModuleRecipeData) candidate;
            restricted |= ModuleRecipeConditions.hasRestrictions(recipe);
            rebuilt.put(recipe, ModuleRecipeConditions.evaluate(recipe, activeModules));
        }
        failures = Collections.unmodifiableMap(rebuilt);
        // Publish this last: seeing true guarantees the corresponding snapshot
        // has already been made visible to recipe-search worker threads.
        hasRestrictions = restricted;
    }

    /**
     * Returns a precomputed result without consulting the active module set.
     * A restricted recipe missing from the current registry snapshot fails
     * closed until the next structure pass rebuilds the cache.
     */
    public ModuleRecipeConditions.Failure get(ModuleRecipeData recipe) {
        ModuleRecipeConditions.Failure failure = failures.get(recipe);
        return failure == null
            ? ModuleRecipeConditions.Failure.MISSING_REQUIRED
            : failure;
    }

    public boolean hasRestrictions() {
        return hasRestrictions;
    }

    public void clear() {
        hasRestrictions = false;
        failures = Collections.emptyMap();
    }
}
