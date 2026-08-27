package net.edwin.mmcecomplement.redstoneinterface;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runtime definitions populated by CraftTweaker scripts. */
public final class RedstoneInterfaceRegistry {

    private static final Map<ResourceLocation, LinkedHashMap<String, RedstoneValueDefinition>>
        DEFINITIONS = new LinkedHashMap<>();

    private RedstoneInterfaceRegistry() {
    }

    public static ResourceLocation normalizeMachineId(String machineId) {
        if (machineId == null || machineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine id cannot be empty");
        }
        String normalized = machineId.trim();
        return normalized.indexOf(':') >= 0
            ? new ResourceLocation(normalized)
            : new ResourceLocation(ModularMachinery.MODID, normalized);
    }

    public static String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Redstone value name cannot be empty");
        }
        return name.trim();
    }

    public static synchronized boolean register(ResourceLocation machineId,
                                                RedstoneValueDefinition definition) {
        LinkedHashMap<String, RedstoneValueDefinition> machineDefinitions =
            DEFINITIONS.computeIfAbsent(machineId, ignored -> new LinkedHashMap<>());
        if (machineDefinitions.containsKey(definition.getName())) {
            return false;
        }
        machineDefinitions.put(definition.getName(), definition);
        return true;
    }

    @Nullable
    public static synchronized RedstoneValueDefinition get(ResourceLocation machineId,
                                                           String name) {
        Map<String, RedstoneValueDefinition> machineDefinitions = DEFINITIONS.get(machineId);
        return machineDefinitions == null ? null : machineDefinitions.get(name);
    }

    public static synchronized List<String> getNames(ResourceLocation machineId) {
        Map<String, RedstoneValueDefinition> machineDefinitions = DEFINITIONS.get(machineId);
        if (machineDefinitions == null || machineDefinitions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(machineDefinitions.keySet()));
    }

    public static synchronized void clear() {
        DEFINITIONS.clear();
    }
}
