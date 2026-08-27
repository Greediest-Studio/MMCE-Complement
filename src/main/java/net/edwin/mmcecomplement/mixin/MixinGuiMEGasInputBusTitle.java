package net.edwin.mmcecomplement.mixin;

import appeng.core.localization.GuiText;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional title mixin for MekEng's MMCE gas input GUI. */
@Pseudo
@Mixin(targets = "github.kasuminova.mmce.client.gui.GuiMEGasInputBus",
    remap = false)
public abstract class MixinGuiMEGasInputBusTitle extends GuiContainer {
    protected MixinGuiMEGasInputBusTitle(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "drawFG", at = @At("HEAD"), cancellable = true,
        remap = false)
    private void mmceComplement$inventoryTitle(int offsetX, int offsetY,
                                                int mouseX, int mouseY,
                                                CallbackInfo ci) {
        Object owner = getOwner(inventorySlots);
        if (!(owner instanceof MEInventoryInputBus)) return;
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_inventory.gas_title"),
            8, 6, 0x404040);
        fontRenderer.drawString(GuiText.Config.getLocal(), 8, 24, 0x404040);
        fontRenderer.drawString(I18n.format("tooltip.mekeng.stored_gas"),
            8, 125, 0x404040);
        fontRenderer.drawString(GuiText.inventory.getLocal(),
            8, ySize - 93, 0x404040);
        ci.cancel();
    }

    private static Object getOwner(Container container) {
        try {
            return container.getClass().getMethod("getOwner")
                .invoke(container);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
