package net.edwin.mmcecomplement.flux;

import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.api.utils.NBTType;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void serverChunkLoadingPolicyGatesBothHatchTypes() {
        boolean previous = FluxConfig.enableChunkLoading;
        try {
            TileFluxInputHatch input = new TileFluxInputHatch();
            TileFluxOutputHatch output = new TileFluxOutputHatch();
            FluxConfig.enableChunkLoading = true;
            input.setChunkLoadingRequested(true);
            output.setChunkLoadingRequested(true);
            assertTrue(input.isForcedLoading());
            assertTrue(output.isForcedLoading());
            assertTrue(input.isChunkLoadingAllowed());
            assertTrue(output.isChunkLoadingAllowed());

            FluxConfig.enableChunkLoading = false;
            assertTrue(input.isChunkLoadingRequested());
            assertTrue(output.isChunkLoadingRequested());
            assertFalse(input.isForcedLoading());
            assertFalse(output.isForcedLoading());
            assertFalse(input.isChunkLoadingAllowed());
            assertFalse(output.isChunkLoadingAllowed());

            NBTTagCompound synchronizedState = new NBTTagCompound();
            input.writeCustomNBT(synchronizedState, NBTType.TILE_UPDATE);
            TileFluxOutputHatch clientView = new TileFluxOutputHatch();
            clientView.readCustomNBT(synchronizedState, NBTType.TILE_UPDATE);
            assertTrue(synchronizedState.getBoolean("chunkLoading"));
            assertFalse(synchronizedState.getBoolean("serverChunkLoadingAllowed"));
            assertTrue(clientView.isChunkLoadingRequested());
            assertFalse(clientView.isChunkLoadingAllowed());

            input.setChunkLoadingRequested(false);
            assertFalse(input.isForcedLoading());
        } finally {
            FluxConfig.enableChunkLoading = previous;
        }
    }

    private static void assertDefaults(TileFluxHatchBase hatch) {
        assertEquals(TileFluxHatchBase.DEFAULT_BUFFER_CAPACITY,
            hatch.getBufferCapacityRaw());
        assertEquals(TileFluxHatchBase.DEFAULT_TRANSFER_LIMIT,
            hatch.getRawLimit());
    }
}
