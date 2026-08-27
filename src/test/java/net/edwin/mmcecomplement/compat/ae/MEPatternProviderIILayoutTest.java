package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MEPatternProviderIILayoutTest {

    @Test
    void expandedCapacitiesMatchRequestedLayout() {
        assertEquals(18, TileMEPatternProviderII.PATTERN_COLUMNS);
        assertEquals(8, TileMEPatternProviderII.PATTERN_ROWS);
        assertEquals(144, TileMEPatternProviderII.PATTERN_SLOTS);
        assertEquals(9, TileMEPatternProviderII.SUB_ITEM_SLOTS);
        assertEquals(3, TileMEPatternProviderII.SUB_FLUID_SLOTS);
    }

    @Test
    void allSlotsFitInsideExpandedGui() {
        int lastPatternRight = 8
            + (TileMEPatternProviderII.PATTERN_COLUMNS - 1) * 18 + 16;
        int lastPatternBottom = 28
            + (TileMEPatternProviderII.PATTERN_ROWS - 1) * 18 + 16;
        int lastItemRight = ContainerMEPatternProviderII.SUB_STORAGE_X
            + 2 * 18 + 16;
        int lastFluidRight = GuiMEPatternProviderII.RIGHT_PANEL_X + 61 + 16;
        int lastStorageBottom = ContainerMEPatternProviderII.SUB_STORAGE_Y
            + 2 * 18 + 16;

        assertTrue(lastPatternRight < GuiMEPatternProviderII.RIGHT_PANEL_X);
        assertTrue(lastPatternBottom < ContainerMEPatternProviderII.PLAYER_INVENTORY_Y);
        assertTrue(lastItemRight < lastFluidRight);
        assertTrue(lastFluidRight <= GuiMEPatternProviderII.GUI_WIDTH);
        assertTrue(lastStorageBottom <= GuiMEPatternProviderII.GUI_HEIGHT);
    }

    @Test
    void generatedTexturesHaveNativeRequestedDimensions() throws IOException {
        BufferedImage gui = readImage(
            "/assets/mmce_complement/textures/gui/me_pattern_provider_ii.png");
        BufferedImage overlay = readImage(
            "/assets/mmce_complement/textures/blocks/overlay_me_pattern_provider_ii.png");

        assertEquals(418, gui.getWidth());
        assertEquals(268, gui.getHeight());
        assertEquals(16, overlay.getWidth());
        assertEquals(16, overlay.getHeight());
    }

    @Test
    void independentFluidSlotsCarryNativeFMarkers() throws IOException {
        BufferedImage gui = readImage(
            "/assets/mmce_complement/textures/gui/me_pattern_provider_ii.png");
        int markerColor = 0xFFC8C8C8;

        for (int slot = 0; slot < TileMEPatternProviderII.SUB_FLUID_SLOTS;
             slot++) {
            int markerX = 408;
            int markerY = 218 + slot * 18;
            for (int x = 0; x < 4; x++) {
                assertEquals(markerColor, gui.getRGB(markerX + x, markerY));
            }
            for (int y = 0; y < 5; y++) {
                assertEquals(markerColor, gui.getRGB(markerX, markerY + y));
            }
            for (int x = 0; x < 3; x++) {
                assertEquals(markerColor,
                    gui.getRGB(markerX + x, markerY + 2));
            }
        }
    }

    @Test
    void overlayKeepsCreamCenterSeparatedByRecessedCross()
        throws IOException {
        BufferedImage overlay = readImage(
            "/assets/mmce_complement/textures/blocks/overlay_me_pattern_provider_ii.png");

        int creamQuadrant = overlay.getRGB(6, 6);
        assertEquals(0xFFEEDBB9, creamQuadrant);
        assertEquals(0xFF595959, overlay.getRGB(7, 6));
        assertEquals(0xFF666666, overlay.getRGB(8, 6));
        assertEquals(0xFF595959, overlay.getRGB(6, 7));
        assertEquals(0xFF666666, overlay.getRGB(6, 8));
        assertNotEquals(creamQuadrant, overlay.getRGB(7, 7));
    }

    private static BufferedImage readImage(String path) throws IOException {
        try (InputStream stream = MEPatternProviderIILayoutTest.class
            .getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return ImageIO.read(stream);
        }
    }
}
