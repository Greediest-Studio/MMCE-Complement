package net.edwin.mmcecomplement.attachment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Resolves physical matches into effective modules without declaration-order bias. */
public final class AttachmentResolver {

    public static final String MAIN = "main";

    private AttachmentResolver() {
    }

    public static Set<String> resolve(Set<String> matched,
                                      Map<String, ? extends Collection<String>> dependencies,
                                      Map<String, ? extends Collection<String>> conflicts) {
        LinkedHashSet<String> dependencyValid = new LinkedHashSet<>();
        dependencyValid.add(MAIN);

        boolean changed;
        do {
            changed = false;
            for (String id : matched) {
                Collection<String> required = dependencies.get(id);
                if (!dependencyValid.contains(id)
                    && dependencyValid.containsAll(required == null ? Collections.emptySet() : required)) {
                    dependencyValid.add(id);
                    changed = true;
                }
            }
        } while (changed);

        LinkedHashSet<String> disabled = new LinkedHashSet<>();
        for (String id : dependencyValid) {
            if (MAIN.equals(id)) {
                continue;
            }
            Collection<String> incompatible = conflicts.get(id);
            if (incompatible == null) {
                continue;
            }
            for (String other : incompatible) {
                if (dependencyValid.contains(other)) {
                    disabled.add(id);
                    if (!MAIN.equals(other)) {
                        disabled.add(other);
                    }
                }
            }
        }

        LinkedHashSet<String> effective = new LinkedHashSet<>(dependencyValid);
        effective.remove(MAIN);
        effective.removeAll(disabled);
        do {
            changed = false;
            for (String id : new LinkedHashSet<>(effective)) {
                Collection<String> required = dependencies.get(id);
                if (required != null && !dependenciesPresent(required, effective)) {
                    effective.remove(id);
                    changed = true;
                }
            }
        } while (changed);
        return effective;
    }

    private static boolean dependenciesPresent(Collection<String> required, Set<String> effective) {
        for (String dependency : required) {
            if (!MAIN.equals(dependency) && !effective.contains(dependency)) {
                return false;
            }
        }
        return true;
    }
}
