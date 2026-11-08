package net.edwin.mmcecomplement.integration.crafttweaker;

import github.kasuminova.mmce.common.helper.IMachineController;
import net.edwin.mmcecomplement.tile.TileStorageController;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.edwin.mmcecomplement.variant.StorageItemHandler;
import net.edwin.mmcecomplement.variant.StorageFluidHandler;
import net.edwin.mmcecomplement.variant.StorageCapacitySettings;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import stanhebben.zenscript.ZenModule;
import stanhebben.zenscript.IZenErrorLogger;
import stanhebben.zenscript.impl.GenericCompileEnvironment;
import stanhebben.zenscript.impl.GenericRegistry;
import stanhebben.zenscript.util.ZenPosition;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class MachineStorageControllerTest {
    private static FakeController current;
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    public static IMachineController controller() { return current; }

    @Test void subtypeConversionRecipeGuardsAndConfiguredCapacities() throws Exception {
        current = create();
        assertSame(current, MachineControllerExpansion.asStorageController(current));
        assertNull(MachineControllerExpansion.asStorageController(null));
        assertNull(current.getActiveRecipe());
        assertEquals(0, current.getActiveRecipeList().length);
        assertEquals(0, current.getRecipeThreadList().length);
        assertFalse(current.isWorking());
        current.setExtraThreadCount(99);
        assertEquals(0, current.getExtraThreadCount());
        current.addModifier("test", null);
        current.addPermanentModifier("test", null);
        current.removeModifier("test");
        current.removePermanentModifier("test");
        assertFalse(current.hasModifier("test"));
        assertEquals(4, current.getMaxItemSlots());
        assertEquals(3, current.getMaxFluidSlots());
        assertEquals(2, current.getMaxGasSlots());
        assertEquals(1000, current.getFluidSlotCapacity());
        assertEquals(2000, current.getGasSlotCapacity());
        current.fluidsEnabled = current.gasesEnabled = false;
        assertEquals(0, current.getMaxFluidSlots());
        assertEquals(0, current.getMaxGasSlots());
        assertEquals(0, current.getFluidSlotCapacity());
        assertEquals(0, current.getGasSlotCapacity());
    }

    @Test void idsAreUniqueAndStableAcrossOtherStorageConfigurationChanges() throws Exception {
        current = create();
        Set<Long> ids = new HashSet<>();
        for (String kind : new String[]{"item", "fluid", "gas"}) {
            int count = kind.equals("item") ? 4 : kind.equals("fluid") ? 3 : 2;
            for (int i = 0; i < count; i++) {
                long id = current.getSlotId(kind, i);
                assertTrue(ids.add(id));
                assertEquals(i, current.getSlotIndex(id));
                assertEquals(kind, current.getSlotType(id));
            }
        }
        long gasId = current.getSlotId("gas", 1);
        current.itemSlots = 1000;
        assertEquals(gasId, current.getSlotId("gas", 1));
        assertEquals(-1, current.getSlotId("unknown", 0));
        assertEquals(-1, current.getSlotId("item", -1));
        assertEquals(-1, current.getSlotIndex(Long.MAX_VALUE));
        assertNull(current.getSlotType(-1));
        assertNull(current.getItemBySlotId(gasId));
    }

    @Test void unavailableStructureCannotInitializeOrExposeInventory() throws Exception {
        current = create();
        assertFalse(current.isStorageAvailable());
        assertNull(current.getItemInSlot(0));
        assertNull(current.getFluidInSlot(0));
        assertNull(current.getGasInSlot(0));
        assertEquals(0, current.countItem(null));
        assertEquals(0, current.insertFluid(null, false));
        assertEquals(0, current.extractGas(null, false));
        assertFalse(current.setMaxItemSlots(8));
        assertFalse(current.setMaxFluidSlots(8));
        assertFalse(current.setMaxGasSlots(8));
        assertFalse(current.setFluidCapacityInSlot(0, 8000));
        assertFalse(current.setGasCapacityInSlot(0, 8000));
    }

    @Test void liveCapacityChangesPreserveHandlersRejectDestructiveShrinksAndKeepStableIds() throws Exception {
        current = live();
        Object items = current.items, fluids = current.fluids, gases = current.gases;
        long id = current.getSlotId("fluid", 1);
        assertTrue(current.setMaxItemSlots(8));
        assertTrue(current.setMaxFluidSlots(5));
        assertTrue(current.setMaxGasSlots(4));
        assertSame(items, current.items);
        assertSame(fluids, current.fluids);
        assertSame(gases, current.gases);
        assertEquals(8, current.getMaxItemSlots());
        assertEquals(5, current.getMaxFluidSlots());
        assertEquals(4, current.getMaxGasSlots());
        assertEquals(id, current.getSlotId("fluid", 1));
        current.items.setStackInSlot(7, new ItemStack(Items.DIAMOND, 2));
        current.fluids.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
        current.fluids.getTank(4).setFluid(new FluidStack(FluidRegistry.LAVA, 50));
        mekanism.api.gas.Gas gas = new mekanism.api.gas.Gas("dynamic_script_gas", 0xFFFFFF);
        current.gases.receiveGasInSlot(3, new mekanism.api.gas.GasStack(gas, 200), true);
        assertFalse(current.setMaxItemSlots(4));
        assertFalse(current.setMaxFluidSlots(3));
        assertFalse(current.setMaxGasSlots(2));
        assertEquals(2, current.items.getStackInSlot(7).getCount());
        assertEquals(50, current.fluids.getTank(4).getFluidAmount());
        assertEquals(200, current.gases.getAmount(3));
        assertFalse(current.setFluidCapacityInSlot(0, 999));
        assertFalse(current.setGasCapacityInSlot(3, 199));
        assertTrue(current.setFluidCapacityInSlot(0, 2000));
        assertTrue(current.setGasCapacityInSlot(3, 3000));
        assertEquals(2000, current.getFluidCapacityInSlot(0));
        assertEquals(1000, current.getFluidCapacityInSlot(1));
        assertEquals(3000, current.getGasCapacityInSlot(3));
        assertEquals(2000, current.getGasCapacityInSlot(0));
        assertEquals(2000, current.fluids.getTankProperties()[0].getCapacity());
        assertEquals(3000, current.gases.getTankInfo()[3].getMaxGas());
        int saves = current.saves;
        assertTrue(current.setMaxItemSlots(8));
        assertTrue(current.setFluidCapacityInSlot(0, 2000));
        assertEquals(saves, current.saves, "Repeated event calls must not continually dirty the chunk");
        assertFalse(current.setMaxItemSlots(0));
        assertFalse(current.setMaxGasSlots(4097));
        assertFalse(current.setFluidCapacityInSlot(-1, 500));
        assertFalse(current.setGasCapacityInSlot(4, 100));
        assertFalse(current.setFluidSlotCapacity(999));
        assertEquals(2000, current.getFluidCapacityInSlot(0));
        assertTrue(current.setFluidSlotCapacity(4000));
        assertTrue(current.setGasSlotCapacity(4000));
        assertEquals(4000, current.getFluidCapacityInSlot(0));
        assertEquals(4000, current.getFluidCapacityInSlot(4));
        assertTrue(current.setMaxFluidSlots(6));
        assertEquals(4000, current.fluids.getTank(5).getCapacity());
        current.fluids.getTank(4).setFluid(null);
        current.gases.drawGasInSlot(3, new mekanism.api.gas.GasStack(gas, 200), true);
        current.items.extractItem(7, 2, false);
        assertTrue(current.setMaxItemSlots(4));
        assertTrue(current.setMaxFluidSlots(3));
        assertTrue(current.setMaxGasSlots(2));
        assertEquals(id, current.getSlotId("fluid", 1));
        assertEquals(1000, current.fluids.getTank(0).getFluidAmount());
    }

    @Test void capacityChangesRespectThreadAndEnabledTypeGuards() throws Exception {
        current = live();
        current.fluidsEnabled = current.gasesEnabled = false;
        assertFalse(current.setMaxFluidSlots(8));
        assertFalse(current.setMaxGasSlots(8));
        assertFalse(current.setFluidSlotCapacity(2000));
        assertFalse(current.setGasSlotCapacity(2000));
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try { assertFalse(executor.submit(() -> current.setMaxItemSlots(8)).get()); }
        finally { executor.shutdownNow(); }
        assertEquals(4, current.getMaxItemSlots());
    }

    @Test void capacityOverridesAndTankContentsSurviveNbtRoundTrip() throws Exception {
        current = live();
        assertTrue(current.setMaxFluidSlots(4));
        assertTrue(current.setFluidCapacityInSlot(3, 8000));
        assertTrue(current.setGasCapacityInSlot(1, 9000));
        current.fluids.getTank(3).fill(new FluidStack(FluidRegistry.WATER, 7000), true);
        mekanism.api.gas.Gas gas = mekanism.api.gas.GasRegistry.register(new mekanism.api.gas.Gas("dynamic_saved_gas", 0xFFFFFF));
        current.gases.receiveGasInSlot(1, new mekanism.api.gas.GasStack(gas, 8500), true);
        StorageFluidHandler reloadedFluid = new StorageFluidHandler(current, 3, 1000);
        StorageGasHandler reloadedGas = new StorageGasHandler(current, 2, 2000);
        reloadedFluid.deserializeNBT(current.fluids.serializeNBT());
        reloadedGas.deserializeNBT(current.gases.serializeNBT());
        StorageCapacitySettings settings = new StorageCapacitySettings();
        settings.deserializeNBT(current.settings.serializeNBT());
        assertEquals(4, settings.slots("Fluid", 3));
        assertEquals(8000, settings.capacity("Fluid", 3, 1000));
        assertEquals(9000, settings.capacity("Gas", 1, 2000));
        assertEquals(4, reloadedFluid.getSlots());
        assertEquals(8000, reloadedFluid.getTank(3).getCapacity());
        assertEquals(7000, reloadedFluid.getTank(3).getFluidAmount());
        assertEquals(9000, reloadedGas.getCapacityInSlot(1));
        assertEquals(8500, reloadedGas.getAmount(1));
    }

    @Test void cachedFluidSlotViewsFollowResizeAndNbtInsteadOfDetachedTanks() throws Exception {
        current = live();
        IFluidHandler cached = current.fluids.getTankInteractionHandler(2);
        assertTrue(current.setMaxFluidSlots(2));
        assertEquals(0, cached.fill(new FluidStack(FluidRegistry.WATER, 1000), true));
        assertNull(cached.drain(1000, true));
        assertEquals(0, cached.getTankProperties().length);
        assertTrue(current.setMaxFluidSlots(3));
        assertTrue(current.setFluidCapacityInSlot(2, 5000));
        assertEquals(4500, cached.fill(new FluidStack(FluidRegistry.WATER, 4500), true));
        assertEquals(5000, cached.getTankProperties()[0].getCapacity());
        assertEquals(4500, current.fluids.getTank(2).getFluidAmount());
        net.minecraft.nbt.NBTTagCompound saved = current.fluids.serializeNBT();
        current.fluids.deserializeNBT(saved);
        assertEquals(4500, cached.drain(4500, true).amount);
        assertEquals(0, current.fluids.getTank(2).getFluidAmount());
    }

    @Test void sortingVariableCapacityTanksNeverTruncatesOrMovesCapacityOverrides() throws Exception {
        current = live();
        assertTrue(current.setFluidCapacityInSlot(0, 100));
        assertTrue(current.setFluidCapacityInSlot(1, 2000));
        assertTrue(current.setGasCapacityInSlot(0, 100));
        assertTrue(current.setGasCapacityInSlot(1, 3000));
        current.fluids.getTank(1).fill(new FluidStack(FluidRegistry.WATER, 1700), true);
        mekanism.api.gas.Gas gas = new mekanism.api.gas.Gas("dynamic_sorted_gas", 0xFFFFFF);
        current.gases.receiveGasInSlot(1, new mekanism.api.gas.GasStack(gas, 2800), true);
        current.fluids.sort();
        current.gases.sort();
        assertEquals(100, current.fluids.getTank(0).getFluidAmount());
        assertEquals(1600, current.fluids.getTank(1).getFluidAmount());
        assertEquals(100, current.gases.getAmount(0));
        assertEquals(2700, current.gases.getAmount(1));
        assertEquals(100, current.getFluidCapacityInSlot(0));
        assertEquals(2000, current.getFluidCapacityInSlot(1));
        assertEquals(100, current.getGasCapacityInSlot(0));
        assertEquals(3000, current.getGasCapacityInSlot(1));
        assertFalse(current.setFluidSlotCapacity(1000));
        assertEquals(100, current.getFluidCapacityInSlot(0));
        assertEquals(2000, current.getFluidCapacityInSlot(1));
        assertFalse(current.setGasSlotCapacity(1000));
        assertEquals(100, current.getGasCapacityInSlot(0));
        assertEquals(3000, current.getGasCapacityInSlot(1));
    }

    @Test void scriptWrappersShareLiveStorageAndNotifyOnlyForRealMutations() throws Exception {
        current = create();
        current.formed = true;
        net.minecraft.server.MinecraftServer server = allocate(net.minecraft.server.dedicated.DedicatedServer.class);
        Field thread = net.minecraft.server.MinecraftServer.class.getDeclaredField("serverThread");
        thread.setAccessible(true);
        thread.set(server, Thread.currentThread());
        net.minecraft.world.WorldServer world = allocate(net.minecraft.world.WorldServer.class);
        Field serverField = java.util.Arrays.stream(net.minecraft.world.WorldServer.class.getDeclaredFields())
            .filter(f -> f.getType() == net.minecraft.server.MinecraftServer.class).findFirst().get();
        serverField.setAccessible(true);
        serverField.set(world, server);
        current.setWorld(world);
        current.items = new StorageItemHandler(current, 4);
        current.fluids = new StorageFluidHandler(current, 3, 1000);
        current.gases = new StorageGasHandler(current, 2, 2000);
        assertTrue(current.isStorageAvailable());
        crafttweaker.api.item.IItemStack diamonds = CraftTweakerMC.getIItemStack(new ItemStack(Items.DIAMOND, 2));
        assertEquals(2, current.insertItem(diamonds, true));
        assertEquals(0, current.changes);
        assertEquals(2, current.insertItem(diamonds, false));
        assertEquals(2, current.countItem(diamonds));
        assertEquals(2, current.getItemBySlotId(current.getSlotId("item", 0)).getAmount());
        assertEquals(2, current.extractItem(diamonds, false));
        assertFalse(current.hasItem(diamonds));
        crafttweaker.api.liquid.ILiquidStack water = CraftTweakerMC.getILiquidStack(new FluidStack(FluidRegistry.WATER, 1200));
        assertEquals(1000, current.insertFluidInSlot(1, water, false));
        assertEquals(1000, current.countFluid(water));
        assertEquals(1000, current.getFluidInSlot(1).getAmount());
        assertEquals(1000, current.extractFluid(water, false));
        assertEquals(4, current.changes);
        assertEquals(4, current.saves);
        assertEquals(4, current.syncs);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            assertEquals(0, executor.submit(() -> current.insertItem(diamonds, false)).get().intValue());
        } finally { executor.shutdownNow(); }
        assertEquals(4, current.changes);
        current.formed = false;
        assertEquals(0, current.insertItem(diamonds, false));
        assertNull(current.getFluidInSlot(1));
    }

    @Test void realZenScriptCompilesSubtypeInheritanceExpansionAndOptionalArguments() throws Exception {
        current = create();
        List<String> errors = new ArrayList<>();
        IZenErrorLogger logger = new IZenErrorLogger() {
            @Override public void error(ZenPosition pos, String message) { errors.add(pos + ": " + message); }
            @Override public void error(String message) { errors.add(message); }
            @Override public void error(String message, Throwable cause) { errors.add(message + ": " + cause); }
            @Override public void warning(ZenPosition pos, String message) { }
            @Override public void info(ZenPosition pos, String message) { }
            @Override public void warning(String message) { }
            @Override public void info(String message) { }
        };
        GenericCompileEnvironment environment = new GenericCompileEnvironment();
        GenericRegistry registry = new GenericRegistry(environment, logger);
        environment.setRegistry(registry);
        registry.registerNativeClass(IMachineController.class);
        registry.registerNativeClass(IMachineStorageController.class);
        registry.registerExpansion(MachineControllerExpansion.class);
        registry.registerGlobal("testController", registry.getStaticFunction(
            MachineStorageControllerTest.class, "controller"));
        String source = "import mods.modularmachinery.IMachineController;\n"
            + "import mods.modularmachinery.IMachineStorageController;\n"
            + "val c as IMachineStorageController = testController().asStorageController();\n"
            + "val base as IMachineController = c;\n"
            + "val slots = c.maxItemSlots + c.maxFluidSlots + c.maxGasSlots;\n"
            + "val capacities = c.fluidSlotCapacity + c.gasSlotCapacity;\n"
            + "val resized = c.setMaxItemSlots(8) || c.setMaxFluidSlots(8) || c.setMaxGasSlots(8);\n"
            + "val resizedFluid = c.setFluidCapacityInSlot(0, 2000);\n"
            + "val resizedGas = c.setGasCapacityInSlot(0, 3000);\n"
            + "val allFluid = c.setFluidSlotCapacity(4000); val allGas = c.setGasSlotCapacity(4000);\n"
            + "val recipe = c.activeRecipe;\n"
            + "val threads = c.recipeThreadList;\n"
            + "val n = c.extraThreadCount; c.extraThreadCount = 2;\n"
            + "val working = c.isWorking; val available = c.storageAvailable;\n"
            + "val id = c.getSlotId(\"gas\", 1);\n"
            + "val kind = c.getSlotType(id); val index = c.getSlotIndex(id);\n"
            + "val item = c.getItemInSlot(0); val fluid = c.getFluidInSlot(0); val gas = c.getGasInSlot(0);\n"
            + "val a = c.insertItem(item); val b = c.extractItem(item, true);\n"
            + "val d = c.insertFluidInSlot(0, fluid); val e = c.extractFluid(fluid, true);\n"
            + "val f = c.insertGas(gas); val g = c.extractGasInSlot(0, gas, true);\n"
            + "val present = c.hasItem(item) || c.hasFluid(fluid) || c.hasGas(gas);\n";
        // Use the compiler directly to avoid ZenModule's unconditional "generated/" debug dump.
        java.util.Map<String, byte[]> bytecode = new java.util.HashMap<>();
        stanhebben.zenscript.compiler.IEnvironmentGlobal global = registry.makeGlobalEnvironment(bytecode);
        stanhebben.zenscript.ZenTokener tokens = new stanhebben.zenscript.ZenTokener(
            new java.io.StringReader(source), environment, "storage_api_test.zs", false);
        stanhebben.zenscript.ZenParsedFile parsed = new stanhebben.zenscript.ZenParsedFile(
            "storage_api_test.zs", "StorageApiTest", tokens, global);
        ZenModule.compileScripts("storage_api_test.zs", java.util.Collections.singletonList(parsed), global, false);
        ZenModule module = new ZenModule(bytecode, getClass().getClassLoader());
        assertTrue(errors.isEmpty(), errors.toString());
        assertNotNull(module);
        assertDoesNotThrow(() -> module.getMain().run());
    }

    private static FakeController create() throws Exception {
        FakeController controller = allocate(FakeController.class);
        controller.itemSlots = 4;
        controller.settings = new StorageCapacitySettings();
        controller.fluidsEnabled = controller.gasesEnabled = true;
        return controller;
    }

    private static FakeController live() throws Exception {
        FakeController controller = create();
        controller.formed = true;
        net.minecraft.server.MinecraftServer server = allocate(net.minecraft.server.dedicated.DedicatedServer.class);
        Field thread = net.minecraft.server.MinecraftServer.class.getDeclaredField("serverThread");
        thread.setAccessible(true);
        thread.set(server, Thread.currentThread());
        net.minecraft.world.WorldServer world = allocate(net.minecraft.world.WorldServer.class);
        Field serverField = java.util.Arrays.stream(net.minecraft.world.WorldServer.class.getDeclaredFields())
            .filter(f -> f.getType() == net.minecraft.server.MinecraftServer.class).findFirst().get();
        serverField.setAccessible(true);
        serverField.set(world, server);
        controller.setWorld(world);
        controller.items = new StorageItemHandler(controller, 4);
        controller.fluids = new StorageFluidHandler(controller, 3, 1000);
        controller.gases = new StorageGasHandler(controller, 2, 2000);
        return controller;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    public static class FakeController extends TileStorageController implements StorageController {
        int itemSlots;
        StorageCapacitySettings settings;
        boolean fluidsEnabled, gasesEnabled;
        boolean formed;
        int changes, saves, syncs;
        ItemStackHandler items;
        StorageFluidHandler fluids;
        StorageGasHandler gases;
        @Override public boolean isStructureFormed() { return formed; }
        @Override public void markDirty() { saves++; }
        @Override public void markForUpdateSync() { syncs++; }
        @Override public void mmceComplement$markStorageChanged() { changes++; }
        @Override public boolean mmceComplement$isStorageController() { return true; }
        @Override public int mmceComplement$getStorageSlots() { return 0; }
        @Override public StorageCapacitySettings mmceComplement$getCapacitySettings() { return settings; }
        @Override public int mmceComplement$getConfiguredStorageSlots() { return settings.slots("Item", itemSlots); }
        @Override public ItemStackHandler mmceComplement$getStorageInventory() {
            assertNotNull(items, "Sealed inventory opened"); return items;
        }
        @Override public boolean mmceComplement$hasFluidStorage() { return fluidsEnabled; }
        @Override public int mmceComplement$getFluidStorageSlots() { return settings.slots("Fluid", 3); }
        @Override public int mmceComplement$getFluidStorageCapacity() { return settings.capacity("Fluid", 1000); }
        @Override public IFluidHandler mmceComplement$getFluidStorage() { assertNotNull(fluids); return fluids; }
        @Override public IFluidHandler mmceComplement$getFluidTank(int slot) { assertNotNull(fluids); return fluids.getTankInteractionHandler(slot); }
        @Override public boolean mmceComplement$hasGasStorage() { return gasesEnabled; }
        @Override public int mmceComplement$getGasStorageSlots() { return settings.slots("Gas", 2); }
        @Override public int mmceComplement$getGasStorageCapacity() { return settings.capacity("Gas", 2000); }
        @Override public Object mmceComplement$getGasStorage() { assertNotNull(gases); return gases; }
    }
}
