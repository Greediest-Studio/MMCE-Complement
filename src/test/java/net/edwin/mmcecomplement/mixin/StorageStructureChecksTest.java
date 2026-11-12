package net.edwin.mmcecomplement.mixin;

import net.edwin.mmcecomplement.variant.StorageMachine;
import net.edwin.mmcecomplement.variant.StorageStructureChecks;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class StorageStructureChecksTest {
    @Test
    void helperIsOutsideTheMixinOwnedPackage() {
        assertFalse(StorageStructureChecks.class.getName()
            .startsWith("net.edwin.mmcecomplement.mixin."));
    }

    @Test
    void limitsBlockTypeFallbackToConfiguredStorageVariants() {
        StorageMachine ordinaryMachine = machine(false);
        StorageMachine storageMachine = machine(true);

        assertFalse(StorageStructureChecks.isStorageVariant(new Object()));
        assertFalse(StorageStructureChecks.isStorageVariant(ordinaryMachine));
        assertTrue(StorageStructureChecks.isStorageVariant(storageMachine));
    }

    private static StorageMachine machine(boolean storageVariant) {
        return (StorageMachine) Proxy.newProxyInstance(
            StorageMachine.class.getClassLoader(),
            new Class<?>[]{StorageMachine.class},
            (proxy, method, args) -> {
                if (method.getName().equals("mmceComplement$isStorageVariant")) {
                    return storageVariant;
                }
                Class<?> type = method.getReturnType();
                if (type == boolean.class) return false;
                if (type == int.class) return 0;
                return null;
            });
    }
}
