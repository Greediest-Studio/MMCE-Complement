package net.edwin.mmcecomplement.batch;

/** Pure arithmetic for batch duration and parallelism scaling. */
public final class BatchProcessingLogic {

    private BatchProcessingLogic() {}

    /**
     * Chooses the largest integer factor whose theoretical duration does not
     * exceed the configured limit. When the limit is no greater than one
     * theoretical operation, a single operation is retained as requested.
     */
    public static int calculateFactor(double theoreticalDuration,
                                      int maxBatchTime,
                                      int maxFactor) {
        if (!(theoreticalDuration > 0.0D)
            || Double.isInfinite(theoreticalDuration)
            || maxBatchTime <= 0
            || maxFactor <= 1
            || maxBatchTime <= theoreticalDuration) {
            return 1;
        }

        // Use the theoretical duration rather than the final rounded tick
        // count. This is what makes a 0.60-tick recipe fit exactly 1000 times
        // into a 600-tick batch without allowing a nominally oversized batch.
        // Modifier evaluation is float-based in MMCE. Permit only a tiny
        // relative tolerance so values such as float 0.60 do not become 999
        // because their binary representation is 0.6000000238...
        double tolerance = Math.max(1.0E-9D, maxBatchTime * 1.0E-7D);
        double ratio = (maxBatchTime + tolerance) / theoreticalDuration;
        if (ratio >= maxFactor) {
            return maxFactor;
        }
        return Math.max(1, (int) Math.floor(ratio));
    }

    public static int multiplyParallelismSaturated(int parallelism, int factor) {
        if (parallelism <= 0 || factor <= 0) {
            return 1;
        }
        long result = (long) parallelism * factor;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    /**
     * Scales only the eligible part of a parallelism budget.  Factory
     * controllers can expose extra/custom threads in the same budget, but a
     * batch hatch must not multiply those threads.
     */
    public static int multiplyParallelismExcluding(int parallelism,
                                                   int excludedParallelism,
                                                   int factor) {
        if (parallelism <= 0 || factor <= 0) {
            return 1;
        }
        int excluded = Math.max(0, Math.min(parallelism, excludedParallelism));
        int eligible = parallelism - excluded;
        if (eligible == 0) {
            return excluded;
        }
        int scaled = multiplyParallelismSaturated(eligible, factor);
        long result = (long) scaled + excluded;
        return result >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    /** Number of full base-parallel batches needed for the actual input set. */
    public static int factorForActualParallelism(int actualParallelism,
                                                 int baseParallelism,
                                                 int requestedFactor) {
        if (requestedFactor <= 1 || actualParallelism <= baseParallelism) {
            return 1;
        }
        int safeBase = Math.max(1, baseParallelism);
        long factor = ((long) actualParallelism + safeBase - 1L) / safeBase;
        return (int) Math.min(requestedFactor, factor);
    }
}
