package net.edwin.mmcecomplement.thread;

/** Pure thread-hatch multiplier logic, kept separate from world scanning. */
public final class ThreadHatchLogic {

    private ThreadHatchLogic() {}

    /**
     * Applies a hatch tier to normal machine threads only.
     *
     * @param baseThreads machine-defined normal thread count
     * @param extraThreads externally added normal threads; never multiplied
     * @param tierCounts formed hatch counts indexed from MK I through MK VI
     * @param multipliers configured multipliers indexed from MK I through MK VI
     * @param allowStacking whether every hatch should multiply with every other hatch
     */
    public static int apply(int baseThreads, int extraThreads, int[] tierCounts,
                            double[] multipliers, boolean allowStacking) {
        double multiplier = getEffectiveMultiplier(tierCounts, multipliers, allowStacking);
        double total = Math.ceil(baseThreads * multiplier) + extraThreads;
        if (Double.isNaN(total)) {
            return extraThreads;
        }
        if (total >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (total <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) total;
    }

    public static double getEffectiveMultiplier(int[] tierCounts, double[] multipliers,
                                                boolean allowStacking) {
        if (tierCounts == null || multipliers == null) {
            return 1.0D;
        }
        int tiers = Math.min(tierCounts.length, multipliers.length);
        if (!allowStacking) {
            for (int tier = tiers - 1; tier >= 0; tier--) {
                if (tierCounts[tier] > 0) {
                    return sanitizeMultiplier(multipliers[tier]);
                }
            }
            return 1.0D;
        }

        // A configured zero multiplier wins regardless of iteration order and
        // avoids Infinity * 0 producing NaN for very large structures.
        for (int tier = 0; tier < tiers; tier++) {
            if (tierCounts[tier] > 0 && sanitizeMultiplier(multipliers[tier]) == 0.0D) {
                return 0.0D;
            }
        }

        double result = 1.0D;
        for (int tier = 0; tier < tiers; tier++) {
            int count = Math.max(0, tierCounts[tier]);
            if (count == 0) {
                continue;
            }
            result *= Math.pow(sanitizeMultiplier(multipliers[tier]), count);
            if (Double.isInfinite(result) || result >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return result;
    }

    private static double sanitizeMultiplier(double multiplier) {
        if (Double.isNaN(multiplier) || multiplier < 0.0D) {
            return 0.0D;
        }
        return multiplier;
    }
}
