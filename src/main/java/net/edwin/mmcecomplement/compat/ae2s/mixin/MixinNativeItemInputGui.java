package net.edwin.mmcecomplement.compat.ae2s.mixin;

import github.formlessdragon.appcompat.client.gui.mmce.GuiMEItemInputBus;
import github.formlessdragon.appcompat.common.container.mmce.ContainerMEItemInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These buses configure resource types; their marker counts stay fixed at one. */
@Mixin(value = GuiMEItemInputBus.class, remap = false)
public abstract class MixinNativeItemInputGui {
    @Shadow @Final private ContainerMEItemInputBus container;

    @Inject(method = "onMouseWheelEvent", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$typeOnly(int x, int y, int wheel, CallbackInfo ci) {
        Object owner = container.getOwner();
        if (owner instanceof MEInventoryInputBus || owner instanceof TileMEOreDictInputBus) ci.cancel();
    }
}
