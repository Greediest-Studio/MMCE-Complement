package net.edwin.mmcecomplement.event;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentControllerPositionIndexTest {

    @Test
    void chunkLookupOnlyVisitsControllersWatchingThatRegion() {
        AttachmentControllerPositionIndex<Object> index =
            new AttachmentControllerPositionIndex<>();
        Object first = new Object();
        Object second = new Object();
        BlockPos watched = new BlockPos(32, 70, -17);

        index.replace(first, Collections.singletonList(
            new StructureBoundingBox(32, 60, -17, 35, 80, -14)));
        index.replace(second, Collections.singletonList(
            new StructureBoundingBox(48, 60, -17, 51, 80, -14)));

        List<Object> found = new ArrayList<>();
        index.forEachAt(watched, found::add);

        assertEquals(Collections.singletonList(first), found);
    }

    @Test
    void replacingAndRemovingControllersClearsStalePositions() {
        AttachmentControllerPositionIndex<Object> index =
            new AttachmentControllerPositionIndex<>();
        Object first = new Object();
        Object second = new Object();
        BlockPos oldPosition = new BlockPos(15, 2, 0);
        BlockPos sharedPosition = new BlockPos(16, 5, 0);
        BlockPos newPosition = new BlockPos(32, 8, 0);
        StructureBoundingBox oldBounds =
            new StructureBoundingBox(15, 0, 0, 16, 10, 0);
        StructureBoundingBox secondBounds =
            new StructureBoundingBox(16, 0, 0, 17, 10, 0);
        StructureBoundingBox newBounds =
            new StructureBoundingBox(32, 0, 0, 33, 10, 0);
        index.replace(first, Collections.singletonList(oldBounds));
        index.replace(second, Collections.singletonList(secondBounds));

        index.replace(first, Collections.singletonList(newBounds));
        List<Object> found = new ArrayList<>();
        index.forEachAt(oldPosition, found::add);
        assertTrue(found.isEmpty());
        index.forEachAt(sharedPosition, found::add);
        assertEquals(Collections.singletonList(second), found);
        index.remove(first);
        index.remove(second);
        assertTrue(index.isEmpty());
    }

    @Test
    void controllerKeysUseIdentity() {
        AttachmentControllerPositionIndex<EqualKey> index =
            new AttachmentControllerPositionIndex<>();
        EqualKey first = new EqualKey();
        EqualKey second = new EqualKey();
        BlockPos watched = new BlockPos(4, 5, 6);
        StructureBoundingBox watchedBounds =
            new StructureBoundingBox(4, 0, 6, 5, 10, 7);
        index.replace(first, Collections.singletonList(watchedBounds));
        index.replace(second, Collections.singletonList(watchedBounds));

        List<EqualKey> found = new ArrayList<>();
        index.forEachAt(watched, found::add);

        assertEquals(2, found.size());
        index.remove(first);
        found.clear();
        index.forEachAt(watched, found::add);
        assertEquals(1, found.size());
        assertSame(second, found.get(0));
        assertFalse(index.isEmpty());
    }

    private static final class EqualKey {
        @Override
        public boolean equals(Object other) { return other instanceof EqualKey; }

        @Override
        public int hashCode() { return 1; }
    }
}
