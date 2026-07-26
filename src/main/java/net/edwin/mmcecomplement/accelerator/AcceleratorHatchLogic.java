package net.edwin.mmcecomplement.accelerator;

/** Pure highest-tier selection logic for accelerator hatches. */
public final class AcceleratorHatchLogic {

    public static final double TIER_FACTOR = 0.6D;

    private AcceleratorHatchLogic() {}

    public static double getEffectiveDurationMultiplier(int[] tierCounts) {
        if (tierCounts == null) {
            return 1.0D;
        }
        for (int tier = tierCounts.length - 1; tier >= 0; tier--) {
            if (tierCounts[tier] > 0) {
                return Math.pow(TIER_FACTOR, tier + 1);
            }
        }
        return 1.0D;
    }
}
