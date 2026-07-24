package net.edwin.mmcecomplement.attachment;

import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentPatternResolverTest {

    @Test
    void removesOnlyDifferingDefinitionsOwnedByAParent() {
        BlockPos differing = new BlockPos(1, 0, 0);
        BlockPos equal = new BlockPos(2, 0, 0);
        BlockPos childOnly = new BlockPos(3, 0, 0);
        BlockArray.BlockInformation parentInfo = info("parent");
        BlockArray.BlockInformation childInfo = info("child");
        BlockArray.BlockInformation sharedInfo = info("shared");

        TaggedPositionBlockArray main = pattern(differing, parentInfo, equal, sharedInfo);
        TaggedPositionBlockArray child = pattern(differing, childInfo, equal, sharedInfo,
            childOnly, childInfo);
        Map<String, AttachmentModule> modules = modules(
            new AttachmentModule("child", child, Collections.singleton("main"),
                Collections.emptySet(), true));

        TaggedPositionBlockArray effective = AttachmentPatternResolver
            .getEffectiveModulePattern(main, modules, "child");

        assertFalse(effective.hasBlockAt(differing));
        assertTrue(effective.hasBlockAt(equal));
        assertTrue(effective.hasBlockAt(childOnly));
    }

    @Test
    void mergedPreviewContainsTransitiveParentsAndKeepsRootDefinition() {
        BlockPos shared = new BlockPos(1, 0, 0);
        BlockPos parentOnly = new BlockPos(2, 0, 0);
        BlockPos childOnly = new BlockPos(3, 0, 0);
        BlockArray.BlockInformation mainInfo = info("main");
        BlockArray.BlockInformation parentInfo = info("parent");
        BlockArray.BlockInformation childInfo = info("child");

        TaggedPositionBlockArray main = pattern(shared, mainInfo);
        AttachmentModule parent = new AttachmentModule("parent",
            pattern(shared, parentInfo, parentOnly, parentInfo),
            Collections.singleton("main"), Collections.emptySet(), true);
        AttachmentModule child = new AttachmentModule("child",
            pattern(shared, parentInfo, childOnly, childInfo), Collections.singleton("parent"),
            Collections.emptySet(), true);

        TaggedPositionBlockArray merged = AttachmentPatternResolver.getMergedPreviewPattern(
            main, modules(parent, child), "child");

        assertEquals(3, merged.getPattern().size());
        assertSame(mainInfo.canonicalize(), merged.getPattern().get(shared));
        assertTrue(merged.hasBlockAt(parentOnly));
        assertTrue(merged.hasBlockAt(childOnly));
    }

    @Test
    void rotatesAndCachesLazilyCreatedAttachmentPatterns() {
        BlockPos original = new BlockPos(1, 0, 0);
        TaggedPositionBlockArray attachment = pattern(original, info("attachment"));

        TaggedPositionBlockArray rotated = AttachmentPatternResolver.getRotatedPattern(
            attachment, EnumFacing.WEST);

        assertTrue(rotated.hasBlockAt(new BlockPos(0, 0, -1)));
        assertSame(rotated, AttachmentPatternResolver.getRotatedPattern(
            attachment, EnumFacing.WEST));
    }

    private static BlockArray.BlockInformation info(String marker) {
        BlockArray.BlockInformation info = new BlockArray.BlockInformation(Collections.emptyList());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("marker", marker);
        info.setPreviewTag(tag);
        return info;
    }

    private static TaggedPositionBlockArray pattern(Object... entries) {
        TaggedPositionBlockArray pattern = new TaggedPositionBlockArray();
        for (int i = 0; i < entries.length; i += 2) {
            pattern.addBlock((BlockPos) entries[i], (BlockArray.BlockInformation) entries[i + 1]);
        }
        return pattern;
    }

    private static Map<String, AttachmentModule> modules(AttachmentModule... values) {
        Map<String, AttachmentModule> modules = new LinkedHashMap<>();
        for (AttachmentModule module : values) {
            modules.put(module.getId(), module);
        }
        return modules;
    }
}
