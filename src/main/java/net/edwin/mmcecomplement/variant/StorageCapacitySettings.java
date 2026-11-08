package net.edwin.mmcecomplement.variant;

import net.minecraft.nbt.NBTTagCompound;

/** Per-controller overrides. Machine definitions remain shared and immutable. */
public final class StorageCapacitySettings {
    public static final int MAX_SLOTS = 4096;
    private NBTTagCompound values = new NBTTagCompound();

    public int slots(String type, int fallback) {
        int count = values.getInteger(type + "Slots");
        return count > 0 && count <= MAX_SLOTS ? count : fallback;
    }

    public int capacity(String type, int fallback) {
        int amount = values.getInteger(type + "Capacity");
        return amount > 0 ? amount : fallback;
    }

    public int capacity(String type, int slot, int fallback) {
        int amount = values.getCompoundTag(type + "Capacities").getInteger(Integer.toString(slot));
        return amount > 0 ? amount : capacity(type, fallback);
    }

    public void setSlots(String type, int count) {
        values.setInteger(type + "Slots", count);
        NBTTagCompound capacities = values.getCompoundTag(type + "Capacities");
        for (String key : new java.util.HashSet<>(capacities.getKeySet())) {
            try { if (Integer.parseInt(key) >= count) capacities.removeTag(key); }
            catch (NumberFormatException invalid) { capacities.removeTag(key); }
        }
        if (capacities.isEmpty()) values.removeTag(type + "Capacities");
    }

    public void setCapacity(String type, int amount) {
        values.setInteger(type + "Capacity", amount);
        values.removeTag(type + "Capacities");
    }

    public void setCapacity(String type, int slot, int amount) {
        String key = type + "Capacities";
        if (!values.hasKey(key, 10)) values.setTag(key, new NBTTagCompound());
        values.getCompoundTag(key).setInteger(Integer.toString(slot), amount);
    }

    public NBTTagCompound serializeNBT() { return values.copy(); }
    public void deserializeNBT(NBTTagCompound tag) { values = tag.copy(); }
}
