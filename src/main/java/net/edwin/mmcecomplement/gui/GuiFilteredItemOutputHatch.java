package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.filter.CompactQuantityFormatter;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileFilteredItemOutputHatch;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** MMCE-style output slot plus a JEI-capable ghost item filter slot. */
public class GuiFilteredItemOutputHatch extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        "mmce_complement",
        "textures/gui/container/filtered_output_hatch.png");
    public static final int FILTER_X = 108;
    public static final int FILTER_Y = 35;

    private final TileFilteredItemOutputHatch tile;

    public GuiFilteredItemOutputHatch(EntityPlayer player,
                                      TileFilteredItemOutputHatch tile) {
        super(new ContainerFilteredItemOutputHatch(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    public int getFilterScreenX() { return guiLeft + FILTER_X; }
    public int getFilterScreenY() { return guiTop + FILTER_Y; }

    public void setFilterFromJei(ItemStack stack) {
        ItemStack normalized = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!normalized.isEmpty()) normalized.setCount(1);
        tile.setFilter(normalized);
        NBTTagCompound payload = new NBTTagCompound();
        if (!normalized.isEmpty()) normalized.writeToNBT(payload);
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.SetHatchFieldMessage(tile.getPos(),
                tile.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_FILTERED_ITEM_OUTPUT,
                payload));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        if (overFilter(mouseX, mouseY)) {
            if (mouseButton == 1) {
                setFilterFromJei(ItemStack.EMPTY);
            } else if (mouseButton == 0) {
                ItemStack cursor = mc.player.inventory.getItemStack();
                if (!cursor.isEmpty()) setFilterFromJei(cursor);
            }
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (overOutput(mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            ItemStack stored = tile.getInventory().getStackInSlot(0);
            tooltip.add(stored.isEmpty() ? I18n.format(
                "gui.mmce_complement.filtered_output_hatch.empty")
                : stored.getDisplayName());
            tooltip.add(I18n.format(
                "gui.mmce_complement.filtered_item_output_hatch.amount",
                tile.getStoredCount(), Integer.MAX_VALUE));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else if (overFilter(mouseX, mouseY)) {
            List<String> tooltip = new ArrayList<>();
            ItemStack filter = tile.getFilter();
            tooltip.add(I18n.format(
                "gui.mmce_complement.filtered_output_hatch.filter"));
            tooltip.add(filter.isEmpty() ? I18n.format(
                "gui.mmce_complement.filtered_output_hatch.unset")
                : filter.getDisplayName());
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
            "gui.mmce_complement.filtered_item_output_hatch.title"),
            8, 6, 0x404040);
        drawCentered(I18n.format(
            "gui.mmce_complement.filtered_output_hatch.output"), 58, 21);
        drawCentered(I18n.format(
            "gui.mmce_complement.filtered_output_hatch.filter"), 116, 21);
        fontRenderer.drawString(I18n.format("container.inventory"),
            8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                    int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawSlotFrame(guiLeft + 49, guiTop + 34);
        drawSlotFrame(guiLeft + FILTER_X - 1, guiTop + FILTER_Y - 1);
        ItemStack filter = tile.getFilter();
        if (!filter.isEmpty()) {
            ItemStack display = filter.copy();
            display.setCount(1);
            itemRender.renderItemAndEffectIntoGUI(display,
                guiLeft + FILTER_X, guiTop + FILTER_Y);
        }
    }

    @Override
    public void drawSlot(Slot slot) {
        if (slot.slotNumber != 0) {
            super.drawSlot(slot);
            return;
        }
        ItemStack stored = tile.getInventory().getStackInSlot(0);
        if (stored.isEmpty()) return;
        ItemStack display = stored.copy();
        display.setCount(1);
        itemRender.renderItemAndEffectIntoGUI(display, slot.xPos, slot.yPos);
        itemRender.renderItemOverlayIntoGUI(fontRenderer, display,
            slot.xPos, slot.yPos,
            CompactQuantityFormatter.format(tile.getStoredCount()));
    }

    private void drawCentered(String text, int x, int y) {
        fontRenderer.drawString(text,
            x - fontRenderer.getStringWidth(text) / 2, y, 0x404040);
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFFFFFFFF);
        drawRect(x + 2, y + 2, x + 17, y + 17, 0xFF8B8B8B);
        drawRect(x + 2, y + 2, x + 16, y + 16, 0xFF373737);
    }

    private boolean overOutput(int mouseX, int mouseY) {
        return isPointInRegion(50, 35, 16, 16, mouseX, mouseY);
    }

    private boolean overFilter(int mouseX, int mouseY) {
        return isPointInRegion(FILTER_X, FILTER_Y, 16, 16,
            mouseX, mouseY);
    }
}
