package net.edwin.mmcecomplement.attachment;

import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable definition of one optional structure attached to a machine. */
public final class AttachmentModule {

    private final String id;
    private final TaggedPositionBlockArray pattern;
    private final Set<String> dependencies;
    private final Set<String> conflicts;
    private final boolean upgrade;
    private final String modelName;
    private volatile TaggedPositionBlockArray effectivePattern;
    private volatile long effectivePatternMainUid = Long.MIN_VALUE;

    public AttachmentModule(String id,
                            TaggedPositionBlockArray pattern,
                            Set<String> dependencies,
                            Set<String> conflicts,
                            boolean upgrade) {
        this(id, pattern, dependencies, conflicts, upgrade, null);
    }

    public AttachmentModule(String id,
                            TaggedPositionBlockArray pattern,
                            Set<String> dependencies,
                            Set<String> conflicts,
                            boolean upgrade,
                            String modelName) {
        this.id = id;
        this.pattern = pattern;
        this.dependencies = Collections.unmodifiableSet(new LinkedHashSet<>(dependencies));
        this.conflicts = Collections.unmodifiableSet(new LinkedHashSet<>(conflicts));
        this.upgrade = upgrade;
        this.modelName = modelName == null || modelName.trim().isEmpty()
            ? null : modelName.trim();
    }

    public String getId() {
        return id;
    }

    public TaggedPositionBlockArray getPattern() {
        return pattern;
    }

    /** Returns the parent-filtered pattern, cached by the owning main pattern UID. */
    public TaggedPositionBlockArray getEffectivePattern(TaggedPositionBlockArray mainPattern,
                                                        java.util.Map<String, AttachmentModule> modules) {
        TaggedPositionBlockArray cached = effectivePattern;
        if (cached != null && effectivePatternMainUid == mainPattern.uid) {
            return cached;
        }
        synchronized (this) {
            if (effectivePattern == null || effectivePatternMainUid != mainPattern.uid) {
                effectivePattern = AttachmentPatternResolver.getEffectiveModulePattern(
                    mainPattern, modules, id);
                effectivePatternMainUid = mainPattern.uid;
            }
            return effectivePattern;
        }
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Set<String> getConflicts() {
        return conflicts;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    /** Optional registered MMCE custom-model name for separate rendering. */
    public String getModelName() {
        return modelName;
    }
}
