package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.client.gui.widget.Button4State;
import github.kasuminova.mmce.client.gui.widget.Button5State;
import github.kasuminova.mmce.client.gui.widget.base.DynamicWidget;
import github.kasuminova.mmce.client.gui.widget.impl.preview.MachineStructurePreviewPanel;
import github.kasuminova.mmce.client.gui.widget.impl.preview.WorldSceneRendererWidget;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentPreviewRenderer;
import net.edwin.mmcecomplement.client.gui.widget.PreviewIconButton4State;
import net.edwin.mmcecomplement.client.gui.widget.PreviewIconButton5State;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Mixin(value = MachineStructurePreviewPanel.class, remap = false)
public abstract class MixinMachineStructurePreviewPanel {

    @Unique
    private static final ResourceLocation mmceComplement$MODULE_BUTTON_TEXTURE = new ResourceLocation(
        "mmce_complement", "textures/gui/attachment_module_button.png");

    @Unique
    private static final ResourceLocation mmceComplement$MERGE_BUTTON_TEXTURE = new ResourceLocation(
        "mmce_complement", "textures/gui/attachment_merge_button.png");

    @Shadow
    @Final
    protected WorldSceneRendererWidget renderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mmceComplement$addAttachmentButtons(DynamicMachine machine, CallbackInfo ci) {
        if (((AttachmentMachine) (Object) machine).mmceComplement$getAttachmentModules().isEmpty()) {
            return;
        }

        AttachmentPreviewRenderer preview = (AttachmentPreviewRenderer) renderer;
        Button4State moduleButton = new PreviewIconButton4State();
        moduleButton.setTextureLocation(mmceComplement$MODULE_BUTTON_TEXTURE)
            .setTooltipFunction(btn -> Collections.singletonList(I18n.format(
                "gui.mmce_complement.preview.current_module",
                preview.mmceComplement$getPreviewModule())))
            .setWidthHeight(13, 13);

        Button5State mergeButton = new PreviewIconButton5State();
        mergeButton.setTextureLocation(mmceComplement$MERGE_BUTTON_TEXTURE)
            .setTooltipFunction(btn -> Collections.singletonList(I18n.format(
                preview.mmceComplement$isMergingPreviewParents()
                    ? "gui.mmce_complement.preview.merge_parents.disable"
                    : "gui.mmce_complement.preview.merge_parents.enable")))
            .setWidthHeight(13, 13);

        int originalButtonCount = machine.getDynamicPatterns().isEmpty() ? 4 : 6;
        int originalMenuLeft = 184 - (originalButtonCount * 15 + 6);
        moduleButton.setAbsXY(originalMenuLeft - 15, 161);
        mergeButton.setAbsXY(originalMenuLeft - 30, 161).setEnabled(false);

        moduleButton.setOnClickedListener(btn -> {
            preview.mmceComplement$showNextPreviewModule();
            mergeButton.setClicked(false);
            mergeButton.setEnabled(preview.mmceComplement$previewModuleHasParents());
        });
        mergeButton.setOnClickedListener(btn ->
            preview.mmceComplement$setMergePreviewParents(mergeButton.isClicked()));

        MachineStructurePreviewPanel panel = (MachineStructurePreviewPanel) (Object) this;
        List<DynamicWidget> widgets = panel.getWidgets();
        int rendererIndex = widgets.indexOf(renderer);
        widgets.add(rendererIndex, mergeButton);
        widgets.add(rendererIndex + 1, moduleButton);
    }
}
