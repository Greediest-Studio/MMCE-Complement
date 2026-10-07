package net.edwin.mmcecomplement.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.attachment.AttachmentJsonParser;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModelMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(value = DynamicMachine.MachineDeserializer.class, remap = false)
public abstract class MixinDynamicMachineDeserializer {

    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;"
        + "Lcom/google/gson/JsonDeserializationContext;)Lhellfirepvp/modularmachinery/common/machine/DynamicMachine;",
        at = @At("RETURN"))
    private void mmceComplement$readAttachmentModules(JsonElement json,
                                                       Type type,
                                                       JsonDeserializationContext context,
                                                       CallbackInfoReturnable<DynamicMachine> cir) {
        DynamicMachine machine = cir.getReturnValue();
        AttachmentMachine attachmentMachine = (AttachmentMachine) (Object) machine;
        attachmentMachine.mmceComplement$getAttachmentModules().putAll(
            AttachmentJsonParser.parse(json.getAsJsonObject(), machine, context));
        JsonElement modeElement = json.getAsJsonObject().get("attachment-module-model");
        if (modeElement != null) {
            if (!modeElement.isJsonPrimitive()
                || !modeElement.getAsJsonPrimitive().isString()) {
                throw new com.google.gson.JsonParseException(
                    "'attachment-module-model' has to be a string!");
            }
            attachmentMachine.mmceComplement$setAttachmentModelMode(
                AttachmentModelMode.parse(modeElement.getAsString()));
        }
    }
}
