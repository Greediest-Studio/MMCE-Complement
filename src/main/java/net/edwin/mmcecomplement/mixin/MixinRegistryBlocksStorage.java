package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.registry.RegistryBlocks;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.registry.StorageControllerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Filters CraftTweaker pre-load storage variants from MMCE's second list pass. */
@Mixin(value = RegistryBlocks.class, remap = false)
public abstract class MixinRegistryBlocksStorage {
    /** Filter the already-built wait list without competing with ECO AE's
     * redirect on MachineRegistry.getWaitForLoadMachines(). */
    @Redirect(method = "registerCustomControllers", at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/machine/MachineRegistry;"
            + "getWaitForLoadMachines()Ljava/util/List;", remap = false), remap = false)
    private static List<DynamicMachine> mmceComplement$filterStorageList() {
        List<DynamicMachine> machines = MachineRegistry.getWaitForLoadMachines();
        machines.removeIf(StorageControllerRegistry::isStorage);
        return machines;
    }

    @Redirect(method = "registerCustomControllers", at = @At(value = "INVOKE",
        target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), remap = false)
    private static boolean mmceComplement$filterStoragePreload(List list, Object value) {
        return StorageControllerRegistry.filterMachine(list, value)
            || list.add(value);
    }
}

