package net.edwin.mmcecomplement.compat.ae.gui;

import github.kasuminova.mmce.client.gui.GuiMEItemBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssemblyFallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Mekanism-free GUI for the registered full-exposure block. */
public class GuiMEFullExposureFallback extends GuiMEItemBus {

    private final TileMEFullExposureAssemblyFallback tile;

    public GuiMEFullExposureFallback(TileMEFullExposureAssemblyFallback tile,
                                     EntityPlayer player) {
        super(new ContainerMEFullExposureFallback(tile, player));
        this.tile = tile;
        this.ySize = 204;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_full_exposure_assembly.title"),
            8, 8, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.me_inventory.active"),
            8, 24, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.me_inventory.on"),
            8, 42, 0x404040);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(new ResourceLocation(
            "modularmachinery", "textures/gui/meiteminputbus.png"));
        drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
    }
}
