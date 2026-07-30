package net.edwin.mmcecomplement.filter;

import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/** One-slot, int-max item store with an exact ghost-stack filter. */
public final class FilteredItemHandler implements FilteredItemRecipeHandler {

    private ItemStack stored = ItemStack.EMPTY;
    private ItemStack filter = ItemStack.EMPTY;
    private final Runnable changed;

    public FilteredItemHandler(Runnable changed) {
        this.changed = changed == null ? () -> { } : changed;
    }

    private FilteredItemHandler(ItemStack stored, ItemStack filter) {
        this.changed = () -> { };
        this.stored = copy(stored);
        this.filter = normalizedFilter(filter);
    }

    public ItemStack getFilter() {
        return copy(filter);
    }

    public void setFilter(ItemStack stack) {
        ItemStack normalized = normalizedFilter(stack);
        if (sameExact(filter, normalized)) return;
        filter = normalized;
        changed.run();
    }

    public boolean accepts(ItemStack stack) {
        return !filter.isEmpty() && !stack.isEmpty() && sameExact(filter, stack);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        checkSlot(slot);
        return stored;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack,
                                boolean simulate) {
        checkSlot(slot);
        if (stack.isEmpty() || !accepts(stack)
            || (!stored.isEmpty() && !sameExact(stored, stack))) {
            return stack;
        }
        int accepted = roomFor(stack.getCount());
        if (accepted <= 0) return stack;
        if (!simulate) add(stack, accepted);
        if (accepted >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.setCount(stack.getCount() - accepted);
        return remainder;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        checkSlot(slot);
        if (amount <= 0 || stored.isEmpty()) return ItemStack.EMPTY;
        int extracted = Math.min(amount, stored.getCount());
        ItemStack result = stored.copy();
        result.setCount(extracted);
        if (!simulate) {
            int left = stored.getCount() - extracted;
            if (left <= 0) stored = ItemStack.EMPTY;
            else stored.setCount(left);
            changed.run();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        checkSlot(slot);
        return Integer.MAX_VALUE;
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        checkSlot(slot);
        if (stack.isEmpty()) {
            if (!stored.isEmpty()) {
                stored = ItemStack.EMPTY;
                changed.run();
            }
            return;
        }
        if (!accepts(stack)) return;
        stored = stack.copy();
        if (stored.getCount() < 1) stored.setCount(1);
        changed.run();
    }

    @Override
    public int insertFilteredOutput(ItemStack stack, int amount,
                                    boolean simulate) {
        if (amount <= 0 || stack.isEmpty() || !accepts(stack)
            || (!stored.isEmpty() && !sameExact(stored, stack))) {
            return 0;
        }
        int accepted = roomFor(amount);
        if (!simulate && accepted > 0) add(stack, accepted);
        return accepted;
    }

    @Override
    public FilteredItemRecipeHandler copyForSimulation() {
        return new FilteredItemHandler(stored, filter);
    }

    public void load(ItemStack storedStack, int count, ItemStack filterStack) {
        filter = normalizedFilter(filterStack);
        if (storedStack == null || storedStack.isEmpty() || count <= 0) {
            stored = ItemStack.EMPTY;
        } else {
            stored = storedStack.copy();
            stored.setCount(count);
        }
    }

    private int roomFor(int requested) {
        int current = stored.isEmpty() ? 0 : Math.max(0, stored.getCount());
        long room = (long) Integer.MAX_VALUE - current;
        return (int) Math.min(Math.max(0L, requested), room);
    }

    private void add(ItemStack template, int amount) {
        if (stored.isEmpty()) {
            stored = template.copy();
            stored.setCount(amount);
        } else {
            stored.setCount(stored.getCount() + amount);
        }
        changed.run();
    }

    private static ItemStack normalizedFilter(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.copy();
        result.setCount(1);
        return result;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public static boolean sameExact(ItemStack first, ItemStack second) {
        return first != null && second != null
            && ItemStack.areItemsEqual(first, second)
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static void checkSlot(int slot) {
        if (slot != 0) throw new IndexOutOfBoundsException("Slot: " + slot);
    }
}
