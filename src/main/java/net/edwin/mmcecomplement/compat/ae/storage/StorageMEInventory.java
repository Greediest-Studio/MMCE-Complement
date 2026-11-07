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
import java.util.function.BiConsumer;

/** Direct backing inventory with bus-style change detection for external edits. */
public abstract class StorageMEInventory<T extends IAEStack<T>> implements IMEInventoryHandler<T> {
    private final IStorageChannel<T> channel;
    private IItemList<T> announced;
    private BiConsumer<T, Long> networkChangeListener;

    protected StorageMEInventory(IStorageChannel<T> channel) {
        this.channel = channel;
        announced = channel.createList();
    }

    public final void resetSnapshot() { announced = getAvailableItems(channel.createList()); }

    /** AE already reports its own transfers; retain only changes made outside AE. */
    protected final void networkChange(T prototype, long delta) {
        T previous = announced.findPrecise(prototype);
        long amount = Math.max(0, (previous == null ? 0 : previous.getStackSize()) + delta);
        if (previous == null) announced.add(prototype.copy().setStackSize(amount));
        else previous.setStackSize(amount);
        if (delta != 0 && networkChangeListener != null) networkChangeListener.accept(prototype, delta);
    }

    public final void setNetworkChangeListener(BiConsumer<T, Long> listener) { networkChangeListener = listener; }

    public final List<T> pollChanges() {
        List<T> changes = new ArrayList<>();
        pollNetworkChanges((stack, delta) -> changes.add(stack.copy().setStackSize(delta)));
        return changes;
    }

    /** Keep the signed delta separate: official legacy stack wrappers clamp sizes to zero. */
    public final void pollNetworkChanges(BiConsumer<T, Long> listener) {
        IItemList<T> current = getAvailableItems(channel.createList());
        for (T stack : current) {
            T previous = announced.findPrecise(stack);
            long delta = stack.getStackSize() - (previous == null ? 0 : previous.getStackSize());
            if (delta != 0) listener.accept(stack, delta);
        }
        for (T stack : announced) {
            if (current.findPrecise(stack) == null && stack.getStackSize() != 0) {
                listener.accept(stack, -stack.getStackSize());
            }
        }
        announced = current;
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
