package net.edwin.mmcecomplement.compat.ae;

import appeng.api.config.Actionable;
import appeng.api.definitions.IItemDefinition;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public final class AEEnergyBusTransferUtil {

    private AEEnergyBusTransferUtil() {}

    @Nullable
    public static IAEItemStack createAEStack(IItemDefinition definition, long amount) {
        if (definition == null || amount < 0L) {
            return null;
        }

        ItemStack stack = definition.maybeStack(1).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) {
            return null;
        }
        return AEItemStack.fromItemStack(stack).setStackSize(amount);
    }

    @Nullable
    public static <T extends IAEStack<T>> T extractFromME(IMEInventory<T> inventory, T request, IActionSource source, Actionable mode) {
        if (inventory == null || request == null || source == null || mode == null) {
            return null;
        }

        T possible = inventory.extractItems(request.copy(), Actionable.SIMULATE, source);
        long retrieved = possible == null ? 0L : possible.getStackSize();
        if (retrieved <= 0L) {
            return null;
        }

        if (mode == Actionable.MODULATE) {
            possible.setStackSize(retrieved);
            return inventory.extractItems(possible, Actionable.MODULATE, source);
        }

        return possible.setStackSize(retrieved);
    }

    @Nullable
    public static <T extends IAEStack<T>> T injectToME(IMEInventory<T> inventory, T input, IActionSource source, Actionable mode) {
        if (inventory == null || input == null || source == null || mode == null) {
            return input;
        }

        T possible = inventory.injectItems(input, Actionable.SIMULATE, source);
        long stored = input.getStackSize();
        if (possible != null) {
            stored -= possible.getStackSize();
        }

        if (stored <= 0L) {
            return input;
        }

        if (mode == Actionable.MODULATE) {
            if (stored < input.getStackSize()) {
                T leftover = input.copy();
                T split = input.copy();
                leftover.decStackSize(stored);
                split.setStackSize(stored);
                leftover.add(inventory.injectItems(split, Actionable.MODULATE, source));
                return leftover;
            }
            return inventory.injectItems(input, Actionable.MODULATE, source);
        }

        T transferred = input.copy();
        transferred.setStackSize(input.getStackSize() - stored);
        return transferred != null && transferred.getStackSize() > 0L ? transferred : null;
    }
}