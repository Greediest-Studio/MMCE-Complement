package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.api.item.IIngredient;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.common.integration.crafttweaker.gas.CraftTweakerGasStack;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.edwin.mmcecomplement.variant.StorageFluidHandler;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class StorageScriptAccessTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void itemRequestsArePartialSimulatableAndMatchNbtExactly() {
        ItemStackHandler items = new ItemStackHandler(2);
        ItemStack tagged = new ItemStack(Items.DIAMOND, 100);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound().setString("owner", "one");
        assertEquals(100, StorageScriptAccess.transferItem(items, tagged, -1, true, true));
        assertEquals(0, StorageScriptAccess.countItem(items, tagged));
        assertEquals(100, StorageScriptAccess.transferItem(items, tagged, -1, true, false));
        assertEquals(100, tagged.getCount());
        assertEquals(0, StorageScriptAccess.countItem(items, new ItemStack(Items.DIAMOND)));
        ItemStack snapshot = StorageScriptAccess.itemAt(items, 0);
        snapshot.getTagCompound().setString("owner", "changed");
        snapshot.setCount(1);
        assertEquals(100, StorageScriptAccess.countItem(items, tagged));
        assertEquals(28, StorageScriptAccess.transferItem(items, tagged, -1, true, false));
        assertEquals(100, StorageScriptAccess.transferItem(items, tagged, -1, false, true));
        assertEquals(128, StorageScriptAccess.countItem(items, tagged));
        assertEquals(64, StorageScriptAccess.transferItem(items, tagged, 1, false, false));
        assertEquals(64, StorageScriptAccess.transferItem(items, tagged, -1, false, false));
        assertEquals(0, StorageScriptAccess.countItem(items, tagged));
        for (int slot : new int[] {-2, 2, Integer.MAX_VALUE}) {
            assertEquals(0, StorageScriptAccess.transferItem(items, tagged, slot, true, false));
            assertTrue(StorageScriptAccess.itemAt(items, slot).isEmpty());
        }
    }

    @Test void bulkItemInsertFillsMatchingStacksBeforeEmptySlots() {
        ItemStackHandler items = new ItemStackHandler(2);
        items.setStackInSlot(1, new ItemStack(Items.DIAMOND, 60));
        assertEquals(4, StorageScriptAccess.transferItem(items, new ItemStack(Items.DIAMOND, 4), -1, true, false));
        assertTrue(items.getStackInSlot(0).isEmpty());
        assertEquals(64, items.getStackInSlot(1).getCount());
    }

    @Test void concurrentScriptCallsCountOnlyActualAcceptedItems() throws Exception {
        // Handler callbacks represent the same notifications used by shared hatches.
        java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
        ItemStackHandler items = new ItemStackHandler(1) {
            @Override protected void onContentsChanged(int slot) { changes.incrementAndGet(); }
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> operation = () -> {
                start.await();
                return StorageScriptAccess.transferItem(items, new ItemStack(Items.DIAMOND, 40), -1, true, false);
            };
            Future<Integer> first = executor.submit(operation), second = executor.submit(operation);
            start.countDown();
            assertEquals(64, first.get() + second.get());
            assertEquals(64, StorageScriptAccess.countItem(items, new ItemStack(Items.DIAMOND)));
            assertEquals(2, changes.get());
        } finally { executor.shutdownNow(); }
    }

    @Test void fluidTransfersRespectTanksNbtSimulationAndDetachedSnapshots() {
        StorageFluidHandler fluids = new StorageFluidHandler(new TileEntity() { }, 2, 1000);
        FluidStack request = new FluidStack(FluidRegistry.WATER, 2500);
        request.tag = new NBTTagCompound();
        request.tag.setString("owner", "one");
        assertEquals(2000, StorageScriptAccess.transferFluid(fluids, request, true, true));
        assertEquals(0, StorageScriptAccess.countFluid(fluids, request));
        assertEquals(1000, StorageScriptAccess.transferFluid(fluids.getTankInteractionHandler(1), request, true, false));
        FluidStack snapshot = StorageScriptAccess.fluidAt(fluids.getTankInteractionHandler(1));
        snapshot.amount = 1;
        snapshot.tag.setString("owner", "changed");
        assertEquals(1000, StorageScriptAccess.countFluid(fluids, request));
        assertEquals(0, StorageScriptAccess.countFluid(fluids, new FluidStack(FluidRegistry.WATER, 1)));
        assertEquals(1000, StorageScriptAccess.transferFluid(fluids, request, true, false));
        assertEquals(2000, StorageScriptAccess.transferFluid(fluids, request, false, true));
        assertEquals(2000, StorageScriptAccess.countFluid(fluids, request));
        assertEquals(1000, StorageScriptAccess.transferFluid(fluids.getTankInteractionHandler(0), request, false, false));
        assertEquals(1000, StorageScriptAccess.transferFluid(fluids, request, false, false));
        assertEquals(2500, request.amount);
    }

    @Test void gasBridgeUsesActualOptionalStackTypeAndNeverTouchesAnotherTank() {
        StorageGasHandler gas = new StorageGasHandler(new TileEntity() { }, 2, 1000);
        Gas hydrogen = new Gas("storage_script_hydrogen", 0xFFFFFF);
        Gas oxygen = new Gas("storage_script_oxygen", 0xFFFFFF);
        IIngredient request = new CraftTweakerGasStack(new GasStack(hydrogen, 2500));
        assertEquals(1000, StorageScriptGasBridge.transfer(gas, request, 1, true, true));
        assertEquals(0, StorageScriptGasBridge.count(gas, request));
        assertEquals(1000, StorageScriptGasBridge.transfer(gas, request, 1, true, false));
        IIngredient snapshot = StorageScriptGasBridge.stack(gas, 1);
        ((GasStack) snapshot.getInternal()).amount = 1;
        assertEquals(1000, StorageScriptGasBridge.count(gas, request));
        assertEquals(0, gas.getAmount(0));
        assertEquals(0, StorageScriptGasBridge.transfer(gas,
            new CraftTweakerGasStack(new GasStack(oxygen, 100)), 1, false, false));
        assertEquals(1000, StorageScriptGasBridge.transfer(gas, request, -1, true, false));
        assertEquals(2000, StorageScriptGasBridge.transfer(gas, request, -1, false, true));
        assertEquals(2000, StorageScriptGasBridge.count(gas, request));
        assertEquals(1000, StorageScriptGasBridge.transfer(gas, request, 1, false, false));
        assertEquals(1000, StorageScriptGasBridge.transfer(gas, request, -1, false, false));
        assertEquals(0, StorageScriptGasBridge.transfer(gas, request, -2, true, false));
        assertEquals(0, StorageScriptGasBridge.transfer(null, request, -1, true, false));
    }
}
