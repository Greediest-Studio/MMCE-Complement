package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import java.util.ArrayList;
import java.util.List;

/** Direct backing inventory with bus-style change detection for external edits. */
public abstract class StorageMEInventory<T extends IAEStack<T>> implements IMEInventoryHandler<T> {
    private final IStorageChannel<T> channel;
    private IItemList<T> announced;
    private IMEInventoryHandler<T> networkView = this;

    protected StorageMEInventory(IStorageChannel<T> channel) {
        this.channel = channel;
        announced = channel.createList();
    }

    public final IMEInventoryHandler<T> getNetworkView() { return networkView; }

    public final void setNetworkView(IMEInventoryHandler<T> view) {
        networkView = view;
    }

    public final void resetSnapshot() { announced = networkView.getAvailableItems(channel.createList()); }

    // ICellContainer handlers do NOT automatically announce their own AE
    // transfers. Keep the last announced snapshot until the delta is posted,
    // including changes made by injectItems/extractItems through this handler.
    public final List<T> pollChanges() {
        IItemList<T> current = networkView.getAvailableItems(channel.createList());
        IItemList<T> difference = channel.createList();
        for (T stack : current) difference.add(stack.copy());
        for (T stack : announced) difference.add(stack.copy().setStackSize(-stack.getStackSize()));
        List<T> changes = new ArrayList<>();
        for (T stack : difference) if (stack.getStackSize() != 0) changes.add(stack.copy());
        announced = current;
        return changes;
    }

    @Override public final IStorageChannel<T> getChannel() { return channel; }
    @Override public AccessRestriction getAccess() { return AccessRestriction.READ_WRITE; }
    @Override public boolean isPrioritized(T input) { return false; }
    @Override public boolean canAccept(T input) {
        if (input == null || input.getStackSize() <= 0) return false;
        T remainder = injectItems(input, Actionable.SIMULATE, null);
        return remainder == null || remainder.getStackSize() < input.getStackSize();
    }
    @Override public int getPriority() { return 0; }
    @Override public int getSlot() { return 0; }
    @Override public boolean validForPass(int pass) { return true; }
}
