package net.edwin.mmcecomplement.compat.ae;

import github.kasuminova.mmce.common.block.appeng.BlockMEPatternProvider;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEDataPatternProvider;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MEDataPatternProviderResourceTest {

    @Test
    void blockKeepsTheTierOneProviderCompatibilityProtocols() {
        assertTrue(BlockMEPatternProvider.class.isAssignableFrom(
            BlockMEDataPatternProvider.class));
        assertEquals("tile.modularmachinery.blockmepatternprovider",
            BlockMEDataPatternProvider.MEMORY_CARD_PROVIDER_TYPE);
        assertEquals("whimcraft:link_card",
            BlockMEDataPatternProvider.WHIMCRAFT_LINK_CARD.toString());
    }

    @Test
    void modelLayersTheAnimatedDataInterfaceOverTheOriginalProvider()
        throws IOException {
        String model = readText(
            "/assets/mmce_complement/models/block/"
                + "me_data_pattern_provider.json");
        assertTrue(model.contains(
            "mmce_complement:block/blockmodel_double_overlay_all"));
        assertTrue(model.contains(
            "modularmachinery:blocks/overlay_mepatternprovider"));
        assertTrue(model.contains(
            "mmce_complement:blocks/overlay_smartinterface_number"));

        BufferedImage overlay = readImage(
            "/assets/mmce_complement/textures/blocks/"
                + "overlay_smartinterface_number.png");
        assertEquals(16, overlay.getWidth());
        assertEquals(400, overlay.getHeight());
        assertEquals(25, overlay.getHeight() / overlay.getWidth());

        String metadata = readText(
            "/assets/mmce_complement/textures/blocks/"
                + "overlay_smartinterface_number.png.mcmeta");
        assertTrue(metadata.contains("\"frametime\": 2"));

        String expandedModel = readText(
            "/assets/mmce_complement/models/block/"
                + "me_data_pattern_provider_ii.json");
        assertTrue(expandedModel.contains(
            "overlay_me_pattern_provider_ii"));
        assertTrue(expandedModel.contains(
            "overlay_smartinterface_number"));

        String zh = readText(
            "/assets/mmce_complement/lang/zh_cn.lang");
        String en = readText(
            "/assets/mmce_complement/lang/en_us.lang");
        assertTrue(zh.contains(
            "tile.mmce_complement.me_data_pattern_provider.name="));
        assertTrue(zh.contains(
            "tile.mmce_complement.me_data_pattern_provider_ii.name="));
        assertTrue(en.contains(
            "tile.mmce_complement.me_data_pattern_provider.name="));
        assertTrue(en.contains(
            "tile.mmce_complement.me_data_pattern_provider_ii.name="));
        assertTrue(zh.contains(
            "tile.mmce_complement.me_data_pattern_provider=ME数据机械样板供应器"));
        assertTrue(zh.contains(
            "tile.mmce_complement.me_data_pattern_provider_ii=ME数据机械样板供应器 II"));
        assertTrue(en.contains(
            "tile.mmce_complement.me_data_pattern_provider=ME Data Pattern Provider"));
        assertTrue(en.contains(
            "tile.mmce_complement.me_data_pattern_provider_ii=ME Data Pattern Provider II"));
    }

    private static BufferedImage readImage(String path) throws IOException {
        try (InputStream stream = MEDataPatternProviderResourceTest.class
            .getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return ImageIO.read(stream);
        }
    }

    private static String readText(String path) throws IOException {
        try (InputStream stream = MEDataPatternProviderResourceTest.class
            .getResourceAsStream(path)) {
            assertNotNull(stream, path);
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
