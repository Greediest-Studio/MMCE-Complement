package net.edwin.mmcecomplement.variant;

/** Predicates shared by storage-specific controller structure checks. */
public final class StorageStructureChecks {
    private StorageStructureChecks() { }

    public static boolean isStorageVariant(Object machine) {
        return machine instanceof StorageMachine
            && ((StorageMachine) machine).mmceComplement$isStorageVariant();
    }
}
