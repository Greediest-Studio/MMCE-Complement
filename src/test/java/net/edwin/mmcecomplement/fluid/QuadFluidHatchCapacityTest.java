package net.edwin.mmcecomplement.fluid;

import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidInputHatch;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuadFluidHatchCapacityTest {

    @Test
    void dividesTinyCapacityIntoFourTanksWithCeiling() {
        assertEquals(25, TileQuadFluidInputHatch.capacityForTotal(100));
        assertEquals(26, TileQuadFluidInputHatch.capacityForTotal(101));
        assertEquals(1, TileQuadFluidInputHatch.capacityForTotal(1));
    }

    @Test
    void remainsSafeAtConfigurationBounds() {
        assertEquals(1, TileQuadFluidInputHatch.capacityForTotal(0));
        assertEquals(536870912,
            TileQuadFluidInputHatch.capacityForTotal(Integer.MAX_VALUE));
    }

    @Test
    void dividesNormalCapacityIntoNineTanksWithCeiling() {
        assertEquals(112, TileNineFluidInputHatch.capacityForTotal(1000));
        assertEquals(1, TileNineFluidInputHatch.capacityForTotal(1));
        assertEquals(238609295,
            TileNineFluidInputHatch.capacityForTotal(Integer.MAX_VALUE));
    }

    @Test
    void routesFourDifferentMaterialsAndRejectsAFifth() {
        boolean[] occupied = {true, true, true, true};
        boolean[] noMatch = {false, false, false, false};
        assertEquals(-1, QuadTankRouting.findFillTarget(occupied, noMatch));

        for (int expected = 0; expected < 4; expected++) {
            boolean[] progressivelyOccupied = {
                expected > 0, expected > 1, expected > 2, expected > 3
            };
            assertEquals(expected,
                QuadTankRouting.findFillTarget(progressivelyOccupied, noMatch));
        }
    }

    @Test
    void matchingMaterialAlwaysKeepsItsOriginalTank() {
        boolean[] occupied = {true, false, false, false};
        boolean[] matching = {true, false, false, false};
        assertEquals(0, QuadTankRouting.findFillTarget(occupied, matching));
    }

    @Test
    void outputUsesRoomyMatchBeforeAllocatingDuplicate() {
        boolean[] occupied = {true, true, false, false};
        boolean[] matching = {true, true, false, false};
        boolean[] hasRoom = {false, true, true, true};
        assertEquals(1,
            QuadTankRouting.findOutputFillTarget(occupied, matching, hasRoom));
    }

    @Test
    void outputAllocatesEmptySlotWhenAllMatchingTanksAreFull() {
        boolean[] occupied = {true, true, false, false};
        boolean[] matching = {true, true, false, false};
        boolean[] hasRoom = {false, false, true, true};
        assertEquals(2,
            QuadTankRouting.findOutputFillTarget(occupied, matching, hasRoom));
    }
}
