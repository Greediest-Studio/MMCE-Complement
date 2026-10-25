package net.edwin.mmcecomplement.variant;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Inventory access exposed by a storage variant controller tile. */
public interface StorageController {
    boolean mmceComplement$isStorageController();

    int mmceComplement$getStorageSlots();

    ItemStackHandler mmceComplement$getStorageInventory();

    boolean mmceComplement$hasFluidStorage();
    int mmceComplement$getFluidStorageSlots();
    int mmceComplement$getFluidStorageCapacity();
    IFluidHandler mmceComplement$getFluidStorage();
    IFluidHandler mmceComplement$getFluidTank(int index);
    boolean mmceComplement$hasGasStorage();
    int mmceComplement$getGasStorageSlots();
    int mmceComplement$getGasStorageCapacity();
    Object mmceComplement$getGasStorage();
}
