package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import net.edwin.mmcecomplement.attachment.AttachmentModelMode;
import net.edwin.mmcecomplement.variant.StorageMachine;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(value = DynamicMachine.class, remap = false)
public abstract class MixinDynamicMachine implements AttachmentMachine, StorageMachine {

    @Unique
    private final Map<String, AttachmentModule> mmceComplement$attachmentModules = new LinkedHashMap<>();

    @Unique
    private AttachmentModelMode mmceComplement$attachmentModelMode =
        AttachmentModelMode.DEFAULT;

    @Unique
    private boolean mmceComplement$storageVariant;

    @Unique
    private int mmceComplement$storageSlots = 54;

    @Unique private int mmceComplement$fluidSlots;
    @Unique private int mmceComplement$fluidCapacity;
    @Unique private int mmceComplement$gasSlots;
    @Unique private int mmceComplement$gasCapacity;

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

    @Override
    public boolean mmceComplement$isStorageVariant() {
        return mmceComplement$storageVariant;
    }

    @Override
    public int mmceComplement$getStorageSlots() {
        return mmceComplement$storageSlots;
    }

    @Override public boolean mmceComplement$hasFluidStorage() {
        return mmceComplement$fluidSlots > 0 && mmceComplement$fluidCapacity > 0;
    }
    @Override public int mmceComplement$getFluidStorageSlots() { return mmceComplement$fluidSlots; }
    @Override public int mmceComplement$getFluidStorageCapacity() { return mmceComplement$fluidCapacity; }
    @Override public boolean mmceComplement$hasGasStorage() {
        return mmceComplement$gasSlots > 0 && mmceComplement$gasCapacity > 0;
    }
    @Override public int mmceComplement$getGasStorageSlots() { return mmceComplement$gasSlots; }
    @Override public int mmceComplement$getGasStorageCapacity() { return mmceComplement$gasCapacity; }

    @Unique
    public void mmceComplement$configureStorageVariant(JsonObject root) {
        boolean variant = false;
        JsonElement variantElement = root.get("variant");
        if (variantElement != null && variantElement.isJsonPrimitive()) {
            variant = "storage".equalsIgnoreCase(variantElement.getAsString());
        }
        JsonElement storageElement = root.get("storage");
        if (storageElement != null) {
            if (storageElement.isJsonPrimitive() && storageElement.getAsJsonPrimitive().isBoolean()) {
                variant |= storageElement.getAsBoolean();
            } else if (storageElement.isJsonObject()) {
                variant = true;
                JsonElement slots = storageElement.getAsJsonObject().get("slots");
                if (slots != null && slots.isJsonPrimitive() && slots.getAsJsonPrimitive().isNumber()) {
                    mmceComplement$storageSlots = slots.getAsInt();
                }
                mmceComplement$readFluidStorage(storageElement.getAsJsonObject(), "fluid");
                mmceComplement$readFluidStorage(storageElement.getAsJsonObject(), "fluids");
                mmceComplement$readGasStorage(storageElement.getAsJsonObject(), "gas");
                mmceComplement$readGasStorage(storageElement.getAsJsonObject(), "gases");
                mmceComplement$readScalarStorage(storageElement.getAsJsonObject(), "fluid", true);
                mmceComplement$readScalarStorage(storageElement.getAsJsonObject(), "gas", false);
            }
        }
        mmceComplement$readFluidStorage(root, "fluid-storage");
        mmceComplement$readFluidStorage(root, "fluids");
        mmceComplement$readFluidStorage(root, "fluid");
        mmceComplement$readGasStorage(root, "gas-storage");
        mmceComplement$readGasStorage(root, "gases");
        mmceComplement$readGasStorage(root, "gas");
        mmceComplement$readScalarStorage(root, "fluid", true);
        mmceComplement$readScalarStorage(root, "gas", false);
        JsonElement legacySlots = root.get("storage-slots");
        if (legacySlots != null && legacySlots.isJsonPrimitive()
            && legacySlots.getAsJsonPrimitive().isNumber()) {
            variant = true;
            mmceComplement$storageSlots = legacySlots.getAsInt();
        }
        mmceComplement$storageVariant = variant;
        if (mmceComplement$storageSlots < 1) mmceComplement$storageSlots = 54;
        if (mmceComplement$storageSlots > 4096) mmceComplement$storageSlots = 4096;
    }

    @Unique
    private void mmceComplement$readFluidStorage(JsonObject root, String key) {
        JsonElement e = root.get(key);
        if (e == null) return;
        int slots = 0, capacity = 0;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            JsonElement s = o.get("slots");
            JsonElement c = o.get("capacity");
            if (c == null) c = o.get("amount");
            if (s != null && s.isJsonPrimitive() && s.getAsJsonPrimitive().isNumber()) slots = s.getAsInt();
            if (c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber()) capacity = c.getAsInt();
        } else if (e.isJsonArray() && e.getAsJsonArray().size() >= 2) {
            JsonElement c=e.getAsJsonArray().get(0), s=e.getAsJsonArray().get(1);
            if(c.isJsonPrimitive()&&s.isJsonPrimitive()){capacity=c.getAsInt();slots=s.getAsInt();}
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            capacity = e.getAsInt(); slots = 1;
        }
        if (slots > 0 && capacity > 0) { mmceComplement$fluidSlots = slots; mmceComplement$fluidCapacity = capacity; }
    }

    @Unique
    private void mmceComplement$readGasStorage(JsonObject root, String key) {
        JsonElement e = root.get(key);
        if (e == null) return;
        int slots = 0, capacity = 0;
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            JsonElement s = o.get("slots");
            JsonElement c = o.get("capacity");
            if (c == null) c = o.get("amount");
            if (s != null && s.isJsonPrimitive() && s.getAsJsonPrimitive().isNumber()) slots = s.getAsInt();
            if (c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber()) capacity = c.getAsInt();
        } else if (e.isJsonArray() && e.getAsJsonArray().size() >= 2) {
            JsonElement c=e.getAsJsonArray().get(0), s=e.getAsJsonArray().get(1);
            if(c.isJsonPrimitive()&&s.isJsonPrimitive()){capacity=c.getAsInt();slots=s.getAsInt();}
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            capacity = e.getAsInt(); slots = 1;
        }
        if (slots > 0 && capacity > 0) { mmceComplement$gasSlots = slots; mmceComplement$gasCapacity = capacity; }
    }

    @Unique
    private void mmceComplement$readScalarStorage(JsonObject root, String prefix, boolean fluid) {
        JsonElement c=root.get(prefix+"-capacity"), s=root.get(prefix+"-slots");
        if(c!=null&&s!=null&&c.isJsonPrimitive()&&s.isJsonPrimitive()&&c.getAsJsonPrimitive().isNumber()&&s.getAsJsonPrimitive().isNumber()) {
            if(fluid){mmceComplement$fluidCapacity=c.getAsInt();mmceComplement$fluidSlots=s.getAsInt();}
            else {mmceComplement$gasCapacity=c.getAsInt();mmceComplement$gasSlots=s.getAsInt();}
        }
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
        StorageMachine source = (StorageMachine) (Object) another;
        mmceComplement$storageVariant = source.mmceComplement$isStorageVariant();
        mmceComplement$storageSlots = source.mmceComplement$getStorageSlots();
        mmceComplement$fluidSlots = source.mmceComplement$getFluidStorageSlots();
        mmceComplement$fluidCapacity = source.mmceComplement$getFluidStorageCapacity();
        mmceComplement$gasSlots = source.mmceComplement$getGasStorageSlots();
        mmceComplement$gasCapacity = source.mmceComplement$getGasStorageCapacity();
    }
}
