package net.edwin.mmcecomplement.overclock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverclockHatchLogicTest {

    private static final double[] ENERGY = {4, 16, 64, 256, 1024, 4096};
    private static final double[] DURATION = {
        0.5D, 0.25D, 0.125D, 0.0625D, 0.03125D, 0.015625D
    };

    @Test
    void appliesAllSixDefaultTiers() {
        for (int tier = 0; tier < 6; tier++) {
            int[] counts = new int[6];
            counts[tier] = 1;
            OverclockHatchLogic.Result result = OverclockHatchLogic.getEffectiveMultipliers(
                counts, ENERGY, DURATION, false);
            assertEquals(ENERGY[tier], result.getEnergyMultiplier());
            assertEquals(DURATION[tier], result.getDurationMultiplier());
        }
    }

    @Test
    void nonStackingModeSelectsTheHighestInstalledTier() {
        OverclockHatchLogic.Result result = OverclockHatchLogic.getEffectiveMultipliers(
            new int[] {4, 0, 1, 0, 0, 0}, ENERGY, DURATION, false);
        assertEquals(64.0D, result.getEnergyMultiplier());
        assertEquals(0.125D, result.getDurationMultiplier());
    }

    @Test
    void stackingModeMultipliesEveryInstalledHatch() {
        OverclockHatchLogic.Result result = OverclockHatchLogic.getEffectiveMultipliers(
            new int[] {2, 1, 0, 0, 0, 0}, ENERGY, DURATION, true);
        assertEquals(256.0D, result.getEnergyMultiplier());
        assertEquals(0.0625D, result.getDurationMultiplier());
    }

    @Test
    void noHatchLeavesRecipesUnchanged() {
        OverclockHatchLogic.Result result = OverclockHatchLogic.getEffectiveMultipliers(
            new int[6], ENERGY, DURATION, false);
        assertEquals(1.0D, result.getEnergyMultiplier());
        assertEquals(1.0D, result.getDurationMultiplier());
    }

    @Test
    void configuredValuesAreUsedIndependently() {
        OverclockHatchLogic.Result result = OverclockHatchLogic.getEffectiveMultipliers(
            new int[] {0, 1}, new double[] {3, 7}, new double[] {0.8D, 0.3D}, false);
        assertEquals(7.0D, result.getEnergyMultiplier());
        assertEquals(0.3D, result.getDurationMultiplier());
    }
}
