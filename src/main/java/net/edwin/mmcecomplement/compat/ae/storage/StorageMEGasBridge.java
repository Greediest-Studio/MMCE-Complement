package net.edwin.mmcecomplement.compat.ae.storage;

import net.edwin.mmcecomplement.variant.StorageController;
import java.util.function.Supplier;

/** Do not resolve MekEng's optional channel and stack classes without the mod. */
public final class StorageMEGasBridge {
    private StorageMEGasBridge() { }
    public static StorageMEInventory<?> create(Supplier<StorageController> storage) {
        try {
            return (StorageMEInventory<?>) Class.forName(
                "net.edwin.mmcecomplement.compat.mekeng.StorageMEGasInventory")
                .getMethod("create", Supplier.class).invoke(null, storage);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create AE storage gas inventory", e);
        }
    }
}
