package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.client.gui.widget.impl.preview.WorldSceneRendererWidget;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import net.edwin.mmcecomplement.attachment.AttachmentPatternResolver;
import net.edwin.mmcecomplement.attachment.AttachmentPreviewRenderer;
import net.edwin.mmcecomplement.attachment.AttachmentResolver;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = WorldSceneRendererWidget.class, remap = false)
public abstract class MixinWorldSceneRendererWidget implements AttachmentPreviewRenderer {

    @Shadow
    @Final
    protected DynamicMachine machine;

    @Shadow
    protected BlockArray pattern;

    @Shadow
    protected boolean resetZoom;

    @Shadow
    protected abstract void addControllerToPattern(DynamicMachine machine);

    @Shadow
    protected abstract void addUpgradeIngredientToPattern(DynamicMachine machine);

    @Shadow
    protected abstract void addDynamicPatternToPattern(DynamicMachine machine);

    @Shadow
    protected abstract void refreshPattern();

    @Unique
    private String mmceComplement$previewModule = AttachmentResolver.MAIN;

    @Unique
    private boolean mmceComplement$mergePreviewParents;

    @Override
    public String mmceComplement$getPreviewModule() {
        return mmceComplement$previewModule;
    }

    @Override
    public void mmceComplement$showNextPreviewModule() {
        Map<String, AttachmentModule> modules = mmceComplement$getModules();
        List<String> pages = new ArrayList<>(modules.size() + 1);
        pages.add(AttachmentResolver.MAIN);
        pages.addAll(modules.keySet());
        int current = pages.indexOf(mmceComplement$previewModule);
        mmceComplement$previewModule = pages.get((current + 1) % pages.size());
        mmceComplement$mergePreviewParents = false;
        mmceComplement$refreshAndRecenter();
    }

    @Override
    public boolean mmceComplement$previewModuleHasParents() {
        if (AttachmentResolver.MAIN.equals(mmceComplement$previewModule)) {
            return false;
        }
        return !AttachmentPatternResolver.getAncestors(
            mmceComplement$previewModule, mmceComplement$getModules()).isEmpty();
    }

    @Override
    public boolean mmceComplement$isMergingPreviewParents() {
        return mmceComplement$mergePreviewParents;
    }

    @Override
    public void mmceComplement$setMergePreviewParents(boolean mergeParents) {
        boolean accepted = mergeParents && mmceComplement$previewModuleHasParents();
        if (mmceComplement$mergePreviewParents == accepted) {
            return;
        }
        mmceComplement$mergePreviewParents = accepted;
        mmceComplement$refreshAndRecenter();
    }

    @Inject(method = "initializePattern", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$initializeAttachmentPattern(DynamicMachine machine,
                                                             CallbackInfo ci) {
        Map<String, AttachmentModule> modules = mmceComplement$getModules();
        if (modules.isEmpty() || AttachmentResolver.MAIN.equals(mmceComplement$previewModule)) {
            return;
        }

        TaggedPositionBlockArray selected;
        if (mmceComplement$mergePreviewParents) {
            selected = AttachmentPatternResolver.getMergedPreviewPattern(
                machine.getPattern(), modules, mmceComplement$previewModule);
        } else {
            selected = modules.get(mmceComplement$previewModule).getEffectivePattern(
                machine.getPattern(), modules);
        }

        boolean includesMain = mmceComplement$mergePreviewParents
            && AttachmentPatternResolver.getAncestors(
                mmceComplement$previewModule, modules).contains(AttachmentResolver.MAIN);
        if (includesMain) {
            pattern = new BlockArray(machine.getPattern());
            addControllerToPattern(machine);
            if (!(machine.isHideComponentsWhenFormed()
                && ((WorldSceneRendererWidget) (Object) this).isStructureFormed())) {
                addUpgradeIngredientToPattern(machine);
                addDynamicPatternToPattern(machine);
            }
            for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry
                : selected.getPattern().entrySet()) {
                if (!pattern.hasBlockAt(entry.getKey())) {
                    pattern.addBlock(entry.getKey(), entry.getValue());
                }
            }
        } else {
            pattern = new BlockArray(selected);
            addControllerToPattern(machine);
        }
        ci.cancel();
    }

    @Unique
    private Map<String, AttachmentModule> mmceComplement$getModules() {
        return ((AttachmentMachine) (Object) machine).mmceComplement$getAttachmentModules();
    }

    @Unique
    private void mmceComplement$refreshAndRecenter() {
        resetZoom = true;
        refreshPattern();
    }
}
