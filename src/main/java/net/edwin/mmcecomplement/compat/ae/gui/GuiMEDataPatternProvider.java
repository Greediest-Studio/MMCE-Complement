package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.container.slot.AppEngSlot;
import github.kasuminova.mmce.client.gui.GuiMEPatternProvider;
import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceType;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The normal 36-slot pattern-provider GUI with a modal per-pattern data editor.
 * Keeping the editor in this screen leaves the original server container open,
 * so Escape and clicks outside the editor can return to the provider directly.
 */
public class GuiMEDataPatternProvider extends GuiMEPatternProvider {

    private static final int PANEL_WIDTH = 210;
    private static final int PANEL_HEIGHT = 126;

    private final TileMEDataPatternProvider dataOwner;
    private final List<SmartInterfaceType> interfaceTypes =
        new ArrayList<>();

    private int configuredSlot = -1;
    private int selectedTypeIndex = -1;
    private GuiTextField valueField;
    private GuiButton previousType;
    private GuiButton nextType;

    public GuiMEDataPatternProvider(TileMEDataPatternProvider owner,
                                    EntityPlayer player) {
        super(owner, player);
        this.dataOwner = owner;
    }

    @Override
    public void initGui() {
        super.initGui();
        replaceProviderTitle();
        if (configuredSlot >= 0) {
            createEditorControls();
        }
    }

