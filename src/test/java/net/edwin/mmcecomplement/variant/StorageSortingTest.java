package net.edwin.mmcecomplement.variant;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageSortingTest {
    private static final Gas HYDROGEN = new Gas("sort_hydrogen", 0xFFFFFF);
    private static final Gas OXYGEN = new Gas("sort_oxygen", 0xAAFFFF);

    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void fluidSortMergesEqualTagsKeepsDistinctTagsAndPacksEmptySlotsLast() {
        TrackingOwner owner = new TrackingOwner();
        StorageFluidHandler storage = new StorageFluidHandler(owner, 5, 1000);
        FluidStack a = water(500, "a"), b = water(300, "b");
        storage.getTank(0).setFluid(a);
        storage.getTank(1).setFluid(new FluidStack(FluidRegistry.LAVA, 400));
        storage.getTank(2).setFluid(water(800, "a"));
        storage.getTank(3).setFluid(b);
        storage.sort();
        assertEquals(FluidRegistry.LAVA, storage.getTank(0).getFluid().getFluid());
        assertEquals(400, storage.getTank(0).getFluidAmount());
        assertEquals(1000, storage.getTank(1).getFluidAmount());
        assertEquals(300, storage.getTank(2).getFluidAmount());
        assertEquals("a", storage.getTank(1).getFluid().tag.getString("kind"));
        assertEquals("a", storage.getTank(2).getFluid().tag.getString("kind"));
        assertEquals("b", storage.getTank(3).getFluid().tag.getString("kind"));
        assertEquals(300, storage.getTank(3).getFluidAmount());
        assertNull(storage.getTank(4).getFluid());
        assertEquals(500, a.amount);
        assertEquals(300, b.amount);
        int notifications = owner.changes;
        storage.sort();
        assertEquals(notifications, owner.changes);
    }

    @Test void fluidSortIncludesSlotsBeyondTheVisiblePage() {
        StorageFluidHandler storage = new StorageFluidHandler(new TrackingOwner(), 60, 1000);
        storage.getTank(59).setFluid(new FluidStack(FluidRegistry.WATER, 500));
        storage.sort();
        assertEquals(500, storage.getTank(0).getFluidAmount());
        assertNull(storage.getTank(59).getFluid());
    }

    @Test void fluidSortTotalsCanExceedIntegerRange() {
        StorageFluidHandler storage = new StorageFluidHandler(new TrackingOwner(), 2, Integer.MAX_VALUE);
        storage.getTank(0).setFluid(new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE));
        storage.getTank(1).setFluid(new FluidStack(FluidRegistry.WATER, Integer.MAX_VALUE));
        storage.sort();
        assertEquals(4294967294L, (long) storage.getTank(0).getFluidAmount()
            + storage.getTank(1).getFluidAmount());
    }

    @Test void malformedOverCapacityFluidIsRetainedIfItCannotBeRepacked() {
        StorageFluidHandler storage = new StorageFluidHandler(new TrackingOwner(), 1, 1000);
        storage.getTank(0).setFluid(new FluidStack(FluidRegistry.WATER, 1500));
        storage.sort();
        assertEquals(1500, storage.getTank(0).getFluidAmount());
    }

    @Test void gasSortMergesAndOrdersTypesWithoutLosingStockOrRepeatedNotifications() {
        TrackingOwner owner = new TrackingOwner();
        StorageGasHandler storage = new StorageGasHandler(owner, 5, 1000);
        storage.receiveGas(null, new GasStack(OXYGEN, 1000), true);
        storage.receiveGas(null, new GasStack(HYDROGEN, 1000), true);
        storage.receiveGas(null, new GasStack(OXYGEN, 500), true);
        storage.drawGas(new GasStack(OXYGEN, 800), true);
        storage.drawGas(new GasStack(HYDROGEN, 700), true);
        StorageGasBridge.sort(storage);
        assertSame(HYDROGEN, storage.getStack(0).getGas());
        assertEquals(300, storage.getAmount(0));
        assertSame(OXYGEN, storage.getStack(1).getGas());
        assertEquals(700, storage.getAmount(1));
        for (int i = 2; i < 5; i++) assertNull(storage.getStack(i));
        int notifications = owner.changes;
        StorageGasBridge.sort(storage);
        assertEquals(notifications, owner.changes);
    }

    @Test void gasSortSplitsMergedTypesAtPerTankCapacity() {
        StorageGasHandler storage = new StorageGasHandler(new TrackingOwner(), 4, 1000);
        storage.receiveGas(null, new GasStack(OXYGEN, 1000), true);
        storage.receiveGas(null, new GasStack(HYDROGEN, 300), true);
        storage.receiveGas(null, new GasStack(OXYGEN, 800), true);
        storage.drawGas(new GasStack(OXYGEN, 500), true);
        storage.sort();
        assertEquals(300, storage.getAmount(0));
        assertSame(HYDROGEN, storage.getStack(0).getGas());
        assertEquals(1000, storage.getAmount(1));
        assertEquals(300, storage.getAmount(2));
        assertSame(OXYGEN, storage.getStack(1).getGas());
        assertSame(OXYGEN, storage.getStack(2).getGas());
        assertNull(storage.getStack(3));
    }

    @Test void gasSortTotalsCanExceedIntegerRange() {
        StorageGasHandler storage = new StorageGasHandler(new TrackingOwner(), 2, Integer.MAX_VALUE);
        storage.receiveGas(null, new GasStack(HYDROGEN, Integer.MAX_VALUE), true);
        storage.receiveGas(null, new GasStack(HYDROGEN, Integer.MAX_VALUE), true);
        storage.sort();
        assertEquals(4294967294L, (long) storage.getAmount(0) + storage.getAmount(1));
    }

    @Test void malformedOverCapacityGasIsRetainedIfItCannotBeRepacked() {
        StorageGasHandler storage = new StorageGasHandler(new TrackingOwner(), 1, 1000);
        storage.receiveGas(null, new GasStack(HYDROGEN, 1000), true);
        storage.getStack(0).amount = 1500;
        storage.sort();
        assertEquals(1500, storage.getAmount(0));
    }

    @Test void zeroSlotsAndEmptyStorageAreSafeToSort() {
        assertDoesNotThrow(() -> new StorageFluidHandler(new TrackingOwner(), 0, 1000).sort());
        assertDoesNotThrow(() -> new StorageGasHandler(new TrackingOwner(), 0, 1000).sort());
        assertDoesNotThrow(() -> StorageGasBridge.sort(null));
    }

    private static FluidStack water(int amount, String tag) {
        FluidStack result = new FluidStack(FluidRegistry.WATER, amount);
        result.tag = new NBTTagCompound();
        result.tag.setString("kind", tag);
        return result;
    }

    private static final class TrackingOwner extends TileEntity {
        private int changes;
        @Override public void markDirty() { changes++; }
    }
}
