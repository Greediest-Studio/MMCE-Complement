package net.edwin.mmcecomplement.compat.ae.gui;

import hellfirepvp.modularmachinery.common.util.MiscUtils;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyBusBase;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class GuiMEEnergyBus extends GuiContainer {

        private static final ResourceLocation VANILLA_CONTAINER_BG =
            new ResourceLocation("mmce_complement", "textures/gui/container/me_energy_bus.png");

    private final TileMEEnergyBusBase tile;
    private GuiTextField bufferCapacityField;
    private long lastSentCapacity;

    public GuiMEEnergyBus(EntityPlayer player, TileMEEnergyBusBase tile) {
        super(new ContainerMEEnergyBus(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.bufferCapacityField = new GuiTextField(0, fontRenderer, guiLeft + 15, guiTop + 52, 146, 16);
        this.bufferCapacityField.setMaxStringLength(19);
        this.bufferCapacityField.setEnableBackgroundDrawing(true);
        this.bufferCapacityField.setTextColor(0xFFFFFF);
        this.bufferCapacityField.setDisabledTextColour(0xFFFFFF);
        this.bufferCapacityField.setText(String.valueOf(tile.getBufferCapacity()));
        this.lastSentCapacity = tile.getBufferCapacity();
    }

    @Override
    public void onGuiClosed() {
        pushBufferCapacity();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.bufferCapacityField.updateCursorCounter();

        if (!this.bufferCapacityField.isFocused()) {
            String current = String.valueOf(tile.getBufferCapacity());
            if (!current.equals(this.bufferCapacityField.getText())) {
                this.bufferCapacityField.setText(current);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        boolean wasFocused = bufferCapacityField.isFocused();
        bufferCapacityField.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasFocused && !bufferCapacityField.isFocused()) {
            pushBufferCapacity();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (this.bufferCapacityField.isFocused()) {
            boolean controlKey = MiscUtils.isTextBoxKey(keyCode);
            if (Character.isDigit(typedChar) || controlKey) {
                if (this.bufferCapacityField.textboxKeyTyped(typedChar, keyCode)) {
                    pushBufferCapacity();
                }
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                this.bufferCapacityField.setFocused(false);
                pushBufferCapacity();
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
        String title = tile.provideComponent().getIOType().name().equals("INPUT")
                ? I18n.format("gui.mmce_complement.me_energy_input_bus.title")
                : I18n.format("gui.mmce_complement.me_energy_output_bus.title");
        fontRenderer.drawString(title, 8, 6, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.me_energy_bus.buffer") + ": "
            + formatLong(tile.getCurrentEnergy()) + " FE", 8, 22, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.max_buffer"), 8, 40, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(VANILLA_CONTAINER_BG);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        this.bufferCapacityField.drawTextBox();
    }

    private void pushBufferCapacity() {
        long parsed = parseLong(this.bufferCapacityField.getText());
        if (parsed == lastSentCapacity) {
            return;
        }

        lastSentCapacity = parsed;
        NBTTagCompound payload = new NBTTagCompound();
        payload.setLong("v", parsed);
        NetworkHandlerMMCE.CHANNEL.sendToServer(new NetworkHandlerMMCE.SetHatchFieldMessage(
                tile.getPos(),
                tile.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_BUFFER_CAP,
                payload));
    }

    private static long parseLong(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        try {
            BigInteger value = new BigInteger(text);
            if (value.compareTo(BigInteger.ZERO) < 0) {
                return 0L;
            }
            BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
            if (value.compareTo(max) > 0) {
                return Long.MAX_VALUE;
            }
            return value.longValue();
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String formatLong(long value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }
}