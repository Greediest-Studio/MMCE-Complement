package net.edwin.mmcecomplement.registry;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.item.ItemBlockController;
import net.edwin.mmcecomplement.block.BlockStorageController;
import net.edwin.mmcecomplement.variant.StorageMachine;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.reflect.Field;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Owns storage-variant controllers independently of MMCE's controller factory. */
public final class StorageControllerRegistry {
    private static final Map<DynamicMachine, BlockStorageController> CONTROLLERS =
        new IdentityHashMap<>();
    private static final List<ItemBlockController> ITEMS = new ArrayList<>();
    private static boolean blocksRegistered;
    private static boolean itemsRegistered;
    private static IForgeRegistry<Block> activeBlockRegistry;
    private static boolean configStorageIdsLoaded;
    private static final Set<String> CONFIG_STORAGE_IDS = new HashSet<>();
    private static final Pattern REGISTRY_NAME = Pattern.compile(
        "\\\"registryname\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern STORAGE_MARKER = Pattern.compile(
        "\\\"variant\\\"\\s*:\\s*\\\"storage\\\"|\\\"storage\\\"\\s*:",
        Pattern.CASE_INSENSITIVE);

    private StorageControllerRegistry() { }

    public static boolean isStorage(DynamicMachine machine) {
        if (machine instanceof StorageMachine
            && ((StorageMachine) (Object) machine).mmceComplement$isStorageVariant()) {
            return true;
        }
        loadConfigStorageIds();
        return machine != null && machine.getRegistryName() != null
            && CONFIG_STORAGE_IDS.contains(machine.getRegistryName().getPath());
    }

    /** Fallback for MMCE builds that deserialize the machine after block registration. */
    private static synchronized void loadConfigStorageIds() {
        if (configStorageIdsLoaded) return;
        configStorageIdsLoaded = true;
        File root = new File("config", "modularmachinery/machinery");
        if (!root.isDirectory()) return;
        try {
            Files.walk(root.toPath()).filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                        if (!STORAGE_MARKER.matcher(json).find()) return;
                        Matcher matcher = REGISTRY_NAME.matcher(json);
                        if (matcher.find()) {
                            String id = matcher.group(1);
                            int separator = id.indexOf(':');
                            CONFIG_STORAGE_IDS.add(separator < 0 ? id : id.substring(separator + 1));
                        }
                    } catch (Exception ignored) { }
                });
        } catch (Exception ignored) { }
    }

    public static synchronized void registerBlocks(IForgeRegistry<Block> registry) {
        if (blocksRegistered) return;
        activeBlockRegistry = registry;
        blocksRegistered = true;
        for (DynamicMachine machine : MachineRegistry.getWaitForLoadMachines()) {
            registerBlock(machine, registry);
        }
        // CraftTweaker machines are appended by MMCE immediately before its
        // controller loop. Prime them here when that map is exposed.
        try {
            Class<?> builder = Class.forName(
                "hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder");
            Field field = builder.getDeclaredField("PRE_LOAD_MACHINES");
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof Map) {
                for (Object entry : ((Map<?, ?>) value).values()) {
                    if (entry instanceof hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder) {
                        registerBlock(((hellfirepvp.modularmachinery.common.integration.crafttweaker.MachineBuilder) entry).getMachine(), registry);
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // CraftTweaker is optional.
        }
    }

    private static void registerBlock(DynamicMachine machine, IForgeRegistry<Block> registry) {
        if (!isStorage(machine) || CONTROLLERS.containsKey(machine)) return;
        BlockStorageController block = new BlockStorageController(machine);
        CONTROLLERS.put(machine, block);
        registry.register(block);
        BlockController.MACHINE_CONTROLLERS.put(machine, block);
        addMMCEModelEntry(block);
        addMMCEDynamicColorEntry(block);
    }

    public static synchronized void registerItems(IForgeRegistry<Item> registry) {
        if (itemsRegistered) return;
        itemsRegistered = true;
        for (BlockStorageController block : CONTROLLERS.values()) {
            ItemBlockController item = new ItemBlockController(block);
            item.setRegistryName(block.getRegistryName());
            item.setTranslationKey(block.getTranslationKey());
            item.setCreativeTab(hellfirepvp.modularmachinery.common.CommonProxy.creativeTabModularMachinery);
            registry.register(item);
            ITEMS.add(item);
            addMMCEDynamicItemEntry(item);
        }
    }

    public static Collection<BlockStorageController> getControllers() {
        return Collections.unmodifiableCollection(CONTROLLERS.values());
    }

    @SuppressWarnings("rawtypes")
    public static boolean filterMachine(List target, Object value) {
        if (!(value instanceof DynamicMachine)) return false;
        DynamicMachine machine = (DynamicMachine) value;
        if (!isStorage(machine)) return false;
        BlockStorageController block = CONTROLLERS.get(machine);
        if (block == null && activeBlockRegistry != null) {
            registerBlock(machine, activeBlockRegistry);
            block = CONTROLLERS.get(machine);
        }
        if (block == null) return false;
        BlockController.MACHINE_CONTROLLERS.put(machine, block);
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addMMCEModelEntry(Block block) {
        try {
            Field field = hellfirepvp.modularmachinery.common.registry.RegistryBlocks.class
                .getDeclaredField("blockModelRegister");
            field.setAccessible(true);
            ((List) field.get(null)).add(block);
        } catch (ReflectiveOperationException ignored) { }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addMMCEDynamicColorEntry(Block block) {
        try {
            Field field = hellfirepvp.modularmachinery.common.registry.RegistryBlocks.class
                .getDeclaredField("pendingIBlockColorBlocks");
            field.setAccessible(true);
            Object list = field.get(null);
            if (list instanceof List && !((List) list).contains(block)) ((List) list).add(block);
        } catch (ReflectiveOperationException ignored) { }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addMMCEDynamicItemEntry(Item item) {
        try {
            Field field = hellfirepvp.modularmachinery.common.registry.RegistryItems.class
                .getDeclaredField("pendingDynamicColorItems");
            field.setAccessible(true);
            Object list = field.get(null);
            if (list instanceof List && !((List) list).contains(item)) ((List) list).add(item);
        } catch (ReflectiveOperationException ignored) { }
    }
}


