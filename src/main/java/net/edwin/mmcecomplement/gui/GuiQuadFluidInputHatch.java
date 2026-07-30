package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidOutputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidOutputHatch;
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

/** Four-column tank display and per-tank container interaction GUI. */
public class GuiQuadFluidInputHatch extends GuiContainer {

    private static final ResourceLocation QUAD_BACKGROUND =
        new ResourceLocation("mmce_complement", "textures/gui/quad_fluid_hatch_gui.png");
    private static final ResourceLocation NINE_BACKGROUND =
        new ResourceLocation("mmce_complement", "textures/gui/nine_fluid_hatch_gui.png");
    private static final int TANK_Y = 10;
    private static final int TANK_WIDTH = 20;
    private static final int TANK_HEIGHT = 61;
    // Four 20px wells with the original 35px spacing occupy 125px, centered
    // in the 176px MMCE background with 26px left and 25px right margins.
    private static final int[] QUAD_TANK_X = {26, 61, 96, 131};
    // Nine 20px wells with a one-pixel overlap occupy 172px, so these are
    // centered in the 176px MMCE background (2px on either side).
    private static final int[] NINE_TANK_X = {2, 21, 40, 59, 78, 97, 116, 135, 154};
    private static final int TANK_BASE_U = 15;
    private static final int TANK_BASE_V = 10;
    private static final int TANK_OVERLAY_U = 176;
    private static final int TANK_OVERLAY_V = 0;

    private final TileQuadFluidInputHatch tile;

    public GuiQuadFluidInputHatch(EntityPlayer player, TileQuadFluidInputHatch tile) {
        super(new ContainerQuadFluidInputHatch(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.InteractQuadFluidTankMessage(
                    tile.getPos(), tile.getWorld().provider.getDimension(), tank));
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0) {
            List<String> tooltip = new ArrayList<>();
            boolean nine = tile.getTankCount() != TileQuadFluidInputHatch.TANK_COUNT;
            String keyPrefix;
            if (nine) {
                keyPrefix = tile instanceof TileQuadFluidOutputHatch
                    ? "gui.mmce_complement.nine_fluid_output_hatch."
                    : "gui.mmce_complement.nine_fluid_input_hatch.";
            } else {
                keyPrefix = tile instanceof TileQuadFluidOutputHatch
                    ? "gui.mmce_complement.quad_fluid_output_hatch."
                    : "gui.mmce_complement.quad_fluid_input_hatch.";
            }
            String name = tile.getStoredDisplayName(tank);
            if (name == null) {
                tooltip.add(I18n.format(keyPrefix + "empty"));
            } else {
                tooltip.add(name);
            }
            // Match MMCE's native fluid-hatch tooltip: always show the amount
            // line, including for an empty tank ("Empty" followed by
            // "0 mb / <capacity> mb").
            tooltip.add(I18n.format(
                tile.isGas(tank)
                    ? keyPrefix + "amount.gas"
                    : keyPrefix + "amount.fluid",
                tile.getStoredAmount(tank), tile.getPerTankCapacity()));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.inventory"),
            8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        ResourceLocation background = background();
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        for (int i = 0; i < tile.getTankCount(); i++) {
            int x = guiLeft + tankX()[i];
            int y = guiTop + TANK_Y;

            // MMCE's original tank well from guibar.png.
            mc.getTextureManager().bindTexture(background);
            drawTexturedModalRect(x, y, TANK_BASE_U, TANK_BASE_V,
                TANK_WIDTH, TANK_HEIGHT);
            drawTankContents(i, x, y);

            // MMCE draws this tick-mark overlay after the fluid/gas texture.
            mc.getTextureManager().bindTexture(background);
            drawTexturedModalRect(x, y, TANK_OVERLAY_U, TANK_OVERLAY_V,
                TANK_WIDTH, TANK_HEIGHT);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawTankContents(int index, int x, int y) {
        int amount = tile.getStoredAmount(index);
        ResourceLocation texture = tile.getStoredTexture(index);
        if (amount <= 0 || texture == null) {
            return;
        }
        int height = MathHelper.clamp(
            MathHelper.ceil((float) amount / tile.getPerTankCapacity() * TANK_HEIGHT),
            1, TANK_HEIGHT);
        int tint = tile.getStoredTint(index);
        float red = ((tint >> 16) & 0xFF) / 255F;
        float green = ((tint >> 8) & 0xFF) / 255F;
        float blue = (tint & 0xFF) / 255F;
        GlStateManager.color(red, green, blue, 1F);

        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
            .getTextureExtry(texture.toString());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTexturedModalRect(x, y + TANK_HEIGHT - height,
            sprite, TANK_WIDTH, height);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private int tankAt(int mouseX, int mouseY) {
        int relativeX = mouseX - guiLeft;
        int relativeY = mouseY - guiTop;
        if (relativeY < TANK_Y || relativeY >= TANK_Y + TANK_HEIGHT) {
            return -1;
        }
        int[] tankX = tankX();
        for (int i = 0; i < tile.getTankCount() && i < tankX.length; i++) {
            if (relativeX >= tankX[i] && relativeX < tankX[i] + TANK_WIDTH) {
                return i;
            }
        }
        return -1;
    }

    private int[] tankX() {
        return tile.getTankCount() == TileQuadFluidInputHatch.TANK_COUNT
            ? QUAD_TANK_X : NINE_TANK_X;
    }

    private ResourceLocation background() {
        return tile.getTankCount() == TileQuadFluidInputHatch.TANK_COUNT
            ? QUAD_BACKGROUND : NINE_BACKGROUND;
    }
}
