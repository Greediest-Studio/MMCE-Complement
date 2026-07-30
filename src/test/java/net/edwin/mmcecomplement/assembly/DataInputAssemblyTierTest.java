package net.edwin.mmcecomplement.assembly;

import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DataInputAssemblyTierTest {

    @Test
    void exposesTheConfiguredTierCapacities() {
        int[] itemSlots = {2, 6, 12, 20, 30};
        int[] fluidTanks = {1, 2, 4, 6, 9};
        int[] tankCapacities = {1_000, 8_000, 64_000, 512_000, 4_096_000};

        DataInputAssemblyTier[] tiers = DataInputAssemblyTier.values();
        assertEquals(5, tiers.length);
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(itemSlots[i], tiers[i].getItemSlots());
            assertEquals(fluidTanks[i], tiers[i].getFluidTanks());
            assertEquals(tankCapacities[i], tiers[i].getPerTankCapacity());
            assertSame(tiers[i], DataInputAssemblyTier.fromMeta(
                tiers[i].getMetadata()));
        }
        assertSame(DataInputAssemblyTier.NORMAL,
            DataInputAssemblyTier.fromMeta(0));
    }

    @Test
    void tileAllocatesTheExactInventoryAndTankCounts() {
        for (DataInputAssemblyTier tier : DataInputAssemblyTier.values()) {
            TileDataItemInputHatch tile = new TileDataItemInputHatch(tier);
            assertSame(tier, tile.getTier());
            assertEquals(tier.getItemSlots(), tile.getInventory().getSlots());
            assertEquals(tier.getFluidTanks(), tile.getTankCount());
            assertEquals(tier.getPerTankCapacity(), tile.getPerTankCapacity());
        }
    }
}
