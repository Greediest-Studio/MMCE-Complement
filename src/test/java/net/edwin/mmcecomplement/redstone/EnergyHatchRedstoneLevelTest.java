package net.edwin.mmcecomplement.redstone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyHatchRedstoneLevelTest {
    @Test
    void scalesEnergyToTheComparatorRange() {
        assertEquals(0, EnergyHatchRedstoneLevel.fromEnergy(0, 100));
        assertEquals(15, EnergyHatchRedstoneLevel.fromEnergy(100, 100));
        assertEquals(8, EnergyHatchRedstoneLevel.fromEnergy(50, 100));
    }

    @Test
    void clampsValuesOutsideTheConfiguredCapacity() {
        assertEquals(0, EnergyHatchRedstoneLevel.fromEnergy(-1, 100));
        assertEquals(15, EnergyHatchRedstoneLevel.fromEnergy(101, 100));
    }

    @Test
    void matchesMmceBehaviorWhenCapacityIsZero() {
        assertEquals(0, EnergyHatchRedstoneLevel.fromEnergy(0, 0));
        assertEquals(15, EnergyHatchRedstoneLevel.fromEnergy(1, 0));
    }
}
