package net.edwin.mmcecomplement.attachment;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentSelectorTag;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds attachment patterns while enforcing the rule that a parent owns any
 * position whose definition differs from a child's definition.
 */
public final class AttachmentPatternResolver {

    private AttachmentPatternResolver() {
    }

    /** Returns all transitive parents, including {@code main} when declared. */
    public static Set<String> getAncestors(String moduleId,
                                           Map<String, AttachmentModule> modules) {
        AttachmentModule module = modules.get(moduleId);
        if (module == null) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> ancestors = new LinkedHashSet<>();
        collectAncestors(module, modules, ancestors, new LinkedHashSet<>());
        return ancestors;
    }

    /**
     * Returns a module-only pattern. Conflicting positions owned by any parent
     * are omitted, while equal shared definitions remain visible on the module page.
     */
    public static TaggedPositionBlockArray getEffectiveModulePattern(
        TaggedPositionBlockArray mainPattern,
        Map<String, AttachmentModule> modules,
        String moduleId) {
        AttachmentModule module = modules.get(moduleId);
        if (module == null) {
            return new TaggedPositionBlockArray();
        }

        TaggedPositionBlockArray parents = new TaggedPositionBlockArray();
        appendParentsRootFirst(parents, mainPattern, modules,
            getAncestors(moduleId, modules), moduleId, new LinkedHashSet<>());
        TaggedPositionBlockArray result = new TaggedPositionBlockArray();
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry
            : module.getPattern().getPattern().entrySet()) {
            BlockArray.BlockInformation parentDefinition = parents.getPattern().get(entry.getKey());
            if (parentDefinition != null && !parentDefinition.equals(entry.getValue())) {
                continue;
            }
            copyEntry(module.getPattern(), result, entry.getKey(), entry.getValue());
        }
        result.flushTileBlocksCache();
        return result;
    }

    /** Builds the selected module together with every transitive parent. */
    public static TaggedPositionBlockArray getMergedPreviewPattern(
        TaggedPositionBlockArray mainPattern,
        Map<String, AttachmentModule> modules,
        String moduleId) {
        LinkedHashSet<String> ancestors = new LinkedHashSet<>(getAncestors(moduleId, modules));
        TaggedPositionBlockArray result = new TaggedPositionBlockArray();
        appendParentsRootFirst(result, mainPattern, modules, ancestors,
            moduleId, new LinkedHashSet<>());
        appendPreservingParent(result,
            modules.get(moduleId).getEffectivePattern(mainPattern, modules));
        result.flushTileBlocksCache();
        return result;
    }

    /** Adds a child pattern without allowing it to replace an existing parent position. */
    public static void appendPreservingParent(TaggedPositionBlockArray target,
                                              TaggedPositionBlockArray child) {
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry
            : child.getPattern().entrySet()) {
            if (target.getPattern().containsKey(entry.getKey())) {
                continue;
            }
            copyEntry(child, target, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns a controller-facing version of a lazily created attachment
     * pattern, adding it to MMCE's cache on first use.
     */
    public static TaggedPositionBlockArray getRotatedPattern(
        TaggedPositionBlockArray pattern,
        EnumFacing facing) {
        if (pattern == null || facing == null || !facing.getAxis().isHorizontal()) {
            return null;
        }
        TaggedPositionBlockArray cached = BlockArrayCache.getBlockArrayCache(pattern, facing);
        if (cached != null) {
            return cached;
        }

        TaggedPositionBlockArray rotated = pattern;
        EnumFacing current = EnumFacing.NORTH;
        while (current != facing) {
            current = current.rotateYCCW();
            rotated = rotated.rotateYCCW();
        }
        rotated.flushTileBlocksCache();
        BlockArrayCache.addBlockArrayCache(rotated, facing);
        return rotated;
    }

    private static void appendParentsRootFirst(TaggedPositionBlockArray target,
                                               TaggedPositionBlockArray mainPattern,
                                               Map<String, AttachmentModule> modules,
                                               Set<String> ancestors,
                                               String moduleId,
                                               Set<String> appended) {
        AttachmentModule module = modules.get(moduleId);
        if (module == null) {
            return;
        }
        for (String parentId : module.getDependencies()) {
            if (!ancestors.contains(parentId) || !appended.add(parentId)) {
                continue;
            }
            if (AttachmentResolver.MAIN.equals(parentId)) {
                appendPreservingParent(target, mainPattern);
                continue;
            }
            appendParentsRootFirst(target, mainPattern, modules, ancestors,
                parentId, appended);
            appendPreservingParent(target,
                modules.get(parentId).getEffectivePattern(mainPattern, modules));
        }
    }

    private static void collectAncestors(AttachmentModule module,
                                         Map<String, AttachmentModule> modules,
                                         Set<String> ancestors,
                                         Set<String> visiting) {
        if (!visiting.add(module.getId())) {
            return;
        }
        for (String dependency : module.getDependencies()) {
            if (!ancestors.add(dependency) || AttachmentResolver.MAIN.equals(dependency)) {
                continue;
            }
            AttachmentModule parent = modules.get(dependency);
            if (parent != null) {
                collectAncestors(parent, modules, ancestors, visiting);
            }
        }
        visiting.remove(module.getId());
    }

    private static void copyEntry(TaggedPositionBlockArray source,
                                  TaggedPositionBlockArray target,
                                  BlockPos pos,
                                  BlockArray.BlockInformation info) {
        target.addBlock(pos, info);
        ComponentSelectorTag tag = source.getTag(pos);
        if (tag != null) {
            target.setTag(pos, tag);
        }
    }
}
