package net.edwin.mmcecomplement.gui;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Large three-column GUI for the combined data, item and fluid assembly. */
public class GuiDataItemInputHatch extends GuiContainer {

    private final TileDataItemInputHatch tile;
    private final ResourceLocation background;
    private GuiTextField valueField;
    private GuiButton previousButton;
    private GuiButton nextButton;
    private int showing;
    private SmartInterfaceData displayedData;

    public GuiDataItemInputHatch(EntityPlayer player,
                                 TileDataItemInputHatch tile) {
        super(new ContainerDataItemInputHatch(player, tile));
        this.tile = tile;
        this.background = new ResourceLocation("mmce_complement",
            "textures/gui/container/data_input_assembly_"
                + tile.getTier().getName() + ".png");
        this.xSize = 256;
        this.ySize = tile.getTier().getGuiHeight();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        valueField = new GuiTextField(0, fontRenderer,
            guiLeft + 12, guiTop + 69, 58, 14);
        valueField.setMaxStringLength(16);
        previousButton = new GuiButton(1, guiLeft + 12, guiTop + 86,
            27, 18, "<");
        nextButton = new GuiButton(2, guiLeft + 43, guiTop + 86,
            27, 18, ">");
        buttonList.add(previousButton);
        buttonList.add(nextButton);
        syncDisplayedData(true);
    }

