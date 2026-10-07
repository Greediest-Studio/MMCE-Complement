package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import net.edwin.mmcecomplement.attachment.AttachmentModelMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(value = DynamicMachine.class, remap = false)
public abstract class MixinDynamicMachine implements AttachmentMachine {

    @Unique
    private final Map<String, AttachmentModule> mmceComplement$attachmentModules = new LinkedHashMap<>();

    @Unique
    private AttachmentModelMode mmceComplement$attachmentModelMode =
        AttachmentModelMode.DEFAULT;

    @Override
    public Map<String, AttachmentModule> mmceComplement$getAttachmentModules() {
        return mmceComplement$attachmentModules;
    }

    @Override
    public AttachmentModelMode mmceComplement$getAttachmentModelMode() {
        return mmceComplement$attachmentModelMode;
    }

    @Unique
    public void mmceComplement$setAttachmentModelMode(AttachmentModelMode mode) {
        mmceComplement$attachmentModelMode = mode == null
            ? AttachmentModelMode.DEFAULT : mode;
    }

    @Inject(method = "isHideComponentsWhenFormed", at = @At("RETURN"),
        cancellable = true)
    private void mmceComplement$hideComponentsForCustomAttachmentModel(
        CallbackInfoReturnable<Boolean> cir) {
        if (mmceComplement$attachmentModelMode == AttachmentModelMode.HIDE
            || mmceComplement$attachmentModelMode == AttachmentModelMode.SEPARATE) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mergeFrom", at = @At("RETURN"))
    private void mmceComplement$copyAttachmentModules(DynamicMachine another, CallbackInfo ci) {
        mmceComplement$attachmentModules.clear();
        mmceComplement$attachmentModules.putAll(
            ((AttachmentMachine) (Object) another).mmceComplement$getAttachmentModules());
        mmceComplement$attachmentModelMode = ((AttachmentMachine) (Object) another)
            .mmceComplement$getAttachmentModelMode();
    }
}
