package net.edwin.mmcecomplement.accelerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcceleratorHatchLogicTest {

    private static final double[] DEFAULT_MULTIPLIERS = {
        0.6D, 0.36D, 0.216D, 0.1296D, 0.07776D, 0.046656D, 0.0279936D, 0.01679616D
    };

    @Test
    void appliesAllEightTiers() {
        for (int tier = 1; tier <= 8; tier++) {
            int[] counts = new int[8];
            counts[tier - 1] = 1;
            assertEquals(DEFAULT_MULTIPLIERS[tier - 1],
                AcceleratorHatchLogic.getEffectiveDurationMultiplier(
                    counts, DEFAULT_MULTIPLIERS), 0.0D);
        }
    }

    @Test
    void onlyTheHighestInstalledTierTakesEffect() {
        assertEquals(DEFAULT_MULTIPLIERS[6],
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(
            new int[] {4, 0, 1, 0, 0, 0, 1, 0}, DEFAULT_MULTIPLIERS), 0.0D);
    }

    @Test
    void duplicateHatchesOfOneTierDoNotStack() {
        assertEquals(0.6D,
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(
                new int[] {20, 0, 0, 0, 0, 0, 0, 0}, DEFAULT_MULTIPLIERS), 0.0D);
    }

    @Test
    void noHatchLeavesDurationUnchanged() {
        assertEquals(1.0D,
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(
                new int[8], DEFAULT_MULTIPLIERS), 0.0D);
    }

    @Test
    void usesConfiguredMultipliers() {
        assertEquals(0.123D,
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(
                new int[] {0, 0, 1}, new double[] {0.8D, 0.4D, 0.123D}), 0.0D);
    }
}
