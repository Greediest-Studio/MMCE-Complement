package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.variant.StorageFluidHandler;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE.StorageTankMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class StorageLayoutTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void switchingBetweenDifferentHeightsMovesAllActualInventorySlots() throws Exception {
        TestStorage storage = new TestStorage();
        ContainerStorage container = container(storage);
        assertInventoryAligned(container, 6);
        container.setMode(ContainerStorage.MODE_FLUIDS);
        assertInventoryAligned(container, 1);
        container.setMode(ContainerStorage.MODE_GASES);
        assertInventoryAligned(container, 2);
        container.setMode(ContainerStorage.MODE_ITEMS);
        assertInventoryAligned(container, 6);
        assertEquals(90, container.inventorySlots.size());
    }

    @Test void liveTankCountChangeMovesInventoryWithoutRebuildingSlotIds() throws Exception {
        TestStorage storage = new TestStorage();
        ContainerStorage container = container(storage);
        container.setMode(ContainerStorage.MODE_FLUIDS);
        Slot firstPlayerSlot = container.inventorySlots.get(ContainerStorage.PAGE_SIZE);
        storage.fluidSlots = 37;
        container.refreshLayout();
        assertInventoryAligned(container, 5);
        assertSame(firstPlayerSlot, container.inventorySlots.get(ContainerStorage.PAGE_SIZE));
    }

    @Test void tankHitboxesExcludePlayerInventoryBordersAndUnusedCells() throws Exception {
        TestStorage storage = new TestStorage();
        ContainerStorage container = container(storage);
        container.setMode(ContainerStorage.MODE_FLUIDS);
        int x = ContainerStorage.SLOT_X, y = ContainerStorage.STORAGE_TOP;
        assertEquals(0, container.getTankAt(x, y));
        assertEquals(-1, container.getTankAt(x - 3, y));
        assertEquals(-1, container.getTankAt(x + 162, y));
        assertEquals(-1, container.getTankAt(x, container.getPlayerInventoryTop()));
        storage.fluidSlots = 10;
        container.refreshLayout();
        assertEquals(9, container.getTankAt(x, y + 18));
        assertEquals(-1, container.getTankAt(x + 18, y + 18));
    }

    @Test void scrolledTankHitboxesUseFullIndicesBeyondOneByte() throws Exception {
        TestStorage storage = new TestStorage();
        storage.fluidSlots = 400;
        ContainerStorage container = container(storage);
        container.setMode(ContainerStorage.MODE_FLUIDS);
        container.setPage(30);
        assertEquals(270, container.getTankAt(ContainerStorage.SLOT_X, ContainerStorage.STORAGE_TOP));
        ByteBuf buffer = Unpooled.buffer();
        try {
            new StorageTankMessage(270).toBytes(buffer);
            StorageTankMessage received = new StorageTankMessage();
            received.fromBytes(buffer);
            assertEquals(270, received.tank);
        } finally {
            buffer.release();
        }
    }

    @Test void sortActionTargetsTheSelectedTabAndPreservesOtherStorage() throws Exception {
        TestStorage storage = new TestStorage();
        StorageFluidHandler fluids = new StorageFluidHandler(new TileEntity() { }, 9, 16000);
        StorageGasHandler gases = new StorageGasHandler(new TileEntity() { }, 18, 64000);
        Gas gas = new Gas("sort_route_gas", 0xFFFFFF);
        gases.receiveGas(null, new GasStack(gas, 64000), true);
        gases.receiveGas(null, new GasStack(gas, 500), true);
        gases.drawGas(new GasStack(gas, 64000), true);
        fluids.getTank(4).setFluid(new FluidStack(FluidRegistry.WATER, 500));
        storage.fluids = fluids;
        storage.gases = gases;
        storage.items.setStackInSlot(3, new ItemStack(Items.APPLE, 12));
        ContainerStorage container = container(storage);
        container.setMode(ContainerStorage.MODE_FLUIDS);
        container.sortStorage();
        assertEquals(500, fluids.getTank(0).getFluidAmount());
        assertNull(fluids.getTank(4).getFluid());
        assertEquals(12, storage.items.getStackInSlot(3).getCount());
        assertEquals(500, gases.getAmount(1));
        container.setMode(ContainerStorage.MODE_GASES);
        container.sortStorage();
        assertEquals(500, gases.getAmount(0));
        assertNull(gases.getStack(1));
        assertEquals(12, storage.items.getStackInSlot(3).getCount());
        assertEquals(500, fluids.getTank(0).getFluidAmount());
    }

    private static void assertInventoryAligned(ContainerStorage container, int rows) {
        assertEquals(rows, container.getVisibleRows());
        int top = ContainerStorage.STORAGE_TOP + rows * 18 + 14;
        for (int i = 0; i < 36; i++) {
            Slot slot = container.inventorySlots.get(ContainerStorage.PAGE_SIZE + i);
            assertEquals(ContainerStorage.SLOT_X + i % 9 * 18, slot.xPos);
            assertEquals(i < 27 ? top + i / 9 * 18 : top + 58, slot.yPos);
            assertEquals(ContainerStorage.PAGE_SIZE + i, slot.slotNumber);
        }
    }

    private static ContainerStorage container(TestStorage storage) throws Exception {
        // Only InventoryPlayer is needed: bypass the player constructor so
        // layout regressions can be tested without a server/world or mixins.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        EntityPlayer player = (EntityPlayer) unsafeType.getMethod("allocateInstance", Class.class)
            .invoke(field.get(null), EntityPlayerMP.class);
        player.inventory = new InventoryPlayer(player);
        return new ContainerStorage(storage, player) {
            @Override public boolean hasFluidTab() { return true; }
            @Override public boolean hasGasTab() { return true; }
            @Override public boolean canInteractWith(EntityPlayer ignored) { return true; }
        };
    }

    private static final class TestStorage implements StorageController {
        int fluidSlots = 9;
        private IFluidHandler fluids;
        private Object gases;
        private final ItemStackHandler items = new ItemStackHandler(54);
        @Override public boolean mmceComplement$isStorageController() { return true; }
        @Override public int mmceComplement$getStorageSlots() { return 54; }
        @Override public ItemStackHandler mmceComplement$getStorageInventory() { return items; }
        @Override public boolean mmceComplement$hasFluidStorage() { return true; }
        @Override public int mmceComplement$getFluidStorageSlots() { return fluidSlots; }
        @Override public int mmceComplement$getFluidStorageCapacity() { return 16000; }
        @Override public IFluidHandler mmceComplement$getFluidStorage() { return fluids; }
        @Override public IFluidHandler mmceComplement$getFluidTank(int index) { return null; }
        @Override public boolean mmceComplement$hasGasStorage() { return true; }
        @Override public int mmceComplement$getGasStorageSlots() { return 18; }
        @Override public int mmceComplement$getGasStorageCapacity() { return 64000; }
        @Override public Object mmceComplement$getGasStorage() { return gases; }
    }
}
