package net.edwin.mmcecomplement.variant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageVolumeFormatterTest {
    @Test void subBucketAmountsHaveThreeDigitsAndNoLeadingZero() {
        assertEquals(".001", StorageVolumeFormatter.format(1));
        assertEquals(".010", StorageVolumeFormatter.format(10));
        assertEquals(".100", StorageVolumeFormatter.format(100));
        assertEquals(".500", StorageVolumeFormatter.format(500));
        assertEquals(".999", StorageVolumeFormatter.format(999));
    }

    @Test void bucketAmountsRemainExactAtBoundariesWithoutScientificNotation() {
        assertEquals("0", StorageVolumeFormatter.format(0));
        assertEquals("0", StorageVolumeFormatter.format(-1));
        assertEquals("1", StorageVolumeFormatter.format(1000));
        assertEquals("1.001", StorageVolumeFormatter.format(1001));
        assertEquals("1.5", StorageVolumeFormatter.format(1500));
        assertEquals("16", StorageVolumeFormatter.format(16000));
        assertEquals("2147483.647", StorageVolumeFormatter.format(Integer.MAX_VALUE));
    }

    @Test void explanatoryTextAddsBucketUnitWhileTheCompactFormDoesNot() {
        assertEquals(".500", StorageVolumeFormatter.format(500));
        assertEquals(".500 B", StorageVolumeFormatter.formatWithUnit(500));
        assertEquals("16 B", StorageVolumeFormatter.formatWithUnit(16000));
    }
}
