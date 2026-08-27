package net.edwin.mmcecomplement.mixin;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.render.StackSizeRenderer;
import appeng.container.slot.SlotFake;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides only fake-slot quantities while preserving stored-item amounts. */
@Mixin(targets = "github.kasuminova.mmce.client.gui.GuiMEItemBus",
    remap = false)
public abstract class MixinGuiMEItemBusMarkerCount extends GuiContainer {
    @Unique
    private boolean mmceComplement$hideCurrentSlotCount;

    protected MixinGuiMEItemBusMarkerCount(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), remap = false)
    private void mmceComplement$selectMarkerSlot(Slot slot, CallbackInfo ci) {
        mmceComplement$hideCurrentSlotCount = slot instanceof SlotFake
            && isInventoryMarkerBus();
    }

    @Redirect(method = "drawSlot",
        at = @At(value = "INVOKE",
            target = "Lappeng/client/render/StackSizeRenderer;renderStackSize(Lnet/minecraft/client/gui/FontRenderer;Lappeng/api/storage/data/IAEItemStack;II)V"),
        remap = false)
    private void mmceComplement$hideMarkerCount(StackSizeRenderer renderer,
                                                 FontRenderer font,
                                                 IAEItemStack stack,
                                                 int x, int y) {
        if (mmceComplement$hideCurrentSlotCount) return;
        renderer.renderStackSize(font, stack, x, y);
    }

    private boolean isInventoryMarkerBus() {
        if (inventorySlots instanceof ContainerMEInputAssembly) {
            Object owner = ((ContainerMEInputAssembly) inventorySlots)
                .getOwner();
            return owner instanceof MEInventoryInputBus
                || owner instanceof TileMEOreDictInputBus;
        }
        if (!(inventorySlots instanceof ContainerMEItemInputBus)) return false;
        Object owner = ((ContainerMEItemInputBus) inventorySlots).getOwner();
        return owner instanceof TileMEOreDictInputBus
            || owner instanceof MEInventoryInputBus;
    }
}
