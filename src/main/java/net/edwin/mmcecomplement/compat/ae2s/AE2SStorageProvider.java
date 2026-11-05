package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.IStorageMounts;
import ae2.api.storage.IStorageProvider;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.MEStorageMonitor;
import ae2.me.helpers.IGridConnectedTile;
import appeng.api.AEApi;
import net.edwin.mmcecomplement.compat.ae.tile.TileAEStorageAccessPort;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.compat.ae.storage.StorageMEInventory;
import net.minecraft.util.text.ITextComponent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** Mounts the real storage controller and publishes each change once to AE2S. */
public final class AE2SStorageProvider implements IStorageProvider {
    private final TileAEStorageAccessPort container;
    private final Map<IMEInventoryHandler, Monitor> monitors = new IdentityHashMap<>();

    public AE2SStorageProvider(TileAEStorageAccessPort container) { this.container = container; }

    @Override public void mountInventories(IStorageMounts mounts) {
        for (IStorageChannel<?> channel : AEApi.instance().storage().storageChannels()) {
            for (IMEInventoryHandler inventory : container.getCellArray(channel)) {
                Monitor monitor = monitors.computeIfAbsent(inventory, Monitor::new);
                mounts.mount(monitor, container.getPriority());
            }
        }
    }

    public static void changed(MEMachineComponent tile, StorageMEInventory<?> inventory,
                               IAEStack<?> stack, long delta) {
        ae2.api.networking.IGridNode node = ((IGridConnectedTile) tile).getMainNode().getNode();
        if (node == null) return;
        IStorageProvider service = node.getService(IStorageProvider.class);
        if (!(service instanceof AE2SStorageProvider)) return;
        Monitor monitor = ((AE2SStorageProvider) service).monitors.get(inventory);
        if (monitor != null) monitor.changed(AE2SKeyBridge.toKey(stack), delta);
    }

    private static final class Monitor implements MEStorageMonitor {
        private final IMEInventoryHandler inventoryHandler;
        private final StorageMEInventory<?> inventory;
        private final Map<MEStorageChangeListener, Object> listeners = new IdentityHashMap<>();

        @SuppressWarnings("unchecked")
        private Monitor(IMEInventoryHandler inventory) {
            inventoryHandler = inventory;
            this.inventory = inventory instanceof StorageMEInventory ? (StorageMEInventory<?>) inventory : null;
            if (inventory instanceof StorageMEInventory) {
                ((StorageMEInventory) inventory).setNetworkChangeListener((stack, delta) ->
                    changed(AE2SKeyBridge.toKey((IAEStack<?>) stack), (Long) delta));
            }
        }

        @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            ae2.api.storage.MEStorage.checkPreconditions(key, amount, mode, source);
            if (amount == 0 || !AE2SKeyBridge.matchesChannel(inventoryHandler.getChannel(), key)) return 0;
            IAEStack remainder = inventoryHandler.injectItems(AE2SKeyBridge.toOldStack(key, amount), legacyMode(mode), null);
            return amount - (remainder == null ? 0 : remainder.getStackSize());
        }
        @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
            ae2.api.storage.MEStorage.checkPreconditions(key, amount, mode, source);
            if (amount == 0 || !AE2SKeyBridge.matchesChannel(inventoryHandler.getChannel(), key)) return 0;
            IAEStack extracted = inventoryHandler.extractItems(AE2SKeyBridge.toOldStack(key, amount), legacyMode(mode), null);
            return extracted == null ? 0 : extracted.getStackSize();
        }
        @Override public void getAvailableStacks(KeyCounter out) {
            // A native full scan becomes the announced baseline. A later poll
            // must not announce the same external edit for a second time.
            if (inventory != null) inventory.resetSnapshot();
            appeng.api.storage.data.IItemList stacks = inventoryHandler.getAvailableItems(inventoryHandler.getChannel().createList());
            for (Object entry : stacks) {
                IAEStack<?> stack = (IAEStack<?>) entry;
                if (stack.getStackSize() > 0) out.add(AE2SKeyBridge.toKey(stack), stack.getStackSize());
            }
        }
        @Override public ITextComponent getDescription() { return new net.minecraft.util.text.TextComponentString("MMCE Complement storage"); }
        @Override public void addListener(MEStorageChangeListener listener, Object token) {
            if (listeners.containsKey(listener)) throw new IllegalStateException("Storage listener already registered");
            listeners.put(listener, token);
        }
        @Override public void removeListener(MEStorageChangeListener listener) { listeners.remove(listener); }

        private static appeng.api.config.Actionable legacyMode(Actionable mode) {
            return mode == Actionable.SIMULATE ? appeng.api.config.Actionable.SIMULATE : appeng.api.config.Actionable.MODULATE;
        }

        private void changed(AEKey key, long delta) {
            if (delta == 0) return;
            for (MEStorageChangeListener listener : new ArrayList<>(listeners.keySet())) {
                if (!listeners.containsKey(listener)) continue;
                if (!listener.isValid(listeners.get(listener))) listeners.remove(listener);
                else listener.onStackChange(key, delta);
            }
        }
    }
}
