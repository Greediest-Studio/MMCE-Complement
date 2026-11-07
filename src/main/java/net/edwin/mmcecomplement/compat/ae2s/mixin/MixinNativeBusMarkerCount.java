package net.edwin.mmcecomplement.compat.ae2s.mixin;

import ae2.container.AEBaseContainer;
import ae2.container.slot.AppEngSlot;
import ae2.container.slot.FakeSlot;
import github.formlessdragon.appcompat.client.gui.mmce.GuiMEBusBase;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiMEBusBase.class, remap = false)
public abstract class MixinNativeBusMarkerCount extends GuiContainer {
    protected MixinNativeBusMarkerCount(Container container) { super(container); }

    @Inject(method = "getSlotAmountText", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$hideMarkerCount(Slot slot, AppEngSlot appEngSlot, ItemStack raw,
                                                ItemStack display, CallbackInfoReturnable<String> cir) {
        if (!(slot instanceof FakeSlot) || !(inventorySlots instanceof AEBaseContainer)) return;
        Object owner = ((AEBaseContainer) inventorySlots).getTarget();
        if (owner instanceof MEInventoryInputBus || owner instanceof TileMEOreDictInputBus) cir.setReturnValue(null);
    }
}