    @Override
    public void onGuiClosed() {
        submitValue();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        valueField.updateCursorCounter();
        syncDisplayedData(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int size = tile.getDataProvider().getBoundSize();
        if (size <= 0) {
            return;
        }
        submitValue();
        if (button.id == 1) {
            showing = (showing + size - 1) % size;
        } else if (button.id == 2) {
            showing = (showing + 1) % size;
        }
        syncDisplayedData(true);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.InteractQuadFluidTankMessage(
                    tile.getPos(), tile.getWorld().provider.getDimension(), tank));
            return;
        }
        boolean wasFocused = valueField.isFocused();
        super.mouseClicked(mouseX, mouseY, mouseButton);
        valueField.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasFocused && !valueField.isFocused()) {
            submitValue();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (valueField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN
                || keyCode == Keyboard.KEY_NUMPADENTER) {
                submitValue();
                valueField.setFocused(false);
                return;
            }
            if (Character.isDigit(typedChar) || typedChar == '-'
                || typedChar == '+' || typedChar == '.'
                || typedChar == 'e' || typedChar == 'E'
                || hellfirepvp.modularmachinery.common.util.MiscUtils
                    .isTextBoxKey(keyCode)) {
                valueField.textboxKeyTyped(typedChar, keyCode);
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int tank = tankAt(mouseX, mouseY);
        if (tank >= 0) {
            List<String> tooltip = new ArrayList<>();
            String name = tile.getStoredDisplayName(tank);
            tooltip.add(name == null
                ? I18n.format("gui.mmce_complement.data_input_assembly_hatch.empty")
                : name);
            tooltip.add(I18n.format(
                tile.isGas(tank)
                    ? "gui.mmce_complement.data_input_assembly_hatch.amount.gas"
                    : "gui.mmce_complement.data_input_assembly_hatch.amount.fluid",
                tile.getStoredAmount(tank), tile.getPerTankCapacity()));
            drawHoveringText(tooltip, mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int size = tile.getDataProvider().getBoundSize();
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.data_input_assembly_hatch.title."
                + tile.getTier().getName()),
            8, 6, 0x404040);
        drawCentered(I18n.format(
            "gui.mmce_complement.data_input_assembly_hatch.data"),
            42, 27);
        drawCentered(I18n.format(
            "gui.mmce_complement.data_input_assembly_hatch.items"),
            132, 27);
        drawCentered(I18n.format(
            "gui.mmce_complement.data_input_assembly_hatch.fluids"),
            217, 27);

        SmartInterfaceData data = displayedData;
        if (size <= 0 || data == null) {
            fontRenderer.drawString(I18n.format("gui.smartinterface.notfound"),
                12, 45, 0x404040);
        } else {
            DynamicMachine machine = MachineRegistry.getRegistry()
                .getMachine(data.getParent());
            SmartInterfaceType type = machine == null
                ? null : machine.getSmartInterfaceType(data.getType());
            String machineName = machine == null
                ? data.getParent().toString() : machine.getLocalizedName();
            fontRenderer.drawString(trim(machineName, 58),
                12, 45, 0x404040);
            String typeName = type == null
                ? data.getType() : I18n.format(type.getHeaderInfo());
            fontRenderer.drawString(trim(typeName, 58),
                12, 57, 0x404040);
        }
        fontRenderer.drawString(I18n.format("container.inventory"),
            47, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                    int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        for (int i = 0; i < tile.getTankCount(); i++) {
            int x = guiLeft + tankX(i);
            int y = guiTop + tankY(i);
            drawTankContents(i, x, y);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);

        valueField.setVisible(displayedData != null);
        previousButton.visible = displayedData != null;
        nextButton.visible = displayedData != null;
        if (displayedData != null) {
            valueField.drawTextBox();
        }
    }

    private void syncDisplayedData(boolean forceText) {
        int size = tile.getDataProvider().getBoundSize();
        if (size <= 0) {
            showing = 0;
            displayedData = null;
            return;
        }
        if (showing >= size) {
            showing = size - 1;
        }
        SmartInterfaceData current = tile.getDataProvider()
            .getMachineData(showing);
        if (forceText || current != displayedData && !valueField.isFocused()) {
            valueField.setText(current == null
                ? "" : Float.toString(current.getValue()));
        }
        displayedData = current;
    }

    private void submitValue() {
        if (displayedData == null || valueField == null) {
            return;
        }
        final float value;
        try {
            value = Float.parseFloat(valueField.getText());
        } catch (NumberFormatException ignored) {
            valueField.setText(Float.toString(displayedData.getValue()));
            return;
        }
        if (!Float.isFinite(value)) {
            valueField.setText(Float.toString(displayedData.getValue()));
            return;
        }

        NBTTagCompound payload = new NBTTagCompound();
        payload.setLong("controllerPos", displayedData.getPos().toLong());
        payload.setFloat("value", value);
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.SetHatchFieldMessage(
                tile.getPos(), tile.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_DATA_INPUT_ASSEMBLY_VALUE, payload));
        displayedData.setValue(value);
    }

    private String trim(String text, int maxWidth) {
        return fontRenderer.trimStringToWidth(text == null ? "" : text,
            maxWidth);
    }

    private void drawCentered(String text, int centerX, int y) {
        fontRenderer.drawString(text,
            centerX - fontRenderer.getStringWidth(text) / 2,
            y, 0x404040);
    }

    private void drawTankContents(int index, int x, int y) {
        int amount = tile.getStoredAmount(index);
        ResourceLocation texture = tile.getStoredTexture(index);
        if (amount <= 0 || texture == null) {
            return;
        }
        int tankWidth = getTankWidth();
        int tankHeight = getTankHeight();
        int width = MathHelper.clamp(MathHelper.ceil(
            (float) amount / tile.getPerTankCapacity() * tankWidth),
            1, tankWidth);
        int tint = tile.getStoredTint(index);
        GlStateManager.color(((tint >> 16) & 0xFF) / 255F,
            ((tint >> 8) & 0xFF) / 255F,
            (tint & 0xFF) / 255F, 1F);
        TextureAtlasSprite sprite = Minecraft.getMinecraft()
            .getTextureMapBlocks().getTextureExtry(texture.toString());
        if (sprite == null) {
            sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                .getMissingSprite();
        }
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTexturedModalRect(x, y, sprite, width, tankHeight);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private int tankAt(int mouseX, int mouseY) {
        int relativeX = mouseX - guiLeft;
        int relativeY = mouseY - guiTop;
        for (int i = 0; i < tile.getTankCount(); i++) {
            int x = tankX(i);
            int y = tankY(i);
            if (relativeX >= x && relativeX < x + getTankWidth()
                && relativeY >= y && relativeY < y + getTankHeight()) {
                return i;
            }
        }
        return -1;
    }

    private int tankX(int index) {
        if (tile.getTankCount() <= 2) {
            return 191;
        }
        return 190 + index % 2 * 29;
    }

    private int tankY(int index) {
        if (tile.getTankCount() <= 2) {
            return 45 + index * 30;
        }
        return 45 + index / 2 * 20;
    }

    private int getTankWidth() {
        return tile.getTankCount() <= 2 ? 52 : 25;
    }

    private int getTankHeight() {
        return tile.getTankCount() <= 2 ? 20 : 16;
    }
}
