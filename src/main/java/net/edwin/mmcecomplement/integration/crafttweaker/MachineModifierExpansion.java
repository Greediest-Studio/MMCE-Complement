package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import net.edwin.mmcecomplement.machineconnection.MachineConnectionRegistry;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

/** Adds machine signal compatibility declarations to MMCE's MachineModifier API. */
@ZenRegister
@ZenExpansion("mods.modularmachinery.MachineModifier")
public final class MachineModifierExpansion {
    private MachineModifierExpansion() {
    }

    @ZenMethod
    public static void addMachineConnection(String machineId, String[] targetId) {
        MachineConnectionRegistry.addMachineConnection(machineId, targetId);
    }
}
