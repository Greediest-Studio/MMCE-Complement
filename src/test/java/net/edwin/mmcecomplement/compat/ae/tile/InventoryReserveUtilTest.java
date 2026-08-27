package net.edwin.mmcecomplement.compat.ae.tile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryReserveUtilTest {

    @Test
    void extractsOnlyStockAboveReserve() {
        assertEquals(16L, InventoryReserveUtil.extractable(32L, 16L,
            Integer.MAX_VALUE));
        assertEquals(0L, InventoryReserveUtil.extractable(16L, 16L,
            Integer.MAX_VALUE));
        assertEquals(0L, InventoryReserveUtil.extractable(8L, 16L,
            Integer.MAX_VALUE));
    }

    @Test
    void alsoHonorsBufferRoom() {
        assertEquals(5L, InventoryReserveUtil.extractable(32L, 16L, 5L));
        assertEquals(32L, InventoryReserveUtil.extractable(32L, 0L,
            Integer.MAX_VALUE));
    }

    @Test
    void clampsInvalidInputsWithoutOverflow() {
        assertEquals(10L, InventoryReserveUtil.extractable(10L, -5L,
            Long.MAX_VALUE));
        assertEquals(0L, InventoryReserveUtil.extractable(Long.MAX_VALUE,
            Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(0L, InventoryReserveUtil.extractable(10L, 0L, -1L));
    }
}
