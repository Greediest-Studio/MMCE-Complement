package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileLiquidEnergizerHatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** MMCE-style single-fluid-slot display with a long-backed energy meter. */
public class GuiLiquidEnergizerHatch extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        "modularmachinery", "textures/gui/guibar.png");
    private static final int TANK_X = 15;
    private static final int TANK_Y = 10;
    private static final int TANK_WIDTH = 20;
    private static final int TANK_HEIGHT = 61;

    private final TileLiquidEnergizerHatch tile;

    public GuiLiquidEnergizerHatch(EntityPlayer player,
                                   TileLiquidEnergizerHatch tile) {
        super(new ContainerLiquidEnergizerHatch(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        if (mouseButton == 0 && isOverTank(mouseX, mouseY)) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.InteractQuadFluidTankMessage(
                    tile.getPos(), tile.getWorld().provider.getDimension(), 0));
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (isOverTank(mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            String name = tile.getStoredDisplayName();
            tooltip.add(name == null ? I18n.format(
                "gui.mmce_complement.liquid_energizer_hatch.empty") : name);
            tooltip.add(I18n.format(
                "gui.mmce_complement.liquid_energizer_hatch.amount.fluid",
                tile.getFluidAmountLong(), tile.getFluidCapacityLong()));
            long ratio = tile.getConversionRatio();
            if (ratio > 0L) {
                tooltip.add(I18n.format(
                    "gui.mmce_complement.liquid_energizer_hatch.conversion",
                    ratio));
            }
            drawHoveringText(tooltip, mouseX, mouseY);
        } else if (isOverEnergy(mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add(I18n.format(
                "gui.mmce_complement.liquid_energizer_hatch.energy"));
            tooltip.add(I18n.format(
                "gui.mmce_complement.liquid_energizer_hatch.amount.energy",
                tile.getCurrentEnergy(), tile.getMaxEnergy()));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.liquid_energizer_hatch.title"),
            44, 8, 0x404040);
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.liquid_energizer_hatch.fluid"),
            44, 27, 0x404040);
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.liquid_energizer_hatch.energy"),
            44, 45, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"),
            8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                    int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawFluid();

        int left = guiLeft + 44;
        int top = guiTop + 59;
        drawRect(left, top, left + 121, top + 10, 0xFF3A3A3A);
        drawRect(left + 1, top + 1, left + 120, top + 9, 0xFF171717);
        long capacity = tile.getMaxEnergy();
        long stored = tile.getCurrentEnergy();
        int width = capacity <= 0L || stored <= 0L ? 0
            : MathHelper.clamp((int) Math.ceil(
                Math.min(1.0D, (double) stored / (double) capacity) * 119.0D),
                1, 119);
        if (width > 0) {
            drawRect(left + 1, top + 1, left + 1 + width, top + 9,
                0xFFE04B37);
        }
    }

    private void drawFluid() {
        long amount = tile.getFluidAmountLong();
        ResourceLocation texture = tile.getStoredTexture();
        if (amount <= 0L || texture == null) return;
        long capacity = tile.getFluidCapacityLong();
        int height = capacity <= 0L ? 0 : MathHelper.clamp((int) Math.ceil(
            Math.min(1.0D, (double) amount / (double) capacity) * TANK_HEIGHT),
            1, TANK_HEIGHT);
        int tint = tile.getStoredTint();
        GlStateManager.color(((tint >> 16) & 0xFF) / 255F,
            ((tint >> 8) & 0xFF) / 255F, (tint & 0xFF) / 255F, 1F);
        TextureAtlasSprite sprite = Minecraft.getMinecraft()
            .getTextureMapBlocks().getTextureExtry(texture.toString());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getMissingSprite();
        }
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTexturedModalRect(guiLeft + TANK_X,
            guiTop + TANK_Y + TANK_HEIGHT - height,
            sprite, TANK_WIDTH, height);
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft + TANK_X, guiTop + TANK_Y,
            176, 0, TANK_WIDTH, TANK_HEIGHT);
    }

    private boolean isOverTank(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= TANK_X && x < TANK_X + TANK_WIDTH
            && y >= TANK_Y && y < TANK_Y + TANK_HEIGHT;
    }

    private boolean isOverEnergy(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= 44 && x < 165 && y >= 59 && y < 69;
    }
}
