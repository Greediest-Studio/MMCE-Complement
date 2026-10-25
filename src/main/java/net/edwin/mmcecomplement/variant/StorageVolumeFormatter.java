package net.edwin.mmcecomplement.variant;

import java.math.BigDecimal;
import java.util.Locale;

/** Exact bucket-unit display for storage amounts kept internally in mB. */
public final class StorageVolumeFormatter {
    private StorageVolumeFormatter() { }

    public static String format(int millibuckets) {
        if (millibuckets <= 0) return "0";
        if (millibuckets < 1000) return String.format(Locale.ROOT, ".%03d", millibuckets);
        return BigDecimal.valueOf(millibuckets, 3).stripTrailingZeros().toPlainString();
    }

    /** Display form used in explanatory text (tooltips and the GUI subtitle). */
    public static String formatWithUnit(int millibuckets) {
        return format(millibuckets) + " B";
    }
}
