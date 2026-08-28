package net.edwin.mmcecomplement.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionalDependencyClassLoadingTest {

    @Test
    void commonEntrypointsLoadWithoutCrazyAeOrMekEng() {
        ClassLoader loader = getClass().getClassLoader();
        assertMissing(loader, "dev.beecube31.crazyae2.core.CrazyAE");
        assertMissing(loader,
            "com.mekeng.github.common.me.storage.IGasStorageChannel");

        assertLoadable(loader, "net.edwin.mmcecomplement.MMCEComplement");
        assertLoadable(loader,
            "net.edwin.mmcecomplement.event.RegistryEvents");
        assertLoadable(loader,
            "net.edwin.mmcecomplement.gui.GuiHandlerMMCE");
        assertLoadable(loader,
            "net.edwin.mmcecomplement.network.NetworkHandlerMMCE");
        assertLoadable(loader, "net.edwin.mmcecomplement.init.ModBlocks");
        assertLoadable(loader,
            "net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII");
    }

    @Test
    void commonEntrypointsContainNoDirectOptionalApiReferences()
            throws IOException {
        assertNoOptionalSymbols("net.edwin.mmcecomplement.MMCEComplement");
        assertNoOptionalSymbols(
            "net.edwin.mmcecomplement.event.RegistryEvents");
        assertNoOptionalSymbols(
            "net.edwin.mmcecomplement.gui.GuiHandlerMMCE");
        assertNoOptionalSymbols(
            "net.edwin.mmcecomplement.network.NetworkHandlerMMCE");
        assertNoOptionalSymbols("net.edwin.mmcecomplement.init.ModBlocks");
        assertNoOptionalSymbols(
            "net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII");
    }

    private static void assertLoadable(ClassLoader loader, String className) {
        assertDoesNotThrow(() -> Class.forName(className, false, loader));
    }

    private static void assertMissing(ClassLoader loader, String className) {
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName(className, false, loader));
    }

    private static void assertNoOptionalSymbols(String className)
            throws IOException {
        String resource = className.replace('.', '/') + ".class";
        ClassLoader loader = OptionalDependencyClassLoadingTest.class
            .getClassLoader();
        try (InputStream input = loader.getResourceAsStream(resource)) {
            byte[] bytes = new byte[32768];
            int length = 0;
            int read;
            while ((read = input.read(bytes, length, bytes.length - length))
                    >= 0) {
                length += read;
                if (length == bytes.length) break;
            }
            String constants = new String(bytes, 0, length,
                StandardCharsets.ISO_8859_1);
            assertFalse(constants.contains("sonar/fluxnetworks"), className);
            assertFalse(constants.contains("dev/beecube31"), className);
            assertFalse(constants.contains("com/mekeng"), className);
            assertFalse(constants.contains("mekanism/api"), className);
            assertFalse(constants.contains("kport/modularmagic"), className);
        }
    }
}
