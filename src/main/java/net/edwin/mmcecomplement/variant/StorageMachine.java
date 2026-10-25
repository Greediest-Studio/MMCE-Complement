package net.edwin.mmcecomplement.variant;

import com.google.gson.JsonObject;

/** Marker and configuration exposed by a storage variant machine definition. */
public interface StorageMachine {
    boolean mmceComplement$isStorageVariant();

    int mmceComplement$getStorageSlots();

    boolean mmceComplement$hasFluidStorage();
    int mmceComplement$getFluidStorageSlots();
    int mmceComplement$getFluidStorageCapacity();

    boolean mmceComplement$hasGasStorage();
    int mmceComplement$getGasStorageSlots();
    int mmceComplement$getGasStorageCapacity();

    void mmceComplement$configureStorageVariant(JsonObject root);
}