    /** Replace the parent widget's title instead of painting over it. */
    private void replaceProviderTitle() {
        if (widgetController == null || widgetController.getWidgets().isEmpty()) {
            return;
        }
        if (widgetController.getWidgets().get(0) instanceof MultiLineLabel) {
            ((MultiLineLabel) widgetController.getWidgets().get(0)).setContents(
                Collections.singletonList(I18n.format(
                    "gui.mmce_complement.me_data_pattern_provider.title")));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (valueField != null) {
            valueField.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        if (configuredSlot >= 0) {
            submitConfiguration();
        }
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (configuredSlot < 0) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        // Render the unchanged provider behind the modal, but suppress its
        // hover state and tooltip while the editor owns input.
        super.drawScreen(Integer.MIN_VALUE / 4, Integer.MIN_VALUE / 4,
            partialTicks);
        drawEditor(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        updateHoveredSlot(mouseX, mouseY);
        Slot slot = hoveredSlot;
        if (!(slot instanceof AppEngSlot)
            || ((AppEngSlot) slot).getItemHandler() != dataOwner.getPatterns()
            || slot.getStack().isEmpty()) {
            super.renderHoveredToolTip(mouseX, mouseY);
            return;
        }
        int index = slot.getSlotIndex();
        if (index < 0 || index >= TileMEDataPatternProvider.PATTERN_SLOTS) {
            super.renderHoveredToolTip(mouseX, mouseY);
            return;
        }
        ItemStack stack = slot.getStack();
        List<String> tooltip = new ArrayList<>(getItemToolTip(stack));
        tooltip.add(0, I18n.format(
            "gui.mmce_complement.me_data_pattern_provider.tooltip.value",
            Float.toString(dataOwner.getPatternValue(index))));
        tooltip.addAll(widgetController.getHoverTooltips(new MousePos(
            mouseX, mouseY)));
        drawHoveringText(tooltip, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        if (configuredSlot >= 0) {
            handleEditorClick(mouseX, mouseY, mouseButton);
            return;
        }

        if (mouseButton == 2) {
            updateHoveredSlot(mouseX, mouseY);
            Slot slot = hoveredSlot;
            if (slot instanceof AppEngSlot
                && ((AppEngSlot) slot).getItemHandler()
                    == dataOwner.getPatterns()
                && !slot.getStack().isEmpty()) {
                openEditor(slot.getSlotIndex());
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (configuredSlot < 0) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeEditor();
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN
            || keyCode == Keyboard.KEY_NUMPADENTER) {
            submitConfiguration();
            valueField.setFocused(false);
            return;
        }
        if (valueField.isFocused()
            && (Character.isDigit(typedChar) || typedChar == '-'
                || typedChar == '+' || typedChar == '.'
                || typedChar == 'e' || typedChar == 'E'
                || MiscUtils.isTextBoxKey(keyCode))) {
            valueField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public TileMEDataPatternProvider getOwner() {
        return dataOwner;
    }

    private void openEditor(int slot) {
        if (slot < 0 || slot >= TileMEDataPatternProvider.PATTERN_SLOTS) {
            return;
        }
        configuredSlot = slot;
        refreshInterfaceTypes();
        createEditorControls();
        Keyboard.enableRepeatEvents(true);
    }

    private void closeEditor() {
        submitConfiguration();
        configuredSlot = -1;
        selectedTypeIndex = -1;
        valueField = null;
        previousType = null;
        nextType = null;
        interfaceTypes.clear();
        Keyboard.enableRepeatEvents(false);
    }

    private void createEditorControls() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        valueField = new GuiTextField(0, fontRenderer,
            left + 70, top + 73, 126, 16);
        valueField.setMaxStringLength(16);
        valueField.setText(Float.toString(
            dataOwner.getPatternValue(configuredSlot)));
        valueField.setFocused(true);

        previousType = new GuiButton(1, left + 70, top + 43,
            20, 18, "<");
        nextType = new GuiButton(2, left + 176, top + 43,
            20, 18, ">");
        updateTypeButtons();
    }

    private void drawEditor(int mouseX, int mouseY, float partialTicks) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        drawGradientRect(0, 0, width, height, 0xB0101010, 0xD0101010);
        drawRect(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT,
            0xFF1D1D1D);
        drawRect(left + 1, top + 1, left + PANEL_WIDTH - 1,
            top + PANEL_HEIGHT - 1, 0xFFC6C6C6);
        drawRect(left + 4, top + 4, left + PANEL_WIDTH - 4,
            top + PANEL_HEIGHT - 4, 0xFF373737);

        drawCenteredString(fontRenderer, I18n.format(
            "gui.mmce_complement.me_data_pattern_provider.config.title",
            configuredSlot + 1), width / 2, top + 11, 0xFFFFFF);
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_data_pattern_provider.config.interface"),
            left + 14, top + 48, 0xFFFFFF);
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_data_pattern_provider.config.value"),
            left + 14, top + 77, 0xFFFFFF);

        String typeName = getSelectedTypeDisplayName();
        int maxWidth = 82;
        String trimmed = fontRenderer.trimStringToWidth(typeName, maxWidth);
        drawCenteredString(fontRenderer, trimmed,
            left + 133, top + 48, 0xFFFFFF);

        previousType.drawButton(Minecraft.getMinecraft(), mouseX, mouseY,
            partialTicks);
        nextType.drawButton(Minecraft.getMinecraft(), mouseX, mouseY,
            partialTicks);
        valueField.drawTextBox();

        drawCenteredString(fontRenderer, I18n.format(
            "gui.mmce_complement.me_data_pattern_provider.config.return"),
            width / 2, top + 103, 0xB0B0B0);
    }

    private void handleEditorClick(int mouseX, int mouseY, int mouseButton) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        if (mouseX < left || mouseX >= left + PANEL_WIDTH
            || mouseY < top || mouseY >= top + PANEL_HEIGHT) {
            closeEditor();
            return;
        }

        if (mouseButton != 0) {
            return;
        }
        valueField.mouseClicked(mouseX, mouseY, mouseButton);
        Minecraft minecraft = Minecraft.getMinecraft();
        SoundHandler sounds = minecraft.getSoundHandler();
        if (previousType.mousePressed(minecraft, mouseX, mouseY)) {
            cycleType(-1);
            previousType.playPressSound(sounds);
        } else if (nextType.mousePressed(minecraft, mouseX, mouseY)) {
            cycleType(1);
            nextType.playPressSound(sounds);
        }
    }

    private void cycleType(int direction) {
        if (interfaceTypes.size() <= 1) {
            return;
        }
        selectedTypeIndex = (selectedTypeIndex + direction
            + interfaceTypes.size()) % interfaceTypes.size();
        updateTypeButtons();
        submitConfiguration();
    }

    private void updateTypeButtons() {
        boolean canCycle = interfaceTypes.size() > 1;
        if (previousType != null) previousType.enabled = canCycle;
        if (nextType != null) nextType.enabled = canCycle;
    }

    private void refreshInterfaceTypes() {
        interfaceTypes.clear();
        Map<String, SmartInterfaceType> common = null;
        for (SmartInterfaceData data : dataOwner.getBoundData()) {
            DynamicMachine machine = MachineRegistry.getRegistry()
                .getMachine(data.getParent());
            if (machine == null) {
                continue;
            }
            if (common == null) {
                common = new LinkedHashMap<>(machine.getSmartInterfaceTypes());
            } else {
                common.keySet().retainAll(
                    machine.getSmartInterfaceTypes().keySet());
            }
        }
        if (common != null) {
            interfaceTypes.addAll(common.values());
            Collections.sort(interfaceTypes);
        }

        String selected = dataOwner.getSelectedInterfaceType();
        selectedTypeIndex = -1;
        for (int i = 0; i < interfaceTypes.size(); i++) {
            if (interfaceTypes.get(i).getType().equals(selected)) {
                selectedTypeIndex = i;
                break;
            }
        }
        if (selectedTypeIndex < 0 && !interfaceTypes.isEmpty()) {
            selectedTypeIndex = 0;
        }
    }

    private String getSelectedType() {
        if (selectedTypeIndex >= 0
            && selectedTypeIndex < interfaceTypes.size()) {
            return interfaceTypes.get(selectedTypeIndex).getType();
        }
        return dataOwner.getSelectedInterfaceType();
    }

    private String getSelectedTypeDisplayName() {
        if (selectedTypeIndex < 0
            || selectedTypeIndex >= interfaceTypes.size()) {
            String selected = dataOwner.getSelectedInterfaceType();
            return selected == null || selected.isEmpty()
                ? I18n.format("gui.smartinterface.notfound") : selected;
        }
        SmartInterfaceType type = interfaceTypes.get(selectedTypeIndex);
        String header = type.getHeaderInfo();
        String translated = header == null || header.isEmpty()
            ? "" : I18n.format(header);
        return translated.isEmpty() ? type.getType()
            : translated + " (" + type.getType() + ")";
    }

    private void submitConfiguration() {
        if (configuredSlot < 0 || valueField == null) {
            return;
        }
        final float value;
        try {
            value = Float.parseFloat(valueField.getText());
        } catch (NumberFormatException ignored) {
            valueField.setText(Float.toString(
                dataOwner.getPatternValue(configuredSlot)));
            return;
        }
        if (!Float.isFinite(value)) {
            valueField.setText(Float.toString(
                dataOwner.getPatternValue(configuredSlot)));
            return;
        }

        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("slot", configuredSlot);
        String selectedType = getSelectedType();
        payload.setString("type", selectedType == null ? "" : selectedType);
        payload.setFloat("value", value);
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.SetHatchFieldMessage(
                dataOwner.getPos(),
                dataOwner.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_ME_DATA_PATTERN, payload));
    }
}
