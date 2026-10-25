package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import java.util.function.Supplier;

public final class StorageMEItemInventory extends StorageMEInventory<IAEItemStack> {
    private final Supplier<IItemHandler> inventory;
    public StorageMEItemInventory(IStorageChannel<IAEItemStack> channel, Supplier<IItemHandler> inventory) {
        super(channel);
        this.inventory = inventory;
    }
    @Override public IAEItemStack injectItems(IAEItemStack input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) return input;
        IItemHandler handler = inventory.get();
        if (handler == null) return input;
        long remaining = input.getStackSize();
        // Match occupied slots first, then use empty ones, like the controller GUI.
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack stored = handler.getStackInSlot(slot);
                if (pass == 0 ? stored.isEmpty() || !input.isSameType(stored) : !stored.isEmpty()) continue;
                ItemStack offered = input.createItemStack();
                offered.setCount((int) Math.min(remaining, Integer.MAX_VALUE));
                ItemStack rest = handler.insertItem(slot, offered, mode == Actionable.SIMULATE);
                remaining -= offered.getCount() - rest.getCount();
            }
        }
        if (mode == Actionable.MODULATE) networkChange(input, input.getStackSize() - remaining);
        return remaining == 0 ? null : input.copy().setStackSize(remaining);
    }
    @Override public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) return null;
        IItemHandler handler = inventory.get();
        if (handler == null) return null;
        long remaining = request.getStackSize();
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            if (!request.isSameType(handler.getStackInSlot(slot))) continue;
            remaining -= handler.extractItem(slot, (int) Math.min(remaining, Integer.MAX_VALUE),
                mode == Actionable.SIMULATE).getCount();
        }
        long extracted = request.getStackSize() - remaining;
        if (extracted == 0) return null;
        if (mode == Actionable.MODULATE) networkChange(request, -extracted);
        return request.copy().setStackSize(extracted);
    }
    @Override public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        IItemHandler handler = inventory.get();
        if (handler != null) for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(getChannel().createStack(stack));
        }
        return out;
    }
}
