package net.edwin.mmcecomplement.redstoneinterface;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedstoneSignalLogicTest {

    @Test
    void aggregatesMaximumMinimumAndSum() {
        assertEquals(14, RedstoneSignalLogic.aggregate(
            Arrays.asList(3, 14, 8), RedstoneValueDefinition.OPERATOR_MAX));
        assertEquals(3, RedstoneSignalLogic.aggregate(
            Arrays.asList(3, 14, 8), RedstoneValueDefinition.OPERATOR_MIN));
        assertEquals(25, RedstoneSignalLogic.aggregate(
            Arrays.asList(3, 14, 8), RedstoneValueDefinition.OPERATOR_SUM));
    }

    @Test
    void emptyMinimumIsZeroRatherThanAnInitializerSentinel() {
        assertEquals(0, RedstoneSignalLogic.aggregate(
            Collections.emptyList(), RedstoneValueDefinition.OPERATOR_MIN));
    }

    @Test
    void sumIsNotClampedToVanillaSignalStrength() {
        assertEquals(45, RedstoneSignalLogic.aggregate(
            Arrays.asList(15, 15, 15), RedstoneValueDefinition.OPERATOR_SUM));
    }

    @Test
    void outputIsClampedToRedstoneRange() {
        assertEquals(0, RedstoneSignalLogic.clampOutput(-4));
        assertEquals(11, RedstoneSignalLogic.clampOutput(11));
        assertEquals(15, RedstoneSignalLogic.clampOutput(70));
    }
}
