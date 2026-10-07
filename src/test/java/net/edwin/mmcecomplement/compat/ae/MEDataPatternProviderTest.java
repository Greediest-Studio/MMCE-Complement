package net.edwin.mmcecomplement.compat.ae;

import github.kasuminova.mmce.common.tile.MEPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEDataPatternProviderII;
import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import net.edwin.mmcecomplement.tile.PrioritySmartInterfaceProvider;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MEDataPatternProviderTest {

    @Test
    void retainsTheTierOneProviderAndPriorityContracts() {
        assertTrue(MEPatternProvider.class.isAssignableFrom(
            TileMEDataPatternProvider.class));
        assertTrue(PrioritySmartInterfaceProvider.class.isAssignableFrom(
            TileMEDataPatternProvider.DataPatternInterfaceProvider.class));
        assertTrue(PrioritySmartInterfaceProvider.class.isAssignableFrom(
            TileDataItemInputHatch.DataItemInterfaceProvider.class));
        assertEquals(36, TileMEDataPatternProvider.PATTERN_SLOTS);
        assertEquals("mmce_complement:me_data_pattern_provider",
            TileMEDataPatternProvider.REGISTRY_NAME.toString());
        assertTrue(MEPatternProvider.class.isAssignableFrom(
            TileMEDataPatternProviderII.class));
        assertEquals(144, TileMEDataPatternProviderII.PATTERN_SLOTS);
        assertEquals("mmce_complement:me_data_pattern_provider_ii",
            TileMEDataPatternProviderII.REGISTRY_NAME.toString());
        assertTrue(BlockMEPatternProvider.class.isAssignableFrom(
            BlockMEDataPatternProviderII.class));
    }

    @Test
    void exposesTheConfigurationAndGroupRoutingContract() throws Exception {
        Class<TileMEDataPatternProvider> tile =
            TileMEDataPatternProvider.class;
        Class<TileMEDataPatternProvider.DataPatternInterfaceProvider> data =
            TileMEDataPatternProvider.DataPatternInterfaceProvider.class;

        assertEquals(boolean.class, tile.getMethod("configurePattern",
            int.class, String.class, float.class).getReturnType());
        assertEquals(float.class, tile.getMethod("getPatternValue",
            int.class).getReturnType());
        assertEquals(String.class,
            tile.getMethod("getSelectedInterfaceType").getReturnType());
        assertEquals(data,
            tile.getMethod("getPrimaryDataProvider").getReturnType());
        assertEquals(data, tile.getMethod("getDataProviderForGroup",
            long.class).getReturnType());
        assertEquals(List.class,
            tile.getMethod("getAllDataProviders").getReturnType());
        assertEquals(long.class, data.getMethod("getGroupID")
            .getReturnType());
    }

}
