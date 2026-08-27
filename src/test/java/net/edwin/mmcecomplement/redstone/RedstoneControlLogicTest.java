package net.edwin.mmcecomplement.redstone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedstoneControlLogicTest {

    @Test
    void cyclesFromOneThroughFifteen() {
        assertEquals(2, RedstoneControlLogic.nextThreshold(1));
        assertEquals(15, RedstoneControlLogic.nextThreshold(14));
        assertEquals(1, RedstoneControlLogic.nextThreshold(15));
    }

    @Test
    void clampsSavedThresholdsToValidSignalLevels() {
        assertEquals(1, RedstoneControlLogic.clampThreshold(0));
        assertEquals(15, RedstoneControlLogic.clampThreshold(16));
    }

    @Test
    void shutsDownAtOrAboveThreshold() {
        assertFalse(RedstoneControlLogic.shouldShutdown(7, 8));
        assertTrue(RedstoneControlLogic.shouldShutdown(8, 8));
        assertTrue(RedstoneControlLogic.shouldShutdown(15, 8));
    }
}
