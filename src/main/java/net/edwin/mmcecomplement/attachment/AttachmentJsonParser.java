package net.edwin.mmcecomplement.attachment;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Parses the complement-owned portion of a normal MMCE machine JSON. */
public final class AttachmentJsonParser {

    public static final String JSON_KEY = "modules";

    private AttachmentJsonParser() {
    }

    public static Map<String, AttachmentModule> parse(JsonObject root,
                                                       DynamicMachine owner,
                                                       JsonDeserializationContext context) {
        if (!root.has(JSON_KEY)) {
            return Collections.emptyMap();
        }
        JsonElement modulesElement = root.get(JSON_KEY);
        if (!modulesElement.isJsonArray()) {
            throw new JsonParseException("'modules' has to be an array of attachment module objects!");
        }

        LinkedHashMap<String, AttachmentModule> modules = new LinkedHashMap<>();
        JsonArray modulesArray = modulesElement.getAsJsonArray();
        for (JsonElement element : modulesArray) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("An element of 'modules' is not an object: " + element);
            }
            JsonObject moduleJson = element.getAsJsonObject();
            String id = requiredString(moduleJson, "id").trim();
            if (id.isEmpty()) {
                throw new JsonParseException("Attachment module id must not be empty!");
            }
            if (AttachmentResolver.MAIN.equals(id)) {
                throw new JsonParseException("Attachment module id 'main' is reserved for the main structure!");
            }
            if (modules.containsKey(id)) {
                throw new JsonParseException("Duplicate attachment module id '" + id + "'!");
            }
            if (!moduleJson.has("parts") || !moduleJson.get("parts").isJsonArray()) {
                throw new JsonParseException("Attachment module '" + id + "' is missing its native-format 'parts' array!");
            }

            JsonObject syntheticMachine = new JsonObject();
            syntheticMachine.addProperty("registryname", owner.getRegistryName() + "__module__" + id);
            syntheticMachine.addProperty("localizedname", id);
            // Gson 2.8.0 (the Minecraft 1.12 runtime version) does not expose
            // JsonElement#deepCopy publicly. Json trees have no parent pointer,
            // and MMCE only reads this value, so sharing it is safe here.
            syntheticMachine.add("parts", moduleJson.get("parts"));
            DynamicMachine parsedStructure = context.deserialize(syntheticMachine, DynamicMachine.class);

            Set<String> dependencies = moduleJson.has("depends-on")
                ? stringSet(moduleJson, "depends-on")
                : Collections.singleton(AttachmentResolver.MAIN);
            Set<String> conflicts = moduleJson.has("conflicts-with")
                ? stringSet(moduleJson, "conflicts-with")
                : Collections.emptySet();
            boolean asUpgrade = !moduleJson.has("as-upgrade") || requiredBoolean(moduleJson, "as-upgrade");
            modules.put(id, new AttachmentModule(
                id, parsedStructure.getPattern(), dependencies, conflicts, asUpgrade));
        }
        validateRelations(modules);
        validateDependencyCycles(modules);
        return modules;
    }

    private static void validateRelations(Map<String, AttachmentModule> modules) {
        for (AttachmentModule module : modules.values()) {
            for (String dependency : module.getDependencies()) {
                if (module.getId().equals(dependency)) {
                    throw new JsonParseException("Attachment module '" + module.getId() + "' cannot depend on itself!");
                }
                validateReference(module, dependency, "depends-on", modules);
            }
            for (String conflict : module.getConflicts()) {
                if (module.getId().equals(conflict)) {
                    throw new JsonParseException("Attachment module '" + module.getId() + "' cannot conflict with itself!");
                }
                validateReference(module, conflict, "conflicts-with", modules);
            }
        }
    }

    private static void validateReference(AttachmentModule module,
                                          String reference,
                                          String relation,
                                          Map<String, AttachmentModule> modules) {
        if (!AttachmentResolver.MAIN.equals(reference) && !modules.containsKey(reference)) {
            throw new JsonParseException("Attachment module '" + module.getId() + "' has unknown "
                + relation + " reference '" + reference + "'!");
        }
    }

    private static void validateDependencyCycles(Map<String, AttachmentModule> modules) {
        Set<String> visited = new LinkedHashSet<>();
        Set<String> visiting = new LinkedHashSet<>();
        for (AttachmentModule module : modules.values()) {
            visitDependency(module, modules, visited, visiting);
        }
    }

    private static void visitDependency(AttachmentModule module,
                                        Map<String, AttachmentModule> modules,
                                        Set<String> visited,
                                        Set<String> visiting) {
        if (visited.contains(module.getId())) {
            return;
        }
        if (!visiting.add(module.getId())) {
            throw new JsonParseException("Attachment module dependency cycle contains '"
                + module.getId() + "'!");
        }
        for (String dependency : module.getDependencies()) {
            AttachmentModule parent = modules.get(dependency);
            if (parent != null) {
                visitDependency(parent, modules, visited, visiting);
            }
        }
        visiting.remove(module.getId());
        visited.add(module.getId());
    }

    private static Set<String> stringSet(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (!value.isJsonArray()) {
            throw new JsonParseException("'" + key + "' has to be a string array!");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (JsonElement entry : value.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("'" + key + "' has to contain only strings!");
            }
            String relation = entry.getAsString().trim();
            if (relation.isEmpty()) {
                throw new JsonParseException("'" + key + "' must not contain an empty module id!");
            }
            result.add(relation);
        }
        return result;
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Attachment module field '" + key + "' has to be a string!");
        }
        return value.getAsString();
    }

    private static boolean requiredBoolean(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("Attachment module field '" + key + "' has to be a boolean!");
        }
        return value.getAsBoolean();
    }
}
