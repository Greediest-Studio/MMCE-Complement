package net.edwin.mmcecomplement.redstoneinterface;

import java.util.Collection;

/** Side-effect-free signal aggregation and clamping helpers. */
public final class RedstoneSignalLogic {

    private RedstoneSignalLogic() {
    }

    public static int clampOutput(int value) {
        return Math.max(0, Math.min(15, value));
    }

    public static int aggregate(Collection<Integer> signals, int operator) {
        if (signals == null || signals.isEmpty()) {
            return 0;
        }

        if (operator == RedstoneValueDefinition.OPERATOR_MIN) {
            int result = Integer.MAX_VALUE;
            for (Integer signal : signals) {
                if (signal != null) {
                    result = Math.min(result, Math.max(0, signal));
                }
            }
            return result == Integer.MAX_VALUE ? 0 : result;
        }

        if (operator == RedstoneValueDefinition.OPERATOR_SUM) {
            long result = 0L;
            for (Integer signal : signals) {
                if (signal != null) {
                    result += Math.max(0, signal);
                    if (result >= Integer.MAX_VALUE) {
                        return Integer.MAX_VALUE;
                    }
                }
            }
            return (int) result;
        }

        int result = 0;
        for (Integer signal : signals) {
            if (signal != null) {
                result = Math.max(result, Math.max(0, signal));
            }
        }
        return result;
    }
}
