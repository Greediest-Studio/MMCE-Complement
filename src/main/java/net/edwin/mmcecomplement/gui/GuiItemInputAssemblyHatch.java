package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Two-column item/fluid GUI for the input assembly tiers. */
public class GuiItemInputAssemblyHatch extends GuiContainer {
    private final TileItemInputAssemblyHatch tile;
    private final ResourceLocation background;

    public GuiItemInputAssemblyHatch(EntityPlayer player, TileItemInputAssemblyHatch tile) {
        super(new ContainerDataItemInputHatch(player, tile, 71));
        this.tile = tile;
        this.background = new ResourceLocation("mmce_complement",
            "textures/gui/container/input_assembly_" + tile.getTier().getName() + ".png");
        this.xSize = 256;
        this.ySize = tile.getTier().getGuiHeight();
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(new NetworkHandlerMMCE.InteractQuadFluidTankMessage(
                tile.getPos(), tile.getWorld().provider.getDimension(), tank));
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0) {
            List<String> tooltip = new ArrayList<>();
            String name = tile.getStoredDisplayName(tank);
            tooltip.add(name == null ? I18n.format("gui.mmce_complement.data_input_assembly_hatch.empty") : name);
            tooltip.add(I18n.format(tile.isGas(tank)
                ? "gui.mmce_complement.data_input_assembly_hatch.amount.gas"
                : "gui.mmce_complement.data_input_assembly_hatch.amount.fluid",
                tile.getStoredAmount(tank), tile.getPerTankCapacity()));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else renderHoveredToolTip(mouseX, mouseY);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String titleKey = tile instanceof TileSelfCycleAssemblyHatch
            ? "gui.mmce_complement.self_cycle_assembly_hatch.title"
            : "gui.mmce_complement.input_assembly_hatch.title."
                + tile.getTier().getName();
        String tierName = I18n.format(titleKey);
        fontRenderer.drawString(tierName, 8, 6, 0x404040);
        drawCentered(I18n.format("gui.mmce_complement.input_assembly_hatch.items"), 71, 27);
        drawCentered(I18n.format("gui.mmce_complement.input_assembly_hatch.fluids"), 195, 27);
        fontRenderer.drawString(I18n.format("container.inventory"), 47, ySize - 94, 0x404040);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        for (int i = 0; i < tile.getTankCount(); i++) drawTankContents(i, guiLeft + tankX(i), guiTop + tankY(i));
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawCentered(String text, int x, int y) {
        fontRenderer.drawString(text, x - fontRenderer.getStringWidth(text) / 2, y, 0x404040);
    }
    private void drawTankContents(int index, int x, int y) {
        int amount = tile.getStoredAmount(index); ResourceLocation texture = tile.getStoredTexture(index);
        if (amount <= 0 || texture == null) return;
        int width = MathHelper.clamp(MathHelper.ceil((float) amount / tile.getPerTankCapacity() * getTankWidth()), 1, getTankWidth());
        int tint = tile.getStoredTint(index);
        GlStateManager.color(((tint >> 16) & 0xFF) / 255F, ((tint >> 8) & 0xFF) / 255F, (tint & 0xFF) / 255F, 1F);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getTextureExtry(texture.toString());
        if (sprite == null) sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTexturedModalRect(x, y, sprite, width, getTankHeight());
        GlStateManager.color(1F, 1F, 1F, 1F);
    }
    private int tankAt(int mouseX, int mouseY) {
        int rx = mouseX - guiLeft, ry = mouseY - guiTop;
        for (int i = 0; i < tile.getTankCount(); i++) if (rx >= tankX(i) && rx < tankX(i) + getTankWidth() && ry >= tankY(i) && ry < tankY(i) + getTankHeight()) return i;
        return -1;
    }
    private int tankX(int index) { return tile.getTankCount() <= 2 ? 169 : 169 + index % 2 * 29; }
    private int tankY(int index) { return tile.getTankCount() <= 2 ? 45 + index * 30 : 45 + index / 2 * 20; }
    private int getTankWidth() { return tile.getTankCount() <= 2 ? 52 : 25; }
    private int getTankHeight() { return tile.getTankCount() <= 2 ? 20 : 16; }
}
