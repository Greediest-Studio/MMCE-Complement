package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.config.Actionable;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.storage.impl.GasStorageChannel;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.mekeng.StorageMEGasInventory;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class StorageMEGasInventoryTest {
    @Test void optionalGasChannelSupportsDirectSafeTransfersAndExternalChanges() {
        boolean available;
        try {
            Class.forName("com.mekeng.github.common.me.storage.IGasStorageChannel", false, getClass().getClassLoader());
            available = true;
        } catch (ClassNotFoundException e) { available = false; }
        Assumptions.assumeTrue(available, "Run with -Ptest_ae_storage_gas to test the optional gas channel");
        WithGas.verify();
    }
    // Keep optional signatures outside the outer test class so discovery works
    // in the ordinary test run, which deliberately has no MekEng on its classpath.
    static class WithGas {
        static void verify() {
            Bootstrap.register();
            Gas hydrogen = new Gas("ae_storage_test_hydrogen", 0xFFFFFF);
            Gas oxygen = new Gas("ae_storage_test_oxygen", 0xAAAAFF);
            StorageGasHandler backing = new StorageGasHandler(new TileEntity() { }, 2, 1000);
            AtomicBoolean visible = new AtomicBoolean(true);
            StorageMEGasInventory inventory = new StorageMEGasInventory(GasStorageChannel.INSTANCE,
                () -> visible.get() ? backing : null);
            IAEGasStack request = GasStorageChannel.INSTANCE.createStack(new GasStack(hydrogen, 2500));
            assertEquals(500, inventory.injectItems(request, Actionable.SIMULATE, null).getStackSize());
            assertEquals(0, backing.getAmount(0));
            assertTrue(inventory.pollChanges().isEmpty());
            assertEquals(500, inventory.injectItems(request, Actionable.MODULATE, null).getStackSize());
            assertEquals(2500, request.getStackSize());
            assertEquals(2000, inventory.getAvailableItems(GasStorageChannel.INSTANCE.createList()).getFirstItem().getStackSize());
            assertNull(inventory.extractItems(GasStorageChannel.INSTANCE.createStack(new GasStack(oxygen, 500)), Actionable.MODULATE, null));
            assertEquals(1500, inventory.extractItems(request.copy().setStackSize(1500), Actionable.SIMULATE, null).getStackSize());
            assertEquals(1000, backing.getAmount(0));
            assertEquals(1500, inventory.extractItems(request.copy().setStackSize(1500), Actionable.MODULATE, null).getStackSize());
            assertTrue(inventory.pollChanges().isEmpty());
            backing.receiveGas(null, new GasStack(oxygen, 100), true);
            assertEquals(100, inventory.pollChanges().get(0).getStackSize());
            visible.set(false);
            long removed = inventory.pollChanges().stream().mapToLong(IAEGasStack::getStackSize).sum();
            assertEquals(-600, removed);
            assertFalse(inventory.canAccept(request));
            assertNull(inventory.extractItems(request, Actionable.MODULATE, null));
            visible.set(true);
            assertEquals(600, inventory.pollChanges().stream().mapToLong(IAEGasStack::getStackSize).sum());

            StorageGasHandler large = new StorageGasHandler(new TileEntity() { }, 2, Integer.MAX_VALUE);
            StorageMEGasInventory largeInventory = new StorageMEGasInventory(GasStorageChannel.INSTANCE, () -> large);
            IAEGasStack huge = request.copy().setStackSize(4294967294L);
            assertEquals(2147483647L, largeInventory.injectItems(huge, Actionable.MODULATE, null).getStackSize());
            assertNull(largeInventory.injectItems(huge.copy().setStackSize(Integer.MAX_VALUE), Actionable.MODULATE, null));
            assertEquals(4294967294L, largeInventory.getAvailableItems(GasStorageChannel.INSTANCE.createList()).getFirstItem().getStackSize());
            assertTrue(largeInventory.pollChanges().isEmpty());
            assertEquals(2147483647L, largeInventory.extractItems(huge, Actionable.MODULATE, null).getStackSize());
        }
    }
}
