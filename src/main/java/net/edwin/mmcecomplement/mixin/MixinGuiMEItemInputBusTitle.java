package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import appeng.core.localization.GuiText;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only the ore-dictionary bus title in MMCE's shared input GUI. */
@Mixin(targets = "github.kasuminova.mmce.client.gui.GuiMEItemInputBus", remap = false)
public abstract class MixinGuiMEItemInputBusTitle extends GuiContainer {
    protected MixinGuiMEItemInputBusTitle(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "drawFG", at = @At("HEAD"), cancellable = true,
            remap = false)
    private void mmceComplement$oreDictTitle(int offsetX, int offsetY,
                                             int mouseX, int mouseY,
                                             CallbackInfo ci) {
        if (inventorySlots instanceof ContainerMEItemInputBus) {
            Object owner = ((ContainerMEItemInputBus) inventorySlots)
                .getOwner();
            String title = owner instanceof TileMEOreDictInputBus
                ? "gui.mmce_complement.me_ore_dict.bus_title"
                : owner instanceof TileMEItemInventoryInputBus
                ? "gui.mmce_complement.me_inventory.item_title" : null;
            if (title == null) return;
            fontRenderer.drawString(I18n.format(title),
                    8, 8, 0x404040);
            fontRenderer.drawString(GuiText.Config.getLocal(), 8, 24, 0x404040);
            fontRenderer.drawString(GuiText.StoredItems.getLocal(), 97, 24,
                    0x404040);
            fontRenderer.drawString(GuiText.inventory.getLocal(), 8,
                    ySize - 93, 0x404040);
            ci.cancel();
        }
    }

    @Inject(method = "onMouseWheelEvent", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void mmceComplement$disableMarkerAmountScroll(int mouseX,
                                                          int mouseY,
                                                          int wheel,
                                                          CallbackInfo ci) {
        if (inventorySlots instanceof ContainerMEItemInputBus) {
            Object owner = ((ContainerMEItemInputBus) inventorySlots)
                .getOwner();
            if (owner instanceof TileMEOreDictInputBus
                    || owner instanceof MEInventoryInputBus) {
                ci.cancel();
            }
        }
    }
}
