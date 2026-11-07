package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.storage.MEStorage;
import net.edwin.mmcecomplement.variant.StorageController;
import java.util.function.Supplier;

/** Keeps Mekanism and Applied Mekanistics optional for the native storage port. */
final class NativeGasStorageBridge {
    private NativeGasStorageBridge() { }

    static MEStorage create(Supplier<StorageController> storage) {
        ClassLoader loader = NativeGasStorageBridge.class.getClassLoader();
        try {
            Class.forName("me.ramidzkh.mekae2.ae2.AEGasKey", false, loader);
        } catch (ClassNotFoundException | LinkageError unavailable) {
            return null;
        }
        try {
            return (MEStorage) Class.forName(
                "net.edwin.mmcecomplement.compat.ae2s.mekeng.ControllerGasStorage", true, loader)
                .getConstructor(Supplier.class).newInstance(storage);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot initialize native AE2S gas storage", e);
        }
    }
}
