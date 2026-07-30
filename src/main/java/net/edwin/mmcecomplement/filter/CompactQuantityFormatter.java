package net.edwin.mmcecomplement.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Locale-neutral three-significant-digit quantity labels for compact GUIs. */
public final class CompactQuantityFormatter {

    private CompactQuantityFormatter() { }

    public static String format(long amount) {
        long safe = Math.max(0L, amount);
        if (safe >= 999_500_000L) return scaled(safe, 1_000_000_000L, "G");
        if (safe >= 999_500L) return scaled(safe, 1_000_000L, "M");
        if (safe > 1_000L) return scaled(safe, 1_000L, "k");
        return Long.toString(safe);
    }

    private static String scaled(long amount, long unit, String suffix) {
        BigDecimal value = BigDecimal.valueOf(amount)
            .divide(BigDecimal.valueOf(unit), 6, RoundingMode.HALF_UP);
        int integerDigits = Math.max(1, value.precision() - value.scale());
        int decimals = Math.max(0, 3 - integerDigits);
        return value.setScale(decimals, RoundingMode.HALF_UP)
            .toPlainString() + suffix;
    }
}
