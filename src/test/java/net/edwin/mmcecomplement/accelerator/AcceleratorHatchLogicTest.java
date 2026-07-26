package net.edwin.mmcecomplement.accelerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcceleratorHatchLogicTest {

    @Test
    void appliesAllEightTiers() {
        for (int tier = 1; tier <= 8; tier++) {
            int[] counts = new int[8];
            counts[tier - 1] = 1;
            assertEquals(Math.pow(0.6D, tier),
                AcceleratorHatchLogic.getEffectiveDurationMultiplier(counts), 0.0D);
        }
    }

    @Test
    void onlyTheHighestInstalledTierTakesEffect() {
        assertEquals(Math.pow(0.6D, 7),
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(
                new int[] {4, 0, 1, 0, 0, 0, 1, 0}), 0.0D);
    }

    @Test
    void duplicateHatchesOfOneTierDoNotStack() {
        assertEquals(0.6D,
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(
                new int[] {20, 0, 0, 0, 0, 0, 0, 0}), 0.0D);
    }

    @Test
    void noHatchLeavesDurationUnchanged() {
        assertEquals(1.0D,
            AcceleratorHatchLogic.getEffectiveDurationMultiplier(new int[8]), 0.0D);
    }
}
