package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.networking.storage.IStorageService;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import github.formlessdragon.appcompat.bridge.gtceu.GtceuActionSourceBridge;
import net.edwin.mmcecomplement.compat.ae.storage.StorageMEInventory;

/** Per-operation view for Complement's pull/reserve code, including optional gases. */
public final class AE2SInventoryMonitor<T extends IAEStack<T>> extends StorageMEInventory<T> implements IMEMonitor<T> {
    private final IStorageService storage;
    public AE2SInventoryMonitor(IStorageChannel<T> channel, IStorageService storage) {
        super(channel);
        this.storage = storage;
    }
    @Override public T injectItems(T stack, Actionable mode, IActionSource source) {
        if (stack == null || stack.getStackSize() <= 0) return null;
        long inserted = storage.getInventory().insert(AE2SKeyBridge.toKey(stack), stack.getStackSize(), action(mode),
            GtceuActionSourceBridge.toNew(source));
        return inserted == stack.getStackSize() ? null : stack.copy().setStackSize(stack.getStackSize() - inserted);
    }
    @Override public T extractItems(T stack, Actionable mode, IActionSource source) {
        if (stack == null || stack.getStackSize() <= 0) return null;
        long extracted = storage.getInventory().extract(AE2SKeyBridge.toKey(stack), stack.getStackSize(), action(mode),
            GtceuActionSourceBridge.toNew(source));
        return extracted == 0 ? null : stack.copy().setStackSize(extracted);
    }
    @Override public IItemList<T> getStorageList() { return getAvailableItems(getChannel().createList()); }
    @Override @SuppressWarnings("unchecked") public IItemList<T> getAvailableItems(IItemList<T> out) {
        storage.getCachedInventory().forEach((key, amount) -> {
            if (amount > 0 && AE2SKeyBridge.matchesChannel(getChannel(), key))
                out.addStorage((T) AE2SKeyBridge.toOldStack(key, amount));
        });
        return out;
    }
    // This short-lived view is only used synchronously by Complement. Native
    // mounted inventories use AE2SStorageProvider's real listener lifecycle.
    @Override public void addListener(IMEMonitorHandlerReceiver<T> listener, Object token) {
        throw new UnsupportedOperationException("Use the native AE2S storage service for subscriptions");
    }
    @Override public void removeListener(IMEMonitorHandlerReceiver<T> listener) {}
    private static ae2.api.config.Actionable action(Actionable mode) {
        return mode == Actionable.SIMULATE ? ae2.api.config.Actionable.SIMULATE : ae2.api.config.Actionable.MODULATE;
    }
}
