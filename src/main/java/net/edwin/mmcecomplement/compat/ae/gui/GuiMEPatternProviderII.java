package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.client.gui.widgets.GuiCustomSlot;
import appeng.fluids.client.gui.widgets.GuiFluidTank;
import github.kasuminova.mmce.client.gui.GuiMEPatternProvider;
import github.kasuminova.mmce.client.gui.slot.GuiFullCapFluidTank;
import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.widget.Button;
import github.kasuminova.mmce.client.gui.widget.Button4State;
import github.kasuminova.mmce.client.gui.widget.MultiLineLabel;
import github.kasuminova.mmce.client.gui.widget.base.WidgetController;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import github.kasuminova.mmce.client.gui.widget.container.Row;
import github.kasuminova.mmce.common.network.PktMEPatternProviderAction;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.base.Mods;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 418x268 GUI drawn in the same flat AE/MMCE style as the original. */
public class GuiMEPatternProviderII extends GuiMEPatternProvider {

    public static final int GUI_WIDTH = 418;
    public static final int GUI_HEIGHT = 268;
    public static final int RIGHT_PANEL_X = 336;
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        Tags.MOD_ID, "textures/gui/me_pattern_provider_ii.png");

    private final TileMEPatternProviderII ownerII;

    public GuiMEPatternProviderII(TileMEPatternProviderII owner,
                                  EntityPlayer player) {
        super(owner, player);
        ownerII = owner;
        inventorySlots = new ContainerMEPatternProviderII(owner, player);
        xSize = GUI_WIDTH;
        ySize = GUI_HEIGHT;
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;

        widgetController = new WidgetController(
            WidgetGui.of(this, xSize, ySize, guiLeft, guiTop));
        addLabel("gui.mmce_complement.me_pattern_provider_ii.title", 7, 11);
        addLabel("gui.mepatternprovider.cached", RIGHT_PANEL_X + 6, 11);
        addLabel("gui.mepatternprovider.inventory", 7, 173);
        addLabel("gui.mepatternprovider.single_inv", RIGHT_PANEL_X + 6, 194);

        stackList.setMaxStackPerRow(3);
        stackList.setWidthHeight(69, 126);
        stackList.setAbsXY(RIGHT_PANEL_X + 6, 27);

        Button4State returnItems = new Button4State();
        returnItems
            .setMouseDownTexture(212, 214)
            .setHoveredTexture(194, 214)
            .setTexture(176, 214)
            .setTextureLocation(GuiMEPatternProvider.GUI_TEXTURE)
            .setTooltipFunction(button -> {
                List<String> tooltips = new ArrayList<>();
                tooltips.add(I18n.format("gui.mepatternprovider.return_items"));
                tooltips.add(I18n.format(
                    "gui.mepatternprovider.return_items.desc"));
                return tooltips;
            })
            .setOnClickedListener(button -> ModularMachinery.NET_CHANNEL
                .sendToServer(new PktMEPatternProviderAction(
                    PktMEPatternProviderAction.Action.RETURN_ITEMS)))
            .setWidthHeight(16, 16);

        Button singleInventoryTip = new Button();
        singleInventoryTip
            .setTextureLocation(GuiMEPatternProvider.GUI_TEXTURE)
            .setTexture(230, 214)
            .setHoveredTexture(241, 214)
            .setTooltipFunction(button -> Collections.singletonList(
                I18n.format("gui.mepatternprovider.single_inv.desc")))
            .setWidthHeight(9, 11)
            .setAbsXY(RIGHT_PANEL_X + 66, 193);

        Row buttons = new Row();
        buttons.addWidgets(returnItems.setMarginRight(2), workModeSetting)
            .setAbsXY(RIGHT_PANEL_X + 41, 7);

        widgetController.addWidget(stackList);
        widgetController.addWidget(buttons);
        widgetController.addWidget(singleInventoryTip);
        updateGUIState();
    }

    private void addLabel(String key, int x, int y) {
        widgetController.addWidget(new MultiLineLabel(
            Collections.singletonList(I18n.format(key)))
            .setAutoWrap(false).setMargin(0).setAbsXY(x, y));
    }

    @Override
    public void initGui() {
        super.initGui();
        guiSlots.removeIf(slot -> slot instanceof GuiFluidTank);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Object> rawButtonList = (List) buttonList;
        rawButtonList.removeIf(button -> button instanceof GuiFluidTank);

        for (int slot = 0;
             slot < TileMEPatternProviderII.SUB_FLUID_SLOTS; slot++) {
            GuiFullCapFluidTank tank = new GuiFullCapFluidTank(
                ownerII.getSubFluidHandler(), slot, slot,
                RIGHT_PANEL_X + 61,
                ContainerMEPatternProviderII.SUB_STORAGE_Y + slot * 18,
                16, 16);
            if (Mods.AE2EL.isPresent()) {
                guiSlots.add(tank);
            } else {
                ObfuscationReflectionHelper.setPrivateValue(
                    GuiCustomSlot.class, tank, getGuiLeft(), "x");
                ObfuscationReflectionHelper.setPrivateValue(
                    GuiCustomSlot.class, tank, getGuiTop(), "y");
                rawButtonList.add(tank);
            }
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0,
            xSize, ySize, GUI_WIDTH, GUI_HEIGHT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        // Draw widgets and slots without invoking the original 256x196
        // background renderer.
        widgetController.render(new MousePos(mouseX, mouseY), true);
    }

    @Override
    public TileMEPatternProviderII getOwner() {
        return ownerII;
    }
}
