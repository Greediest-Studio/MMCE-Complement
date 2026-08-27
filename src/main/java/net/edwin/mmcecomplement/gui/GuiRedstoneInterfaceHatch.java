package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneInterfaceRegistry;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneValueDefinition;
import net.edwin.mmcecomplement.tile.TileRedstoneInterfaceHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalInputHatch;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.List;

/** Compact previous/next selector for a machine's registered value names. */
public class GuiRedstoneInterfaceHatch extends GuiContainer {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        "mmce_complement", "textures/gui/container/me_energy_bus.png");

    private final TileRedstoneInterfaceHatch tile;
    private GuiButton previousButton;
    private GuiButton nextButton;

    public GuiRedstoneInterfaceHatch(EntityPlayer player,
                                     TileRedstoneInterfaceHatch tile) {
        super(new ContainerRedstoneInterfaceHatch(player, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        previousButton = addButton(new GuiButton(0,
            guiLeft + 12, guiTop + 48, 24, 20, "<"));
        nextButton = addButton(new GuiButton(1,
            guiLeft + 140, guiTop + 48, 24, 20, ">"));
        refreshButtons();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        refreshButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id != 0 && button.id != 1) {
            super.actionPerformed(button);
            return;
        }
        List<String> names = tile.getAvailableNames();
        if (names.isEmpty()) {
            return;
        }
        int current = names.indexOf(tile.getSelectedName());
        int next;
        if (button.id == 0) {
            next = current <= 0 ? names.size() - 1 : current - 1;
        } else {
            next = current < 0 || current >= names.size() - 1 ? 0 : current + 1;
        }
        String name = names.get(next);
        tile.setSelectedName(name);
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("v", name);
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.SetHatchFieldMessage(tile.getPos(),
                tile.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_REDSTONE_INTERFACE_NAME, payload));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String titleKey = tile instanceof TileRedstoneSignalInputHatch
            ? "gui.mmce_complement.redstone_signal_input_hatch.title"
            : "gui.mmce_complement.redstone_signal_output_hatch.title";
        fontRenderer.drawString(I18n.format(titleKey), 8, 6, 0x404040);
        fontRenderer.drawString(
            I18n.format("gui.mmce_complement.redstone_interface_hatch.machine",
                tile.getBoundMachineId() == null
                    ? I18n.format("gui.mmce_complement.redstone_interface_hatch.unbound")
                    : tile.getBoundMachineId().toString()),
            8, 22, 0x404040);

        String name = tile.getSelectedName().isEmpty()
            ? I18n.format("gui.mmce_complement.redstone_interface_hatch.no_values")
            : tile.getSelectedName();
        drawCenteredString(fontRenderer, name, xSize / 2, 54, 0xFFFFFF);

        String detail = getDetail();
        drawCenteredString(fontRenderer, detail, xSize / 2, 72, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                    int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    private String getDetail() {
        if (tile instanceof TileRedstoneSignalInputHatch) {
            RedstoneValueDefinition definition = tile.getBoundMachineId() == null
                ? null : RedstoneInterfaceRegistry.get(tile.getBoundMachineId(),
                    tile.getSelectedName());
            int operator = definition == null ? 0 : definition.getOperator();
            return I18n.format(
                "gui.mmce_complement.redstone_interface_hatch.input_detail",
                ((TileRedstoneSignalInputHatch) tile).getReceivedSignalStrength(),
                I18n.format("gui.mmce_complement.redstone_interface_hatch.operator."
                    + operator));
        }
        return I18n.format(
            "gui.mmce_complement.redstone_interface_hatch.output_detail");
    }

    private void refreshButtons() {
        boolean enabled = !tile.getAvailableNames().isEmpty();
        previousButton.enabled = enabled;
        nextButton.enabled = enabled;
    }
}
