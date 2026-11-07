package net.edwin.mmcecomplement.compat.ae.tile;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

class IOInventoryListenerCompatTest {
    @Test void jdkListenerReportsChangedSlot() {
        ModernInventory inventory = new ModernInventory();
        AtomicInteger changed = new AtomicInteger(-2);
        IOInventoryListenerCompat.setListener(inventory, changed::set);
        inventory.listener.accept(7);
        assertEquals(7, changed.get());
    }

    @Test void mmcePrimitiveListenerReportsChangedSlotAndHasStableIdentity() {
        PrimitiveInventory inventory = new PrimitiveInventory();
        AtomicInteger changed = new AtomicInteger(-2);
        IOInventoryListenerCompat.setListener(inventory, changed::set);
        inventory.listener.accept(8);
        assertEquals(8, changed.get());
        ((java.util.function.Consumer<Integer>) inventory.listener).accept(9);
        assertEquals(9, changed.get());
        assertEquals(inventory.listener, inventory.listener);
        assertNotEquals(new Object(), inventory.listener);
        assertEquals(System.identityHashCode(inventory.listener), inventory.listener.hashCode());
        assertNotNull(inventory.listener.toString());
    }

    @Test void mmceBoxedListenerReportsChangedSlot() {
        BoxedInventory inventory = new BoxedInventory();
        AtomicInteger changed = new AtomicInteger(-2);
        IOInventoryListenerCompat.setListener(inventory, changed::set);
        inventory.listener.accept(4);
        assertEquals(4, changed.get());
    }

    @Test void oldNoSlotCallbackStillRefreshesWholeDisplay() {
        OldInventory inventory = new OldInventory();
        AtomicInteger changed = new AtomicInteger(-2);
        IOInventoryListenerCompat.setListener(inventory, changed::set);
        inventory.listener.onChange();
        assertEquals(-1, changed.get());
    }

    @Test void failedRegistrationIsReportedWithOriginalCause() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> IOInventoryListenerCompat.setListener(new BrokenInventory(), slot -> {}));
        assertTrue(failure.getCause() instanceof UnsupportedOperationException);
    }

    // Matches MMCE 2.3.3's IOInventory.IntConsumer, including its boxed bridge.
    public interface PrimitiveListener extends java.util.function.Consumer<Integer> {
        void accept(int slot);
        @Override default void accept(Integer slot) { accept(slot.intValue()); }
    }
    public interface BoxedListener { void accept(Integer slot); }
    public interface OldListener { void onChange(); }
    public static class ModernInventory {
        IntConsumer listener;
        public void setListener(IntConsumer value) { listener = value; }
    }
    public static class PrimitiveInventory {
        PrimitiveListener listener;
        public void setListener(PrimitiveListener value) { listener = value; }
    }
    public static class BoxedInventory {
        BoxedListener listener;
        public void setListener(BoxedListener value) { listener = value; }
    }
    public static class OldInventory {
        OldListener listener;
        public void setListener(OldListener value) { listener = value; }
    }
    public static class BrokenInventory {
        public void setListener(IntConsumer listener) { throw new UnsupportedOperationException("test failure"); }
    }
}
