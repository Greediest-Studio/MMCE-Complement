package net.edwin.mmcecomplement.batch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProcessingLogicTest {

    @Test
    void subTickExampleProducesOneThousandOperations() {
        assertEquals(1000,
            BatchProcessingLogic.calculateFactor((float) 0.60D, 600,
                Integer.MAX_VALUE));
    }

    @Test
    void choosesLargestFactorWhoseTheoreticalDurationFits() {
        int factor = BatchProcessingLogic.calculateFactor(7.25D, 100, Integer.MAX_VALUE);
        assertEquals(13, factor);
        assertEquals(94.25D, 7.25D * factor, 0.0D);
    }

    @Test
    void doesNotUseTickRoundingToExceedConfiguredTime() {
        int factor = BatchProcessingLogic.calculateFactor(0.51D, 600, Integer.MAX_VALUE);
        assertEquals(1176, factor);
        assertTrue(0.51D * factor <= 600.0D);
    }

    @Test
    void settingNoGreaterThanSingleOperationKeepsOneOperation() {
        assertEquals(1,
            BatchProcessingLogic.calculateFactor(200.0D, 200, Integer.MAX_VALUE));
        assertEquals(1,
            BatchProcessingLogic.calculateFactor(200.0D, 100, Integer.MAX_VALUE));
    }

    @Test
    void respectsParallelismOverflowLimit() {
        assertEquals(3,
            BatchProcessingLogic.calculateFactor(0.01D, Integer.MAX_VALUE, 3));
        assertEquals(Integer.MAX_VALUE,
            BatchProcessingLogic.multiplyParallelismSaturated(Integer.MAX_VALUE, 2));
    }

    @Test
    void partialInputBatchReducesDurationFactor() {
        assertEquals(125,
            BatchProcessingLogic.factorForActualParallelism(500, 4, 1000));
        assertEquals(1,
            BatchProcessingLogic.factorForActualParallelism(4, 4, 1000));
    }

    @Test
    void disabledOrInvalidInputsDoNotBatch() {
        assertEquals(1,
            BatchProcessingLogic.calculateFactor(0.6D, 0, Integer.MAX_VALUE));
        assertEquals(1,
            BatchProcessingLogic.calculateFactor(Double.NaN, 600, Integer.MAX_VALUE));
    }
}
