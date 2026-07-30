package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.filter.CompactQuantityFormatter;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileFilteredFluidOutputHatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** MMCE-style int-max fluid tank plus a JEI-capable ghost fluid filter. */
public class GuiFilteredFluidOutputHatch extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        "mmce_complement",
        "textures/gui/container/filtered_output_hatch.png");
    private static final int TANK_X = 49;
    private static final int TANK_Y = 25;
    private static final int TANK_W = 20;
    private static final int TANK_H = 46;
    public static final int FILTER_X = 108;
    public static final int FILTER_Y = 39;

    private final TileFilteredFluidOutputHatch tile;

    public GuiFilteredFluidOutputHatch(EntityPlayer player,
                                       TileFilteredFluidOutputHatch tile) {
        super(new ContainerFilteredFluidOutputHatch(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    public int getFilterScreenX() { return guiLeft + FILTER_X; }
    public int getFilterScreenY() { return guiTop + FILTER_Y; }

    public void setFilterFromJei(@Nullable FluidStack stack) {
        FluidStack normalized = stack == null ? null : stack.copy();
        if (normalized != null) normalized.amount = 1;
        tile.setFilter(normalized);
        NBTTagCompound payload = new NBTTagCompound();
        if (normalized != null) normalized.writeToNBT(payload);
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.SetHatchFieldMessage(tile.getPos(),
                tile.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_FILTERED_FLUID_OUTPUT,
                payload));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        if (overFilter(mouseX, mouseY)) {
            if (mouseButton == 1) {
                setFilterFromJei(null);
            } else if (mouseButton == 0) {
                ItemStack cursor = mc.player.inventory.getItemStack();
                FluidStack fluid = FluidUtil.getFluidContained(cursor);
                if (fluid != null) setFilterFromJei(fluid);
            }
            return;
        }
        if (mouseButton == 0 && overTank(mouseX, mouseY)) {
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
        if (overTank(mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            String name = tile.getStoredDisplayName();
            tooltip.add(name == null ? I18n.format(
                "gui.mmce_complement.filtered_output_hatch.empty") : name);
            tooltip.add(I18n.format(
                "gui.mmce_complement.filtered_fluid_output_hatch.amount",
                tile.getStoredAmount(), Integer.MAX_VALUE));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else if (overFilter(mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            FluidStack filter = tile.getFilter();
            tooltip.add(I18n.format(
                "gui.mmce_complement.filtered_output_hatch.filter"));
            tooltip.add(filter == null ? I18n.format(
                "gui.mmce_complement.filtered_output_hatch.unset")
                : filter.getLocalizedName());
            tooltip.add(I18n.format(
                "gui.mmce_complement.filtered_output_hatch.filter_help"));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.filtered_fluid_output_hatch.title"),
            8, 6, 0x404040);
        drawCentered(I18n.format(
            "gui.mmce_complement.filtered_output_hatch.output"), 59, 15);
        drawCentered(I18n.format(
            "gui.mmce_complement.filtered_output_hatch.filter"), 116, 25);
        fontRenderer.drawString(I18n.format("container.inventory"),
            8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                    int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawTankFrame();
        drawFluid(tile.getTank().getStored(), guiLeft + TANK_X,
            guiTop + TANK_Y, TANK_W, TANK_H, true);
        drawSlotFrame(guiLeft + FILTER_X - 1, guiTop + FILTER_Y - 1);
        drawFluid(tile.getFilter(), guiLeft + FILTER_X,
            guiTop + FILTER_Y, 16, 16, false);
        String amount = CompactQuantityFormatter.format(tile.getStoredAmount());
        fontRenderer.drawStringWithShadow(amount,
            guiLeft + TANK_X + TANK_W / 2F
                - fontRenderer.getStringWidth(amount) / 2F,
            guiTop + TANK_Y + TANK_H - 10, 0xFFFFFF);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawFluid(@Nullable FluidStack fluid, int x, int y,
                           int width, int height, boolean scaled) {
        if (fluid == null || fluid.amount <= 0) return;
        int drawnHeight = scaled ? MathHelper.clamp((int) Math.ceil(
            (double) tile.getStoredAmount() / Integer.MAX_VALUE * height),
            1, height) : height;
        int tint = fluid.getFluid().getColor(fluid);
        GlStateManager.color(((tint >> 16) & 0xFF) / 255F,
            ((tint >> 8) & 0xFF) / 255F, (tint & 0xFF) / 255F, 1F);
        TextureAtlasSprite sprite = Minecraft.getMinecraft()
            .getTextureMapBlocks().getTextureExtry(
                fluid.getFluid().getStill(fluid).toString());
        if (sprite == null) sprite = Minecraft.getMinecraft()
            .getTextureMapBlocks().getMissingSprite();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTexturedModalRect(x, y + height - drawnHeight,
            sprite, width, drawnHeight);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawTankFrame() {
        int x = guiLeft + TANK_X - 2;
        int y = guiTop + TANK_Y - 2;
        drawRect(x, y, x + TANK_W + 4, y + TANK_H + 4, 0xFF373737);
        drawRect(x + 1, y + 1, x + TANK_W + 3,
            y + TANK_H + 3, 0xFFFFFFFF);
        drawRect(x + 2, y + 2, x + TANK_W + 3,
            y + TANK_H + 3, 0xFF8B8B8B);
        drawRect(x + 2, y + 2, x + TANK_W + 2,
            y + TANK_H + 2, 0xFF242424);
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFFFFFFFF);
        drawRect(x + 2, y + 2, x + 17, y + 17, 0xFF8B8B8B);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF373737);
    }

    private void drawCentered(String text, int x, int y) {
        fontRenderer.drawString(text,
            x - fontRenderer.getStringWidth(text) / 2, y, 0x404040);
    }

    private boolean overTank(int mouseX, int mouseY) {
        return isPointInRegion(TANK_X, TANK_Y, TANK_W, TANK_H,
            mouseX, mouseY);
    }

    private boolean overFilter(int mouseX, int mouseY) {
        return isPointInRegion(FILTER_X, FILTER_Y, 16, 16,
            mouseX, mouseY);
    }
}
