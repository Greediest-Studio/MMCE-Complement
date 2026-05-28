package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.fluxnetworks.api.gui.EnumNavigationTabs;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.button.NavigationButton;
import sonar.fluxnetworks.client.gui.tab.GuiTabConnections;

/**
 * Connections tab subclass that swaps FN's broken HOME / SELECTION routes for
 * our own {@link GuiFluxHatchHome} and {@link GuiFluxInputHatch}.
 */
@SideOnly(Side.CLIENT)
public class GuiHatchConnections extends GuiTabConnections {

    private final TileFluxHatchBase hatch;

    public GuiHatchConnections(EntityPlayer player, TileFluxHatchBase hatch) {
        super(player, hatch);
        this.hatch = hatch;
    }

    @Override
    public void initGui() {
        super.initGui();
        GuiFluxHatchHome.retainSupportedNavButtons(navigationButtons);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        if (button instanceof NavigationButton) {
            EnumNavigationTabs tab = ((NavigationButton) button).tab;
            if (tab == EnumNavigationTabs.TAB_HOME) {
                GuiFluxHatchHome.playClickSound();
                FMLCommonHandler.instance().showGuiScreen(new GuiFluxHatchHome(player, hatch));
                return;
            }
            if (tab == EnumNavigationTabs.TAB_SELECTION) {
                GuiFluxHatchHome.playClickSound();
                FMLCommonHandler.instance().showGuiScreen(new GuiFluxInputHatch(player, hatch));
                return;
            }
        }
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
    }
}
