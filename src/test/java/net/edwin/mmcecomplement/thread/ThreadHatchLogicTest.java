package net.edwin.mmcecomplement.thread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadHatchLogicTest {

    private static final double[] DEFAULT_MULTIPLIERS = {2, 3, 5, 8, 12, 16};

    @Test
    void appliesTheSixTierMultipliers() {
        int[] expected = {4, 6, 10, 16, 24, 32};
        for (int tier = 1; tier <= expected.length; tier++) {
            int[] counts = new int[6];
            counts[tier - 1] = 1;
            assertEquals(expected[tier - 1],
                ThreadHatchLogic.apply(2, 0, counts, DEFAULT_MULTIPLIERS, false));
        }
    }

    @Test
    void doesNotMultiplyExtraNormalThreads() {
        assertEquals(17, ThreadHatchLogic.apply(
            2, 1, new int[] {0, 0, 0, 1, 0, 0}, DEFAULT_MULTIPLIERS, false));
    }

    @Test
    void leavesTheLimitUnchangedWithoutAHatch() {
        assertEquals(7, ThreadHatchLogic.apply(
            5, 2, new int[6], DEFAULT_MULTIPLIERS, false));
    }

    @Test
    void roundsConfiguredFractionalMultipliersUp() {
        assertEquals(4, ThreadHatchLogic.apply(
            3, 0, new int[] {1}, new double[] {1.25D}, false));
    }

    @Test
    void nonStackingModeUsesTheHighestTierNotTheLargestConfiguredValue() {
        assertEquals(4, ThreadHatchLogic.apply(
            2, 0, new int[] {1, 0, 1}, new double[] {100, 50, 2}, false));
    }

    @Test
    void stackingModeMultipliesEveryInstalledHatch() {
        assertEquals(24, ThreadHatchLogic.apply(
            2, 0, new int[] {2, 1, 0, 0, 0, 0}, DEFAULT_MULTIPLIERS, true));
    }

    @Test
    void saturatesInsteadOfOverflowing() {
        assertEquals(Integer.MAX_VALUE,
            ThreadHatchLogic.apply(Integer.MAX_VALUE, 10,
                new int[] {0, 0, 0, 0, 0, 1}, DEFAULT_MULTIPLIERS, false));
    }
}
