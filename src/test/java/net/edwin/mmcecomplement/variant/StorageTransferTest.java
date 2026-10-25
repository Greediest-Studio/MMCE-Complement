package net.edwin.mmcecomplement.variant;

import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StorageTransferTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void itemOverflowRemainsInInputAndReportsFull() {
        ItemStackHandler input = new ItemStackHandler(1), storage = new ItemStackHandler(1);
        input.setStackInSlot(0, new ItemStack(Items.APPLE, 24));
        storage.setStackInSlot(0, new ItemStack(Items.APPLE, 60));
        assertTrue(StorageTransfer.items(input, storage));
        assertEquals(20, input.getStackInSlot(0).getCount());
        assertEquals(64, storage.getStackInSlot(0).getCount());
    }

    @Test void itemsExportInSlotAndOutputOrderAndResumeAfterRoomIsFreed() {
        ItemStackHandler storage = new ItemStackHandler(2);
        ItemStackHandler first = new ItemStackHandler(1), second = new ItemStackHandler(1);
        storage.setStackInSlot(0, new ItemStack(Items.APPLE, 64));
        storage.setStackInSlot(1, new ItemStack(Items.CARROT, 32));
        StorageTransfer.items(storage, first);
        StorageTransfer.items(storage, second);
        assertEquals(Items.APPLE, first.getStackInSlot(0).getItem());
        assertEquals(64, first.getStackInSlot(0).getCount());
        assertEquals(Items.CARROT, second.getStackInSlot(0).getItem());
        assertTrue(storage.getStackInSlot(0).isEmpty());
        assertTrue(storage.getStackInSlot(1).isEmpty());
    }

    @Test void blockedOutputAndIdleTransfersDoNotChangeStockOrNotify() {
        ItemStackHandler storage = new ItemStackHandler(1), output = new ItemStackHandler(1);
        storage.setStackInSlot(0, new ItemStack(Items.APPLE, 8));
        output.setStackInSlot(0, new ItemStack(Items.CARROT, 64));
        AtomicInteger notifications = new AtomicInteger();
        assertTrue(StorageTransfer.items(storage, output, notifications::incrementAndGet));
        assertEquals(8, storage.getStackInSlot(0).getCount());
        assertEquals(0, notifications.get());
        output.setStackInSlot(0, ItemStack.EMPTY);
        assertFalse(StorageTransfer.items(storage, output, notifications::incrementAndGet));
        assertEquals(1, notifications.get());
        assertFalse(StorageTransfer.items(storage, output, notifications::incrementAndGet));
        assertEquals(1, notifications.get());
    }

    @Test void partialRealInsertionReturnsRemainderToInput() {
        ItemStackHandler input = new ItemStackHandler(1);
        input.setStackInSlot(0, new ItemStack(Items.APPLE, 20));
        ItemStackHandler inconsistent = new ItemStackHandler(1) {
            @Override public int getSlotLimit(int slot) { return 5; }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return simulate ? ItemStack.EMPTY : super.insertItem(slot, stack, false);
            }
        };
        assertTrue(StorageTransfer.items(input, inconsistent));
        assertEquals(15, input.getStackInSlot(0).getCount());
        assertEquals(5, inconsistent.getStackInSlot(0).getCount());
    }

    @Test void internalAccessSupportsNativeHatchSlotRestrictions() {
        IItemHandlerImpl input = new IItemHandlerImpl(new int[]{0}, new int[]{});
        IItemHandlerImpl output = new IItemHandlerImpl(new int[]{}, new int[]{0});
        input.insertItem(0, new ItemStack(Items.APPLE, 12), false);
        assertTrue(input.extractItem(0, 12, true).isEmpty());
        assertFalse(StorageTransfer.items(input.asGUIAccess(), output.asGUIAccess()));
        assertTrue(input.getStackInSlot(0).isEmpty());
        assertEquals(12, output.getStackInSlot(0).getCount());
    }

    @Test void fluidFillsAllMatchingAndEmptyTanksWithoutMutatingSimulation() {
        StorageFluidHandler storage = fluids(3, 1000);
        storage.fill(new FluidStack(FluidRegistry.WATER, 900), true);
        assertEquals(2100, storage.fill(new FluidStack(FluidRegistry.WATER, 2500), false));
        assertEquals(900, storage.getTank(0).getFluidAmount());
        assertEquals(0, storage.getTank(1).getFluidAmount());
        assertEquals(2100, storage.fill(new FluidStack(FluidRegistry.WATER, 2500), true));
        assertEquals(3000, storage.drain(new FluidStack(FluidRegistry.WATER, 4000), false).amount);
    }

    @Test void fluidOverflowRemainsInInputAndClearsWhenSpaceReturns() {
        FluidTank input = new FluidTank(new FluidStack(FluidRegistry.WATER, 2500), 3000);
        StorageFluidHandler storage = fluids(2, 1000);
        assertTrue(StorageTransfer.fluids(input, storage));
        assertEquals(500, input.getFluidAmount());
        assertEquals(2000, storage.drain(new FluidStack(FluidRegistry.WATER, 2000), true).amount);
        assertFalse(StorageTransfer.fluids(input, storage));
        assertEquals(0, input.getFluidAmount());
        assertEquals(500, storage.getTank(0).getFluidAmount());
    }

    @Test void blockedFirstFluidDoesNotStarveLaterTypes() {
        StorageFluidHandler storage = fluids(2, 1000);
        storage.getTank(0).fill(new FluidStack(FluidRegistry.WATER, 800), true);
        storage.getTank(1).fill(new FluidStack(FluidRegistry.LAVA, 600), true);
        FluidTank output = new FluidTank(new FluidStack(FluidRegistry.LAVA, 200), 1000);
        assertTrue(StorageTransfer.fluids(storage, output));
        assertEquals(800, storage.getTank(0).getFluidAmount());
        assertEquals(0, storage.getTank(1).getFluidAmount());
        assertEquals(800, output.getFluidAmount());
    }

    @Test void fluidTagsRemainSeparateAndSurviveTransferAndNbt() {
        FluidStack tagged = new FluidStack(FluidRegistry.WATER, 700);
        tagged.tag = new NBTTagCompound();
        tagged.tag.setString("test", "special");
        StorageFluidHandler storage = fluids(2, 1000);
        storage.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        FluidTank input = new FluidTank(tagged, 1000);
        assertFalse(StorageTransfer.fluids(input, storage));
        StorageFluidHandler restored = fluids(2, 1000);
        restored.deserializeNBT(storage.serializeNBT());
        assertNull(restored.getTank(0).getFluid().tag);
        assertEquals("special", restored.getTank(1).getFluid().tag.getString("test"));
        assertEquals(700, restored.getTank(1).getFluidAmount());
    }

    @Test void fluidPartialRealFillRestoresRejectedRemainder() {
        FluidTank input = new FluidTank(new FluidStack(FluidRegistry.WATER, 1000), 1000);
        FluidTank output = new FluidTank(300) {
            @Override public int fill(FluidStack resource, boolean doFill) {
                return doFill ? super.fill(resource, true) : resource.amount;
            }
        };
        assertTrue(StorageTransfer.fluids(input, output));
        assertEquals(700, input.getFluidAmount());
        assertEquals(300, output.getFluidAmount());
    }

    private static StorageFluidHandler fluids(int slots, int capacity) {
        return new StorageFluidHandler(new TileEntity() { }, slots, capacity);
    }
}
