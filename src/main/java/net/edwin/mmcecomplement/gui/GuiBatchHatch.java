package net.edwin.mmcecomplement.gui;

import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.math.BigInteger;

public class GuiBatchHatch extends GuiContainer {

    private static final ResourceLocation BACKGROUND =
        new ResourceLocation("mmce_complement", "textures/gui/container/me_energy_bus.png");

    private final TileBatchHatch tile;
    private GuiTextField maxBatchTimeField;
    private int lastSentMaxBatchTime;

    public GuiBatchHatch(EntityPlayer player, TileBatchHatch tile) {
        super(new ContainerBatchHatch(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.maxBatchTimeField = new GuiTextField(0, fontRenderer,
            guiLeft + 15, guiTop + 52, 146, 16);
        this.maxBatchTimeField.setMaxStringLength(10);
        this.maxBatchTimeField.setEnableBackgroundDrawing(true);
        this.maxBatchTimeField.setTextColor(0xFFFFFF);
        this.maxBatchTimeField.setDisabledTextColour(0xFFFFFF);
        this.maxBatchTimeField.setText(String.valueOf(tile.getMaxBatchTime()));
        this.lastSentMaxBatchTime = tile.getMaxBatchTime();
    }

    @Override
    public void onGuiClosed() {
        pushMaxBatchTime();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        maxBatchTimeField.updateCursorCounter();
        if (!maxBatchTimeField.isFocused()) {
            String current = String.valueOf(tile.getMaxBatchTime());
            if (!current.equals(maxBatchTimeField.getText())) {
                maxBatchTimeField.setText(current);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        boolean wasFocused = maxBatchTimeField.isFocused();
        maxBatchTimeField.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasFocused && !maxBatchTimeField.isFocused()) {
            pushMaxBatchTime();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (maxBatchTimeField.isFocused()) {
            boolean controlKey = MiscUtils.isTextBoxKey(keyCode);
            if (Character.isDigit(typedChar) || controlKey) {
                if (maxBatchTimeField.textboxKeyTyped(typedChar, keyCode)) {
                    pushMaxBatchTime();
                }
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                maxBatchTimeField.setFocused(false);
                pushMaxBatchTime();
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.mmce_complement.batch_hatch.title"),
            8, 6, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.batch_hatch.description"),
            8, 22, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.batch_hatch.max_time"),
            8, 40, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"),
            8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        maxBatchTimeField.drawTextBox();
    }

    private void pushMaxBatchTime() {
        int parsed = parseInt(maxBatchTimeField.getText());
        if (parsed == lastSentMaxBatchTime) {
            return;
        }
        lastSentMaxBatchTime = parsed;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("v", parsed);
        NetworkHandlerMMCE.CHANNEL.sendToServer(new NetworkHandlerMMCE.SetHatchFieldMessage(
            tile.getPos(), tile.getWorld().provider.getDimension(),
            NetworkHandlerMMCE.FIELD_BATCH_MAX_TIME, payload));
    }

    private static int parseInt(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            BigInteger value = new BigInteger(text);
            if (value.signum() < 0) {
                return 0;
            }
            BigInteger max = BigInteger.valueOf(Integer.MAX_VALUE);
            return value.compareTo(max) > 0 ? Integer.MAX_VALUE : value.intValue();
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
