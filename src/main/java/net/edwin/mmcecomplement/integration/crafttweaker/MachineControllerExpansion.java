package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.helper.IMachineController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** CraftTweaker access to the attachment definitions and active state of a controller. */
@ZenRegister
@ZenExpansion("mods.modularmachinery.IMachineController")
public final class MachineControllerExpansion {

    private MachineControllerExpansion() {
    }

    @ZenMethod
    public static boolean hasModule(IMachineController controller, String moduleId) {
        if (controller == null || moduleId == null) {
            return false;
        }
        String normalizedId = moduleId.trim();
        if (normalizedId.isEmpty()) {
            return false;
        }
        TileMultiblockMachineController tile = controller.getController();
        return tile instanceof AttachmentController
            && ((AttachmentController) tile).mmceComplement$isAttachmentModuleActive(normalizedId);
    }

    @ZenGetter("moduleList")
    public static String[] getModuleList(IMachineController controller) {
        if (controller == null || controller.getController() == null) {
            return new String[0];
        }
        DynamicMachine machine = controller.getController().getFoundMachine();
        if (machine == null || !(machine instanceof AttachmentMachine)) {
            return new String[0];
        }
        return ((AttachmentMachine) (Object) machine).mmceComplement$getAttachmentModules()
            .keySet().toArray(new String[0]);
    }
}
