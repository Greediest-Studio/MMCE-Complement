package net.edwin.mmcecomplement.overclock;

/** Pure aggregation logic for overclock hatch energy and duration multipliers. */
public final class OverclockHatchLogic {

    private OverclockHatchLogic() {}

    public static Result getEffectiveMultipliers(int[] tierCounts,
                                                  double[] energyMultipliers,
                                                  double[] durationMultipliers,
                                                  boolean allowStacking) {
        if (tierCounts == null || energyMultipliers == null || durationMultipliers == null) {
            return Result.NONE;
        }
        int tiers = Math.min(tierCounts.length,
            Math.min(energyMultipliers.length, durationMultipliers.length));
        if (!allowStacking) {
            for (int tier = tiers - 1; tier >= 0; tier--) {
                if (tierCounts[tier] > 0) {
                    return new Result(sanitize(energyMultipliers[tier]),
                        sanitize(durationMultipliers[tier]));
                }
            }
            return Result.NONE;
        }

        double energy = 1.0D;
        double duration = 1.0D;
        for (int tier = 0; tier < tiers; tier++) {
            int count = Math.max(0, tierCounts[tier]);
            if (count == 0) {
                continue;
            }
            energy = multiplySaturated(energy,
                Math.pow(sanitize(energyMultipliers[tier]), count));
            duration = multiplySaturated(duration,
                Math.pow(sanitize(durationMultipliers[tier]), count));
        }
        return new Result(energy, duration);
    }

    private static double sanitize(double multiplier) {
        if (Double.isNaN(multiplier) || multiplier < 0.0D) {
            return 0.0D;
        }
        return Math.min(multiplier, Float.MAX_VALUE);
    }

    private static double multiplySaturated(double left, double right) {
        double result = left * right;
        if (Double.isNaN(result)) {
            return 0.0D;
        }
        if (Double.isInfinite(result) || result >= Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        return result;
    }

    public static final class Result {

        private static final Result NONE = new Result(1.0D, 1.0D);

        private final double energyMultiplier;
        private final double durationMultiplier;

        public Result(double energyMultiplier, double durationMultiplier) {
            this.energyMultiplier = energyMultiplier;
            this.durationMultiplier = durationMultiplier;
        }

        public double getEnergyMultiplier() {
            return energyMultiplier;
        }

        public double getDurationMultiplier() {
            return durationMultiplier;
        }
    }
}
