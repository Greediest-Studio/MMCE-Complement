package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneInterfaceRegistry;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneValueDefinition;
import net.minecraft.util.ResourceLocation;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** CraftTweaker builder for machine-scoped named redstone values. */
@ZenRegister
@ZenClass("mods.mmce_complement.RedstoneInterface")
public final class RedstoneInterface {

    private final ResourceLocation machineId;
    private final String name;
    private final int operator;
    private final String validationError;

    private RedstoneInterface(String machineId, String name, int operator) {
        ResourceLocation parsedMachineId = null;
        String parsedName = null;
        String error = null;
        try {
            parsedMachineId = RedstoneInterfaceRegistry.normalizeMachineId(machineId);
            parsedName = RedstoneInterfaceRegistry.normalizeName(name);
            if (!RedstoneValueDefinition.isValidOperator(operator)) {
                throw new IllegalArgumentException(
                    "operator must be 0 (maximum), 1 (minimum), or 2 (sum)");
            }
        } catch (RuntimeException ex) {
            error = ex.getMessage();
        }
        this.machineId = parsedMachineId;
        this.name = parsedName;
        this.operator = operator;
        this.validationError = error;
    }

    @ZenMethod
    public static RedstoneInterface newRedstone(String machineId, String name) {
        return new RedstoneInterface(machineId, name,
            RedstoneValueDefinition.OPERATOR_MAX);
    }

    @ZenMethod
    public static RedstoneInterface newRedstone(String machineId, String name,
                                                 int operator) {
        return new RedstoneInterface(machineId, name, operator);
    }

    @ZenMethod
    public void build() {
        if (validationError != null) {
            CraftTweakerAPI.logError("[MMCE Complement] Invalid redstone interface: "
                + validationError);
            return;
        }
        RedstoneValueDefinition definition =
            new RedstoneValueDefinition(name, operator);
        if (!RedstoneInterfaceRegistry.register(machineId, definition)) {
            CraftTweakerAPI.logError("[MMCE Complement] Redstone value '" + name
                + "' is already registered for machine '" + machineId + "'.");
        }
    }
}
