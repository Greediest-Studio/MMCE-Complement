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
    void batchMultiplierLeavesExcludedThreadsUntouched() {
        assertEquals(2002,
            BatchProcessingLogic.multiplyParallelismExcluding(4, 2, 1000));
        assertEquals(4,
            BatchProcessingLogic.multiplyParallelismExcluding(4, 2, 1));
    }

    @Test
    void restoresOnlyAnUnchangedPreviousBatchResult() {
        assertEquals(8, BatchProcessingLogic.restoreUnbatchedParallelism(
            62, 8, 62, true, false));

        // A direct field change by another mixin is authoritative even when
        // it did not pass through ActiveMachineRecipe#setMaxParallelism.
        assertEquals(37, BatchProcessingLogic.restoreUnbatchedParallelism(
            37, 8, 62, true, false));

        // An explicit setter call is authoritative even if it happens to set
        // the same integer as the previous batch result.
        assertEquals(62, BatchProcessingLogic.restoreUnbatchedParallelism(
            62, 8, 62, true, true));
    }

    @Test
    void batchScalingUsesTheRuntimeOverrideAsItsBase() {
        int runtimeOverride = BatchProcessingLogic.restoreUnbatchedParallelism(
            37, 8, 62, true, true);
        assertEquals(352, BatchProcessingLogic.multiplyParallelismExcluding(
            runtimeOverride, 2, 10));
    }

    @Test
    void excludedThreadsAreClampedToTheAvailableBudget() {
        assertEquals(4,
            BatchProcessingLogic.multiplyParallelismExcluding(4, 99, 1000));
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
