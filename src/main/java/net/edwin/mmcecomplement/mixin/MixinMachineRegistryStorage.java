package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.edwin.mmcecomplement.registry.StorageControllerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Mirrors ECO AE Extension's machine-list redirect: storage variants are
 * removed from MMCE's controller creation list after their own controller has
 * been installed in BlockController.MACHINE_CONTROLLERS.
 */
@Mixin(value = MachineRegistry.class, remap = false)
public abstract class MixinMachineRegistryStorage {
    @Redirect(method = "getWaitForLoadMachines", at = @At(value = "INVOKE",
        target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), remap = false)
    private static boolean mmceComplement$filterStorageMachine(List list, Object value) {
        return StorageControllerRegistry.filterMachine(list, value)
            || list.add(value);
    }
}

