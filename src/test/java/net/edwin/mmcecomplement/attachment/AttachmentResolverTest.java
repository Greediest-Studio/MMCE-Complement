package net.edwin.mmcecomplement.attachment;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttachmentResolverTest {

    @Test
    void resolvesDependenciesTransitively() {
        Map<String, Collection<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("a", Collections.singleton("main"));
        dependencies.put("b", Collections.singleton("a"));
        dependencies.put("c", Collections.singleton("b"));

        assertEquals(set("a", "b", "c"), AttachmentResolver.resolve(
            set("c", "b", "a"), dependencies, Collections.emptyMap()));
        assertEquals(set("a"), AttachmentResolver.resolve(
            set("a", "c"), dependencies, Collections.emptyMap()));
    }

    @Test
    void allowsParallelModulesWithNoDependencies() {
        Map<String, Collection<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("alternate", Collections.emptySet());
        assertEquals(set("alternate"), AttachmentResolver.resolve(
            set("alternate"), dependencies, Collections.emptyMap()));
    }

    @Test
    void disablesBothSidesOfAConflictAndTheirDependants() {
        Map<String, Collection<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("a", Collections.singleton("main"));
        dependencies.put("b", Collections.singleton("main"));
        dependencies.put("c", Collections.singleton("b"));
        Map<String, Collection<String>> conflicts = new LinkedHashMap<>();
        conflicts.put("a", Collections.singleton("b"));

        assertEquals(Collections.emptySet(), AttachmentResolver.resolve(
            set("a", "b", "c"), dependencies, conflicts));
    }

    @SafeVarargs
    private static <T> Set<T> set(T... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
