package net.edwin.mmcecomplement.variant;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasTransfer;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StorageGasTransferTest {
    private static final Gas HYDROGEN = new Gas("storage_test_hydrogen", 0xFFFFFF);
    private static final Gas OXYGEN = new Gas("storage_test_oxygen", 0xAAFFFF);
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void gasUsesAllMatchingAndEmptyTanksAndSimulationPreservesStock() {
        StorageGasHandler storage = gases(3, 1000);
        storage.receiveGas(null, new GasStack(HYDROGEN, 900), true);
        assertEquals(2100, storage.receiveGas(null, new GasStack(HYDROGEN, 2500), false));
        assertEquals(900, storage.getAmount(0));
        assertEquals(0, storage.getAmount(1));
        assertEquals(2100, storage.receiveGas(null, new GasStack(HYDROGEN, 2500), true));
        assertEquals(3000, storage.drawGas(new GasStack(HYDROGEN, 4000), false).amount);
    }

    @Test void overflowRemainsInInputAndBridgeTransfersOnlyAcceptedGas() {
        StorageGasHandler input = gases(1, 3000), storage = gases(2, 1000);
        input.receiveGas(null, new GasStack(HYDROGEN, 2500), true);
        AtomicInteger changed = new AtomicInteger();
        assertTrue(StorageGasBridge.transfer(input, storage, changed::incrementAndGet));
        assertEquals(500, input.getAmount(0));
        assertEquals(1000, storage.getAmount(0));
        assertEquals(1000, storage.getAmount(1));
        assertEquals(1, changed.get());
        storage.drawGas(new GasStack(HYDROGEN, 1000), true);
        assertFalse(StorageGasBridge.transfer(input, storage, changed::incrementAndGet));
        assertEquals(0, input.getAmount(0));
    }

    @Test void exportsLaterGasWhenFirstTypeIsRejected() {
        StorageGasHandler storage = gases(2, 1000), output = gases(1, 1000);
        storage.receiveGas(null, new GasStack(HYDROGEN, 800), true);
        storage.receiveGas(null, new GasStack(OXYGEN, 600), true);
        output.receiveGas(null, new GasStack(OXYGEN, 200), true);
        assertTrue(StorageGasTransfer.transfer(storage, output, () -> { }));
        assertEquals(800, storage.getAmount(0));
        assertEquals(0, storage.getAmount(1));
        assertSame(OXYGEN, output.getStack(0).getGas());
        assertEquals(800, output.getAmount(0));
    }

    @Test void rejectsNonGasPortsAndDoesNotNotifyBlockedOutputs() {
        StorageGasHandler storage = gases(1, 1000), output = gases(0, 1000);
        storage.receiveGas(null, new GasStack(HYDROGEN, 800), true);
        AtomicInteger changed = new AtomicInteger();
        assertFalse(StorageGasBridge.transfer(new Object(), storage, changed::incrementAndGet));
        assertTrue(StorageGasBridge.transfer(storage, output, changed::incrementAndGet));
        assertEquals(0, changed.get());
        assertEquals(800, storage.getAmount(0));
    }

    private static StorageGasHandler gases(int slots, int capacity) {
        return new StorageGasHandler(new TileEntity() { }, slots, capacity);
    }
}
