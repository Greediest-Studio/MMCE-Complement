package net.edwin.mmcecomplement.compat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDisplayNameSideSafetyTest {

    @Test
    void commonItemNamesUseServerSafeTranslation() throws IOException {
        assertServerSafe("net/edwin/mmcecomplement/item/ItemBlockMachineSignalCable.class");
        assertServerSafe("net/edwin/mmcecomplement/compat/ae/item/ItemBlockMEDataPatternProvider.class");
    }

    private static void assertServerSafe(String resource) throws IOException {
        try (InputStream input = ItemDisplayNameSideSafetyTest.class
                .getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            String constants = new String(output.toByteArray(),
                StandardCharsets.ISO_8859_1);
            assertFalse(constants.contains("net/minecraft/client/"), resource);
            assertTrue(constants.contains("net/minecraft/util/text/translation/I18n"), resource);
            assertTrue(constants.contains("translateToLocal"), resource);
        }
    }
}
