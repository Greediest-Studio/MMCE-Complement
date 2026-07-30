package net.edwin.mmcecomplement.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompactQuantityFormatterTest {

    @Test
    void keepsThresholdAndUsesThreeSignificantDigits() {
        assertEquals("1000", CompactQuantityFormatter.format(1000));
        assertEquals("1.00k", CompactQuantityFormatter.format(1001));
        assertEquals("12.3k", CompactQuantityFormatter.format(12_345));
        assertEquals("999k", CompactQuantityFormatter.format(999_499));
        assertEquals("1.00M", CompactQuantityFormatter.format(999_500));
        assertEquals("1.00M", CompactQuantityFormatter.format(1_000_000));
        assertEquals("1.00G", CompactQuantityFormatter.format(999_500_000));
        assertEquals("2.15G",
            CompactQuantityFormatter.format(Integer.MAX_VALUE));
    }
}
