package net.edwin.mmcecomplement.compat.ae;

import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MEPatternProviderIICompatibilityTest {

    @Test
    void retainsMmcePatternProviderTypeContracts() throws Exception {
        assertTrue(MEPatternProvider.class
            .isAssignableFrom(TileMEPatternProviderII.class));
        assertTrue(BlockMEPatternProvider.class
            .isAssignableFrom(BlockMEPatternProviderII.class));

        // MMCE's mirror dispatches these methods virtually. Provider II must
        // override them so the mirror reaches the expanded stores/components.
        assertEquals(TileMEPatternProviderII.class,
            TileMEPatternProviderII.class.getMethod("provideComponent")
                .getDeclaringClass());
        assertEquals(TileMEPatternProviderII.class,
            TileMEPatternProviderII.class.getMethod("provideComponents")
                .getDeclaringClass());
    }

    @Test
    void exposesWhimcraftInventorySharingContract() throws Exception {
        // Whimcraft 0.1.4 checks instanceof MEPatternProvider, then invokes
        // getInfHandler(). The covariant runtime target must be Provider II.
        assertEquals(InfItemFluidHandler.class,
            TileMEPatternProviderII.class.getMethod("getInfHandler")
                .getReturnType());
        assertEquals(TileMEPatternProviderII.class,
            TileMEPatternProviderII.class.getMethod("getInfHandler")
                .getDeclaringClass());
        assertEquals("whimcraft:link_card",
            BlockMEPatternProviderII.WHIMCRAFT_LINK_CARD.toString());
    }

    @Test
    void memoryCardUsesOriginalMmceMirrorIdentifier() {
        assertEquals("tile.modularmachinery.blockmepatternprovider",
            BlockMEPatternProviderII.MEMORY_CARD_PROVIDER_TYPE);
        assertEquals("tooltip.groupinput.block",
            BlockMEPatternProviderII.GROUP_INPUT_TOOLTIP);
    }
}
