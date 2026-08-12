package net.edwin.mmcecomplement.accelerator;

/** Pure highest-tier selection logic for accelerator hatches. */
public final class AcceleratorHatchLogic {

    private AcceleratorHatchLogic() {}

    public static double getEffectiveDurationMultiplier(int[] tierCounts,
                                                        double[] durationMultipliers) {
        if (tierCounts == null || durationMultipliers == null) {
            return 1.0D;
        }
        int tierCount = Math.min(tierCounts.length, durationMultipliers.length);
        for (int tier = tierCount - 1; tier >= 0; tier--) {
            if (tierCounts[tier] > 0) {
                return durationMultipliers[tier];
            }
        }
        return 1.0D;
    }
}
