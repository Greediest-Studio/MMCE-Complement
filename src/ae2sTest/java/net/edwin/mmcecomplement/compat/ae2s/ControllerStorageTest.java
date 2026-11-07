package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.config.Actionable;
import ae2.api.stacks.*;
import ae2.api.storage.*;
import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.variant.StorageFluidHandler;
import net.edwin.mmcecomplement.compat.ae.tile.TileStorageInteractionAssembly;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.*;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ControllerStorageTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void hostSaveCallbacksDoNotTouchWorldDuringLoading() {
        final int[] saves = { 0 };
        TileStoragePort port = new TileStoragePort() {
            @Override public void markDirty() { saves[0]++; }
        };
        port.markForSave();
        port.partChanged();
        assertEquals(0, saves[0], "AE2S host callbacks must also defer world access");
    }

    @Test void nativeMonitorRefreshesAeTransfersAndExternalChanges() throws Exception {
        ItemStackHandler items = new ItemStackHandler(2);
        StorageFluidHandler fluids = new StorageFluidHandler(new TileEntity() {}, 2, 8000);
        StorageController controller = (StorageController) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {StorageController.class}, (proxy, method, args) -> {
                if (method.getName().equals("mmceComplement$getStorageInventory")) return items;
                if (method.getName().equals("mmceComplement$getFluidStorage")) return fluids;
                return null;
            });
        AtomicReference<StorageController> binding = new AtomicReference<>(controller);
        ControllerStorage storage = new ControllerStorage(binding::get);
        // Exercise the actual installed branch's native monitor, not a local copy.
        Class<?> type = Class.forName(StorageBusSettings.class.getName() + "$ControllerInventory");
        Constructor<?> ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);
        MEStorageMonitor monitor = (MEStorageMonitor) ctor.newInstance();
        Method bind = type.getDeclaredMethod("bind", MEStorage.class);
        bind.setAccessible(true);
        bind.invoke(monitor, storage);
        final KeyCounter terminal = new KeyCounter();
        monitor.addListener(new MEStorageChangeListener() {
            @Override public boolean isValid(Object token) { return true; }
            @Override public void onStackChange(AEKey key, long delta) { terminal.add(key, delta); }
            @Override public void onListUpdate() { terminal.reset(); monitor.getAvailableStacks(terminal); }
        }, this);
        AEItemKey diamond = AEItemKey.of(new ItemStack(Items.DIAMOND));
        AEFluidKey water = AEFluidKey.of(new FluidStack(FluidRegistry.WATER, 1));
        monitor.getAvailableStacks(terminal);
        for (int cycle = 0; cycle < 3; cycle++) {
            assertEquals(1, monitor.insert(diamond, 1, Actionable.MODULATE, null));
            assertEquals(1, terminal.get(diamond));
            assertEquals(1, monitor.extract(diamond, 1, Actionable.MODULATE, null));
            assertEquals(0, terminal.get(diamond), "No ghost after extraction through AE");
            assertEquals(0, monitor.extract(diamond, 1, Actionable.MODULATE, null));
        }
        assertEquals(1000, monitor.insert(water, 1000, Actionable.MODULATE, null));
        assertEquals(1000, terminal.get(water));
        items.insertItem(0, new ItemStack(Items.DIAMOND, 2), false);
        ((ae2.me.storage.ITickingMonitor) monitor).onTick();
        assertEquals(2, terminal.get(diamond));
        assertEquals(1000, monitor.extract(water, 1000, Actionable.MODULATE, null));
        assertEquals(0, terminal.get(water));
        assertEquals(2, monitor.extract(diamond, 2, Actionable.SIMULATE, null));
        assertEquals(2, items.getStackInSlot(0).getCount());
        binding.set(null);
        ((ae2.me.storage.ITickingMonitor) monitor).onTick();
        assertEquals(0, terminal.get(diamond), "Broken structures must withdraw their stock");
        assertEquals(2, items.getStackInSlot(0).getCount());
    }

    @Test void entrypointsLoadWithSupergiantAndWithoutUel() throws Exception {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("appeng.api.AEApi"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("me.ramidzkh.mekae2.ae2.AEGasKey"));
        assertNull(NativeGasStorageBridge.create(() -> null), "Native gas integration remains optional");
        for (String name : new String[] {
            "net.edwin.mmcecomplement.MMCEComplement",
            "net.edwin.mmcecomplement.event.RegistryEvents",
            "net.edwin.mmcecomplement.gui.GuiHandlerMMCE",
            "net.edwin.mmcecomplement.init.ModBlocks",
            "net.edwin.mmcecomplement.mechannel.ModMEChannelTypes",
            TileStoragePort.class.getName(), TileStorageInteractionAssembly.class.getName(),
            Ae2sStorageRegistration.class.getName()}) {
            Class<?> type = Class.forName(name, false, getClass().getClassLoader());
            assertDoesNotThrow(() -> type.getDeclaredMethods(), name);
        }
        assertNotNull(new TileStoragePort());
        TileStorageInteractionAssembly assembly = new TileStorageInteractionAssembly();
        assertFalse(assembly instanceof ae2.api.networking.IInWorldGridNodeHost);
        assertFalse(assembly instanceof ae2.api.parts.IPartHost);
    }
}
