package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.client.model.DynamicMachineModelRegistry;
import github.kasuminova.mmce.client.model.MachineControllerModel;
import github.kasuminova.mmce.client.renderer.MachineControllerRenderer;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModelMode;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders registered child models after MMCE has rendered the main model. */
@Mixin(value = MachineControllerRenderer.class, remap = false)
public abstract class MixinMachineControllerRenderer {

    @Shadow
    protected static void rotateBlock(EnumFacing facing) {
        throw new AssertionError();
    }

    @Inject(method = "render(Lhellfirepvp/modularmachinery/common/tiles/base/"
        + "TileMultiblockMachineController;DDDFI)V", at = @At("RETURN"))
    private void mmceComplement$renderAttachmentModels(
        TileMultiblockMachineController controller, double x, double y,
        double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null
            || !(((AttachmentMachine) (Object) machine)
                .mmceComplement$getAttachmentModelMode()
                == AttachmentModelMode.SEPARATE)) {
            return;
        }
        AttachmentMachine attachmentMachine = (AttachmentMachine) (Object) machine;
        java.util.Map<String, AttachmentModule> modules =
            attachmentMachine.mmceComplement$getAttachmentModules();
        if (modules.isEmpty()) {
            return;
        }
        MachineControllerRenderer renderer = (MachineControllerRenderer) (Object) this;
        net.edwin.mmcecomplement.attachment.AttachmentController state =
            (net.edwin.mmcecomplement.attachment.AttachmentController) (Object) controller;
        for (String id : state.mmceComplement$getActiveAttachmentModules()) {
            AttachmentModule module = modules.get(id);
            if (module == null) {
                continue;
            }
            String modelName = module.getModelName();
            if (modelName == null || modelName.isEmpty()) {
                modelName = machine.getRegistryName().getPath()
                    + "__module__" + id;
            }
            MachineControllerModel model =
                DynamicMachineModelRegistry.INSTANCE.getMachineModel(modelName);
            if (model == null && module.getModelName() != null) {
                model = DynamicMachineModelRegistry.INSTANCE.getMachineModel(
                    machine.getRegistryName().getPath()
                        + "__module__" + id);
            }
            if (model != null && model != controller.getCurrentModel()) {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(0.5F, 0.0F, 0.5F);
                    rotateBlock(controller.getControllerRotation());
                    GlStateManager.translate(-0.5F, 0.0F, -0.5F);
                    renderer.renderDummy(controller, model);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        }
    }
}
