package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.config.Actionable;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.api.ApiStorage;
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
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IFMLSidedHandler;
import net.minecraftforge.fml.relauncher.Side;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class StorageMEInventoryTest {
    private static IItemStorageChannel items;
    private static IFluidStorageChannel fluids;
    @BeforeAll static void bootstrap() throws Exception {
        Bootstrap.register();
        // AE's comparison utilities cache the installation side at class load.
        // Initialize that utility under a temporary server-side Forge delegate.
        Field delegate = FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        delegate.setAccessible(true);
        Object original = delegate.get(FMLCommonHandler.instance());
        try {
            if (original == null) delegate.set(FMLCommonHandler.instance(), Proxy.newProxyInstance(
                IFMLSidedHandler.class.getClassLoader(), new Class<?>[] { IFMLSidedHandler.class },
                (proxy, method, args) -> method.getName().equals("getSide") ? Side.SERVER : null));
            Class.forName("appeng.util.Platform");
        } finally { delegate.set(FMLCommonHandler.instance(), original); }
        ApiStorage storage = new ApiStorage();
        items = storage.getStorageChannel(IItemStorageChannel.class);
        fluids = storage.getStorageChannel(IFluidStorageChannel.class);
    }
    private static IAEItemStack diamonds(long amount) {
        return items.createStack(new ItemStack(Items.DIAMOND)).setStackSize(amount);
    }
    private static IAEFluidStack water(long amount, String tag) {
        FluidStack stack = new FluidStack(FluidRegistry.WATER, 1);
        if (tag != null) { stack.tag = new NBTTagCompound(); stack.tag.setString("kind", tag); }
        return fluids.createStack(stack).setStackSize(amount);
    }
    private static StorageFluidHandler tanks(int slots, int capacity) {
        return new StorageFluidHandler(new TileEntity() { }, slots, capacity);
    }
    @Test void itemSimulationAndPartialTransfersNeverModifyRequestOrLoseOverflow() {
        ItemStackHandler backing = new ItemStackHandler(2);
        StorageMEItemInventory inventory = new StorageMEItemInventory(items, () -> backing);
        IAEItemStack request = diamonds(200);
        assertEquals(72, inventory.injectItems(request, Actionable.SIMULATE, null).getStackSize());
        assertTrue(backing.getStackInSlot(0).isEmpty());
        assertTrue(inventory.pollChanges().isEmpty());
        assertEquals(72, inventory.injectItems(request, Actionable.MODULATE, null).getStackSize());
        assertEquals(200, request.getStackSize());
        assertEquals(128, inventory.getAvailableItems(items.createList()).getFirstItem().getStackSize());
        assertTrue(inventory.pollChanges().isEmpty(), "AE already announced the insertion");
        assertEquals(100, inventory.extractItems(diamonds(100), Actionable.SIMULATE, null).getStackSize());
        assertEquals(64, backing.getStackInSlot(0).getCount());
        assertEquals(100, inventory.extractItems(diamonds(100), Actionable.MODULATE, null).getStackSize());
        assertEquals(28, backing.getStackInSlot(1).getCount());
        assertTrue(inventory.pollChanges().isEmpty(), "AE already announced the extraction");
        assertEquals(28, inventory.extractItems(diamonds(Long.MAX_VALUE), Actionable.MODULATE, null).getStackSize());
        assertNull(inventory.extractItems(diamonds(1), Actionable.MODULATE, null));
    }
    @Test void externalChangesAreReportedExactlyOnceIncludingBeforeAnAeTransfer() {
        ItemStackHandler backing = new ItemStackHandler(2);
        StorageMEItemInventory inventory = new StorageMEItemInventory(items, () -> backing);
        backing.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        assertNull(inventory.injectItems(diamonds(10), Actionable.MODULATE, null));
        assertEquals(20, inventory.pollChanges().get(0).getStackSize());
        assertTrue(inventory.pollChanges().isEmpty());
        backing.extractItem(0, 7, false);
        assertEquals(-7, inventory.pollChanges().get(0).getStackSize());
        assertTrue(inventory.pollChanges().isEmpty());
    }
    @Test void unavailableStorageIsSealedAndReappearsWithoutChangingItsContents() {
        ItemStackHandler backing = new ItemStackHandler(1);
        backing.setStackInSlot(0, new ItemStack(Items.DIAMOND, 37));
        AtomicBoolean visible = new AtomicBoolean(true);
        StorageMEItemInventory inventory = new StorageMEItemInventory(items, () -> visible.get() ? backing : null);
        inventory.resetSnapshot();
        visible.set(false); // missing structure, power, channel, or elected port
        assertEquals(-37, inventory.pollChanges().get(0).getStackSize());
        assertTrue(inventory.getAvailableItems(items.createList()).isEmpty());
        assertFalse(inventory.canAccept(diamonds(1)));
        assertEquals(3, inventory.injectItems(diamonds(3), Actionable.MODULATE, null).getStackSize());
        assertNull(inventory.extractItems(diamonds(37), Actionable.MODULATE, null));
        assertEquals(37, backing.getStackInSlot(0).getCount());
        visible.set(true);
        assertEquals(37, inventory.pollChanges().get(0).getStackSize());
        assertTrue(inventory.pollChanges().isEmpty());
    }
    @Test void inventoryReplacementAndNbtIdentityAreRespected() {
        ItemStackHandler old = new ItemStackHandler(1);
        AtomicReference<ItemStackHandler> backing = new AtomicReference<>(old);
        StorageMEItemInventory inventory = new StorageMEItemInventory(items, backing::get);
        ItemStack tagged = new ItemStack(Items.DIAMOND, 12);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound().setString("kind", "special");
        old.setStackInSlot(0, tagged);
        assertNull(inventory.extractItems(diamonds(12), Actionable.MODULATE, null));
        assertEquals(12, inventory.pollChanges().get(0).getStackSize());
        ItemStackHandler expanded = new ItemStackHandler(2);
        expanded.setStackInSlot(0, tagged.copy());
        expanded.setStackInSlot(1, new ItemStack(Items.DIAMOND, 30));
        backing.set(expanded);
        assertEquals(30, inventory.pollChanges().get(0).getStackSize());
        assertEquals(30, inventory.extractItems(diamonds(50), Actionable.MODULATE, null).getStackSize());
        assertEquals(12, old.getStackInSlot(0).getCount());
        assertEquals("special", expanded.getStackInSlot(0).getTagCompound().getString("kind"));
    }
    @Test void fluidTransfersUseMillibucketsAndRespectTagsAndSimulation() {
        StorageFluidHandler backing = tanks(2, 1000);
        StorageMEFluidInventory inventory = new StorageMEFluidInventory(fluids, () -> backing);
        IAEFluidStack request = water(2500, "pure");
        assertEquals(500, inventory.injectItems(request, Actionable.SIMULATE, null).getStackSize());
        assertNull(backing.getTank(0).getFluid());
        assertEquals(500, inventory.injectItems(request, Actionable.MODULATE, null).getStackSize());
        assertEquals(2500, request.getStackSize());
        assertEquals(2000, inventory.getAvailableItems(fluids.createList()).getFirstItem().getStackSize());
        assertTrue(inventory.pollChanges().isEmpty());
        assertNull(inventory.extractItems(water(1000, "other"), Actionable.MODULATE, null));
        assertEquals(1500, inventory.extractItems(water(1500, "pure"), Actionable.SIMULATE, null).getStackSize());
        assertEquals(1000, backing.getTank(0).getFluidAmount());
        assertEquals(1500, inventory.extractItems(water(1500, "pure"), Actionable.MODULATE, null).getStackSize());
        assertEquals(500, backing.getTank(1).getFluidAmount());
        assertTrue(inventory.pollChanges().isEmpty());
    }
    @Test void fluidTotalsAndAeRequestsLargerThanIntegerRangeDoNotOverflow() {
        StorageFluidHandler backing = tanks(2, Integer.MAX_VALUE);
        StorageMEFluidInventory inventory = new StorageMEFluidInventory(fluids, () -> backing);
        IAEFluidStack request = water(4294967294L, null);
        assertEquals(2147483647L, inventory.injectItems(request, Actionable.MODULATE, null).getStackSize());
        assertNull(inventory.injectItems(water(Integer.MAX_VALUE, null), Actionable.MODULATE, null));
        assertEquals(4294967294L, inventory.getAvailableItems(fluids.createList()).getFirstItem().getStackSize());
        assertTrue(inventory.pollChanges().isEmpty());
        assertEquals(2147483647L, inventory.extractItems(request, Actionable.MODULATE, null).getStackSize());
        assertEquals(2147483647L, inventory.getAvailableItems(fluids.createList()).getFirstItem().getStackSize());
    }
    @Test void externalFluidEditsAndSealingProducePositiveAndNegativeDeltas() {
        StorageFluidHandler backing = tanks(2, 1000);
        AtomicBoolean visible = new AtomicBoolean(true);
        StorageMEFluidInventory inventory = new StorageMEFluidInventory(fluids, () -> visible.get() ? backing : null);
        backing.fill(new FluidStack(FluidRegistry.WATER, 400), true);
        assertEquals(400, inventory.pollChanges().get(0).getStackSize());
        assertTrue(inventory.pollChanges().isEmpty());
        backing.drain(125, true);
        assertEquals(-125, inventory.pollChanges().get(0).getStackSize());
        visible.set(false);
        assertEquals(-275, inventory.pollChanges().get(0).getStackSize());
        assertFalse(inventory.canAccept(water(1, null)));
        assertNull(inventory.extractItems(water(500, null), Actionable.MODULATE, null));
        visible.set(true);
        assertEquals(275, inventory.pollChanges().get(0).getStackSize());
    }
}
