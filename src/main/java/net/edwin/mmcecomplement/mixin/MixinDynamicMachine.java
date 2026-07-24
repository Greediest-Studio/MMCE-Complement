package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(value = DynamicMachine.class, remap = false)
public abstract class MixinDynamicMachine implements AttachmentMachine {

    @Unique
    private final Map<String, AttachmentModule> mmceComplement$attachmentModules = new LinkedHashMap<>();

    @Override
    public Map<String, AttachmentModule> mmceComplement$getAttachmentModules() {
        return mmceComplement$attachmentModules;
    }

    @Inject(method = "mergeFrom", at = @At("RETURN"))
    private void mmceComplement$copyAttachmentModules(DynamicMachine another, CallbackInfo ci) {
        mmceComplement$attachmentModules.clear();
        mmceComplement$attachmentModules.putAll(
            ((AttachmentMachine) (Object) another).mmceComplement$getAttachmentModules());
    }
}
