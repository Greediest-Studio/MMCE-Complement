package net.edwin.mmcecomplement.integration.crafttweaker;

import github.kasuminova.mmce.common.helper.IMachineController;
import net.edwin.mmcecomplement.tile.TileStorageController;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.edwin.mmcecomplement.variant.StorageItemHandler;
import net.edwin.mmcecomplement.variant.StorageFluidHandler;
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
        controller.fluidsEnabled = controller.gasesEnabled = true;
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
        @Override public int mmceComplement$getConfiguredStorageSlots() { return itemSlots; }
        @Override public ItemStackHandler mmceComplement$getStorageInventory() {
            assertNotNull(items, "Sealed inventory opened"); return items;
        }
        @Override public boolean mmceComplement$hasFluidStorage() { return fluidsEnabled; }
        @Override public int mmceComplement$getFluidStorageSlots() { return 3; }
        @Override public int mmceComplement$getFluidStorageCapacity() { return 1000; }
        @Override public IFluidHandler mmceComplement$getFluidStorage() { assertNotNull(fluids); return fluids; }
        @Override public IFluidHandler mmceComplement$getFluidTank(int slot) { assertNotNull(fluids); return fluids.getTankInteractionHandler(slot); }
        @Override public boolean mmceComplement$hasGasStorage() { return gasesEnabled; }
        @Override public int mmceComplement$getGasStorageSlots() { return 2; }
        @Override public int mmceComplement$getGasStorageCapacity() { return 2000; }
        @Override public Object mmceComplement$getGasStorage() { assertNotNull(gases); return gases; }
    }
}
