package net.edwin.mmcecomplement.compat.ae.tile;

import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

/** Builds and normalizes type-only item marker inventories. */
final class InventoryMarkerUtil {

    private static final int ITEM_MARKER_SLOTS = 16;

    private InventoryMarkerUtil() { }

    static IOInventory buildItemMarkers(MEItemInputBus owner) {
        int[] slots = new int[ITEM_MARKER_SLOTS];
        for (int slot = 0; slot < slots.length; slot++) slots[slot] = slot;

        IOInventory markers = new IOInventory(owner, new int[0], new int[0]);
        markers.setStackLimit(1, slots);
        markers.setMiscSlots(slots);
        AtomicBoolean normalizing = new AtomicBoolean();
        markers.setListener(slot -> {
            if (normalizing.get()) return;
            ItemStack marker = markers.getStackInSlot(slot);
            if (!marker.isEmpty() && marker.getCount() != 1) {
                normalizing.set(true);
                try {
                    ItemStack normalized = marker.copy();
                    normalized.setCount(1);
                    markers.setStackInSlot(slot, normalized);
                } finally {
                    normalizing.set(false);
                }
            }
            owner.markNoUpdate();
        });
        return markers;
    }

    static void normalizeItemMarkers(IOInventory markers) {
        for (int slot = 0; slot < markers.getSlots(); slot++) {
            ItemStack marker = markers.getStackInSlot(slot);
            if (!marker.isEmpty() && marker.getCount() != 1) {
                ItemStack normalized = marker.copy();
                normalized.setCount(1);
                markers.setStackInSlot(slot, normalized);
            }
        }
    }
}
