package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.config.Actionable;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.MEStorageMonitor;
import ae2.me.storage.ITickingMonitor;
import me.ramidzkh.mekae2.ae2.AEGasKey;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ControllerGasStorageTest {
    private static final Gas HYDROGEN = new Gas("native_storage_hydrogen", 0xFFFFFF);
    private static final Gas OXYGEN = new Gas("native_storage_oxygen", 0xAAAAFF);
    private static final AEGasKey H = AEGasKey.of(HYDROGEN);
    private static final AEGasKey O = AEGasKey.of(OXYGEN);

    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    private static class Owner extends TileEntity {
        int changes;
        @Override public void markDirty() { changes++; }
    }

    private static class Fixture {
        final Owner owner = new Owner();
        final StorageGasHandler tanks;
        final StorageController controller;
        final AtomicReference<StorageController> binding;
        final ControllerStorage storage;
        boolean enabled = true;

        Fixture(int slots, int capacity) {
            tanks = new StorageGasHandler(owner, slots, capacity);
            controller = (StorageController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { StorageController.class }, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "mmceComplement$getStorageInventory":
                        case "mmceComplement$getFluidStorage": return null;
                        case "mmceComplement$hasGasStorage": return enabled;
                        case "mmceComplement$getGasStorage":
                            assertTrue(enabled, "Disabled gas storage must not be queried");
                            return tanks;
                        default: throw new AssertionError(method.getName());
                    }
                });
            binding = new AtomicReference<>(controller);
            storage = new ControllerStorage(binding::get);
        }

        long count(AEKey key) {
            KeyCounter contents = new KeyCounter();
            storage.getAvailableStacks(contents);
            return contents.get(key);
        }
    }

    @Test void nativeAdapterLoadsWithoutLegacyAeApi() throws Exception {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("appeng.api.AEApi"));
        assertNotNull(NativeGasStorageBridge.create(() -> null));
        assertEquals(0, new Fixture(1, 1000).storage.insert(H, 0, Actionable.MODULATE, null));
    }

    @Test void transfersUseExactGasAndSimulateWithoutChangingTanks() {
        Fixture f = new Fixture(3, 1000);
        f.tanks.receiveGasInSlot(1, new GasStack(HYDROGEN, 600), true);
        f.tanks.receiveGasInSlot(2, new GasStack(OXYGEN, 500), true);
        int changes = f.owner.changes;
        assertEquals(1400, f.storage.insert(H, 2000, Actionable.SIMULATE, null));
        assertEquals(600, f.count(H));
        assertEquals(changes, f.owner.changes);
        assertEquals(1400, f.storage.insert(H, 2000, Actionable.MODULATE, null));
        assertEquals(1000, f.tanks.getAmount(1), "Fill existing gas tanks before empty ones");
        assertEquals(2000, f.count(H));
        assertEquals(500, f.count(O));
        changes = f.owner.changes;
        assertEquals(2000, f.storage.extract(H, Long.MAX_VALUE, Actionable.SIMULATE, null));
        assertEquals(changes, f.owner.changes);
        assertEquals(2000, f.storage.extract(H, Long.MAX_VALUE, Actionable.MODULATE, null));
        assertEquals(0, f.count(H));
        assertEquals(500, f.count(O));
        assertEquals(0, f.storage.extract(H, 1, Actionable.MODULATE, null));
    }

    @Test void longTotalsDoNotOverflowOrRepeatedlyCountSimulatedTankSpace() {
        Fixture f = new Fixture(2, Integer.MAX_VALUE);
        long capacity = 2L * Integer.MAX_VALUE;
        assertEquals(capacity, f.storage.insert(H, Long.MAX_VALUE, Actionable.SIMULATE, null));
        assertEquals(0, f.count(H));
        assertEquals(0, f.owner.changes);
        assertEquals(capacity, f.storage.insert(H, Long.MAX_VALUE, Actionable.MODULATE, null));
        assertEquals(capacity, f.count(H));
        assertEquals(0, f.storage.insert(H, 1, Actionable.MODULATE, null));
        assertEquals(0, f.storage.insert(O, 1, Actionable.MODULATE, null));
        assertEquals(capacity, f.storage.extract(H, Long.MAX_VALUE, Actionable.SIMULATE, null));
        assertEquals(capacity, f.count(H));
        assertEquals(capacity, f.storage.extract(H, Long.MAX_VALUE, Actionable.MODULATE, null));
        assertEquals(0, f.count(H));
    }

    @Test void simulationDoesNotCountPartiallyFilledTankTwice() {
        Fixture f = new Fixture(1, 1000);
        f.tanks.receiveGasInSlot(0, new GasStack(HYDROGEN, 600), true);
        assertEquals(400, f.storage.insert(H, Long.MAX_VALUE, Actionable.SIMULATE, null));
        assertEquals(600, f.count(H));
        assertEquals(0, f.storage.insert(O, 1000, Actionable.SIMULATE, null));
    }

    @Test void disabledGasAndUnformedStructuresRejectAccessWithoutDeletingContents() {
        Fixture f = new Fixture(1, 1000);
        assertEquals(100, f.storage.insert(H, 100, Actionable.MODULATE, null));
        f.enabled = false;
        assertEquals(0, f.count(H));
        assertEquals(0, f.storage.insert(H, 1, Actionable.MODULATE, null));
        assertEquals(0, f.storage.extract(H, 1, Actionable.MODULATE, null));
        f.enabled = true;
        f.binding.set(null);
        assertEquals(0, f.count(H));
        assertEquals(0, f.storage.insert(H, 1, Actionable.MODULATE, null));
        assertEquals(0, f.storage.extract(H, 1, Actionable.MODULATE, null));
        assertEquals(100, f.tanks.getAmount(0));
        f.binding.set(f.controller);
        assertEquals(100, f.count(H));
    }

    @Test void nativeMonitorImmediatelyReflectsTransfersAndTicksExternalMutations() throws Exception {
        Fixture f = new Fixture(2, 1000);
        Class<?> type = Class.forName(StorageBusSettings.class.getName() + "$ControllerInventory");
        Constructor<?> ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);
        MEStorageMonitor monitor = (MEStorageMonitor) ctor.newInstance();
        Method bind = type.getDeclaredMethod("bind", MEStorage.class);
        bind.setAccessible(true);
        bind.invoke(monitor, f.storage);
        KeyCounter terminal = new KeyCounter();
        monitor.addListener(new MEStorageChangeListener() {
            @Override public boolean isValid(Object token) { return true; }
            @Override public void onStackChange(AEKey key, long delta) { terminal.add(key, delta); }
            @Override public void onListUpdate() { terminal.reset(); monitor.getAvailableStacks(terminal); }
        }, this);
        monitor.getAvailableStacks(terminal);
        for (int cycle = 0; cycle < 3; cycle++) {
            assertEquals(100, monitor.insert(H, 100, Actionable.MODULATE, null));
            assertEquals(100, terminal.get(H));
            assertEquals(100, monitor.extract(H, 100, Actionable.SIMULATE, null));
            assertEquals(100, terminal.get(H));
            assertEquals(100, monitor.extract(H, 100, Actionable.MODULATE, null));
            assertEquals(0, terminal.get(H), "No ghost gas after extraction");
        }
        f.tanks.receiveGasInSlot(0, new GasStack(HYDROGEN, 250), true);
        ((ITickingMonitor) monitor).onTick();
        assertEquals(250, terminal.get(H));
        f.tanks.drawGas(new GasStack(HYDROGEN, 50), true);
        ((ITickingMonitor) monitor).onTick();
        assertEquals(200, terminal.get(H));
        f.binding.set(null);
        ((ITickingMonitor) monitor).onTick();
        assertEquals(0, terminal.get(H));
        assertEquals(200, f.tanks.getAmount(0));
    }

    @Test void twoPortsShareAtomicBackingTransfers() throws Exception {
        Fixture f = new Fixture(1, 1000);
        ControllerStorage second = new ControllerStorage(f.binding::get);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<Long> first = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return f.storage.insert(H, 700, Actionable.MODULATE, null);
            });
            Future<Long> other = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return second.insert(H, 700, Actionable.MODULATE, null);
            });
            start.countDown();
            assertEquals(1000, first.get(10, TimeUnit.SECONDS) + other.get(10, TimeUnit.SECONDS));
            assertEquals(1000, f.count(H));
            Future<Long> drawFirst = executor.submit(() -> f.storage.extract(H, 700, Actionable.MODULATE, null));
            Future<Long> drawOther = executor.submit(() -> second.extract(H, 700, Actionable.MODULATE, null));
            assertEquals(1000, drawFirst.get(10, TimeUnit.SECONDS) + drawOther.get(10, TimeUnit.SECONDS));
            assertEquals(0, f.count(H));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test void existingNativePortsObserveLiveTankCountsAndIndividualCapacities() {
        Fixture f = new Fixture(1, 1000);
        ControllerStorage second = new ControllerStorage(f.binding::get);
        assertTrue(f.tanks.resize(2));
        assertTrue(f.tanks.setCapacityInSlot(1, 4000));
        assertEquals(5000, f.storage.insert(H, 6000, Actionable.SIMULATE, null));
        assertEquals(5000, second.insert(H, 6000, Actionable.MODULATE, null));
        assertEquals(5000, f.count(H));
        assertFalse(f.tanks.resize(1));
        assertFalse(f.tanks.setCapacityInSlot(1, 3999));
        assertEquals(5000, f.storage.extract(H, 5000, Actionable.MODULATE, null));
        assertTrue(f.tanks.resize(1));
        assertEquals(0, f.count(H));
        assertEquals(1000, second.insert(H, 6000, Actionable.MODULATE, null));
    }
}
