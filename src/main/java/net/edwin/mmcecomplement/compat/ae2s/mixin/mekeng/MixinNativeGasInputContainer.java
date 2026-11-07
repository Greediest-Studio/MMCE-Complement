package net.edwin.mmcecomplement.compat.ae2s.mixin.mekeng;

import github.kasuminova.mmce.common.tile.MEGasInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEGasInventoryInputBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the inventory variant's buffer intact while its native GUI is open. */
@Mixin(targets = "github.formlessdragon.appcompat.common.container.mmce.ContainerMEGasInputBus", remap = false)
public abstract class MixinNativeGasInputContainer {
    @Shadow @Final private MEGasInputBus owner;
    @Shadow public int capacity;

    @Inject(method = "refreshCapacity", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$inventoryCapacity(CallbackInfo ci) {
        if (owner instanceof TileMEGasInventoryInputBus) {
            capacity = Integer.MAX_VALUE;
            owner.getTanks().setCap(capacity);
            ci.cancel();
        }
    }
}
