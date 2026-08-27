package net.edwin.mmcecomplement.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentCheckCacheTest {

    @Test
    void skipsRepeatedChecksUntilTheFallbackInterval() {
        AttachmentCheckCache cache = new AttachmentCheckCache(100);

        assertTrue(cache.shouldRefresh(20L, false));
        cache.markRefreshed(20L, true);

        assertFalse(cache.shouldRefresh(21L, false));
        assertFalse(cache.shouldRefresh(119L, false));
        assertTrue(cache.shouldRefresh(120L, false));
    }

    @Test
    void worldOrControllerContextChangesInvalidateImmediately() {
        AttachmentCheckCache cache = new AttachmentCheckCache(100);
        cache.markRefreshed(20L, true);

        assertTrue(cache.shouldRefresh(21L, true));
        assertFalse(cache.shouldRefresh(21L, false));

        cache.observeWorldChange(21L, true);
        assertTrue(cache.shouldRefresh(21L, false));
    }

    @Test
    void unloadedAreasRemainDirtyUntilACompleteCheck() {
        AttachmentCheckCache cache = new AttachmentCheckCache(100);

        cache.markRefreshed(20L, false);
        assertTrue(cache.shouldRefresh(21L, false));

        cache.markRefreshed(21L, true);
        assertFalse(cache.shouldRefresh(22L, false));
    }

    @Test
    void resetAndWorldTimeRollbackForceARefresh() {
        AttachmentCheckCache cache = new AttachmentCheckCache(100);
        cache.markRefreshed(200L, true);

        assertTrue(cache.shouldRefresh(10L, false));
        cache.markRefreshed(10L, true);
        cache.reset();
        assertTrue(cache.shouldRefresh(11L, false));
    }
}
