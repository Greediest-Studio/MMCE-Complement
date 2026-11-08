package net.edwin.mmcecomplement.variant;

import net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge;
import net.minecraft.tileentity.TileEntity;

/** Safe changes to a live handler. Caller must already be on the server thread. */
public final class StorageResizing {
    private StorageResizing() { }

    public static boolean slots(StorageController storage, String type, int count) {
        if (storage == null || count < 1 || count > StorageCapacitySettings.MAX_SLOTS
            || storage.mmceComplement$getCapacitySettings() == null) return false;
        if ("Fluid".equals(type) && !storage.mmceComplement$hasFluidStorage()
            || "Gas".equals(type) && !storage.mmceComplement$hasGasStorage()) return false;
        if (storage.mmceComplement$getCapacitySettings().slots(type, -1) == count) return true;
        boolean result;
        switch (type) {
            case "Item":
                Object items = storage.mmceComplement$getStorageInventory();
                result = items instanceof StorageItemHandler && ((StorageItemHandler) items).resize(count);
                break;
            case "Fluid":
                if (!storage.mmceComplement$hasFluidStorage()) return false;
                Object fluids = storage.mmceComplement$getFluidStorage();
                result = fluids instanceof StorageFluidHandler && ((StorageFluidHandler) fluids).resize(count);
                break;
            case "Gas":
                if (!storage.mmceComplement$hasGasStorage()) return false;
                result = StorageGasBridge.resize(storage.mmceComplement$getGasStorage(), count);
                break;
            default: return false;
        }
        if (!result) return false;
        storage.mmceComplement$getCapacitySettings().setSlots(type, count);
        changed(storage);
        return true;
    }

    /** Slot -1 updates all current tanks and the default for future tanks. */
    public static boolean capacity(StorageController storage, String type, int slot, int amount) {
        if (storage == null || amount <= 0 || slot < -1 || storage.mmceComplement$getCapacitySettings() == null) return false;
        if ("Fluid".equals(type) ? !storage.mmceComplement$hasFluidStorage() || slot >= storage.mmceComplement$getFluidStorageSlots()
            : !"Gas".equals(type) || !storage.mmceComplement$hasGasStorage() || slot >= storage.mmceComplement$getGasStorageSlots()) return false;
        StorageCapacitySettings settings = storage.mmceComplement$getCapacitySettings();
        if (slot >= 0 && settings.capacity(type, slot, -1) == amount) return true;
        if (slot == -1 && settings.capacity(type, -1) == amount
            && !settings.serializeNBT().hasKey(type + "Capacities")) return true;
        boolean result;
        if ("Fluid".equals(type) && storage.mmceComplement$hasFluidStorage()) {
            Object handler = storage.mmceComplement$getFluidStorage();
            if (!(handler instanceof StorageFluidHandler)) return false;
            StorageFluidHandler fluids = (StorageFluidHandler) handler;
            result = slot == -1 ? fluids.setCapacity(amount) : fluids.setCapacityInSlot(slot, amount);
        } else if ("Gas".equals(type) && storage.mmceComplement$hasGasStorage()) {
            Object handler = storage.mmceComplement$getGasStorage();
            result = slot == -1 ? StorageGasBridge.setCapacity(handler, amount)
                : StorageGasBridge.setCapacityInSlot(handler, slot, amount);
        } else return false;
        if (!result) return false;
        if (slot == -1) storage.mmceComplement$getCapacitySettings().setCapacity(type, amount);
        else storage.mmceComplement$getCapacitySettings().setCapacity(type, slot, amount);
        changed(storage);
        return true;
    }

    private static void changed(StorageController storage) {
        storage.mmceComplement$markStorageChanged();
        if (storage instanceof TileEntity) ((TileEntity) storage).markDirty();
        if (storage instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized)
            ((hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized) storage).markForUpdateSync();
    }
}
