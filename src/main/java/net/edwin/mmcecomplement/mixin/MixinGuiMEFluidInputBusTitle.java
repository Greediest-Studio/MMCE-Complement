package net.edwin.mmcecomplement.mixin;

import appeng.core.localization.GuiText;
import github.kasuminova.mmce.common.container.ContainerMEFluidInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFluidInventoryInputBus;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the title of the full-inventory ME fluid input bus. */
@Mixin(targets = "github.kasuminova.mmce.client.gui.GuiMEFluidInputBus",
    remap = false)
public abstract class MixinGuiMEFluidInputBusTitle extends GuiContainer {
    protected MixinGuiMEFluidInputBusTitle(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "drawFG", at = @At("HEAD"), cancellable = true,
        remap = false)
    private void mmceComplement$inventoryTitle(int offsetX, int offsetY,
                                                int mouseX, int mouseY,
                                                CallbackInfo ci) {
        if (!(inventorySlots instanceof ContainerMEFluidInputBus)
            || !(((ContainerMEFluidInputBus) inventorySlots).getOwner()
            instanceof TileMEFluidInventoryInputBus)) return;
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_inventory.fluid_title"),
            8, 6, 0x404040);
        fontRenderer.drawString(GuiText.Config.getLocal(), 8, 24, 0x404040);
        fontRenderer.drawString(GuiText.StoredFluids.getLocal(),
            8, 125, 0x404040);
        fontRenderer.drawString(GuiText.inventory.getLocal(),
            8, ySize - 93, 0x404040);
        ci.cancel();
    }
}
