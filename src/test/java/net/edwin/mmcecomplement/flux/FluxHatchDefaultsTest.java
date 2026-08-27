package net.edwin.mmcecomplement.flux;

import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluxHatchDefaultsTest {

    @Test
    void bothHatchesUseTheRequestedDefaults() {
        assertDefaults(new TileFluxInputHatch());
        assertDefaults(new TileFluxOutputHatch());
    }

    @Test
    void missingLegacyFieldsUseTheRequestedDefaults() {
        TileFluxInputHatch hatch = new TileFluxInputHatch();
        long configuredCeiling = hatch.getTier().maxEnergy;
        // EnergyHatchData is normally populated by MMCE's config lifecycle;
        // provide the normal positive ceiling for this isolated unit test.
        hatch.getTier().maxEnergy = 524_288L;
        try {
            hatch.readCustomNBT(new NBTTagCompound());
        } finally {
            hatch.getTier().maxEnergy = configuredCeiling;
        }

        assertEquals(TileFluxHatchBase.DEFAULT_BUFFER_CAPACITY,
            hatch.getBufferCapacityRaw());
        assertEquals(TileFluxHatchBase.DEFAULT_TRANSFER_LIMIT,
            hatch.getRawLimit());
    }

    private static void assertDefaults(TileFluxHatchBase hatch) {
        assertEquals(TileFluxHatchBase.DEFAULT_BUFFER_CAPACITY,
            hatch.getBufferCapacityRaw());
        assertEquals(TileFluxHatchBase.DEFAULT_TRANSFER_LIMIT,
            hatch.getRawLimit());
    }
}
