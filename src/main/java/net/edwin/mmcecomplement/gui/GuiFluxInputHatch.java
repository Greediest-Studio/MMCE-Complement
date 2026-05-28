package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.fluxnetworks.api.gui.EnumNavigationTabs;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.button.NavigationButton;
import sonar.fluxnetworks.client.gui.tab.GuiTabSelection;

/**
 * Client-side GUI for the Wireless Flux Input Hatch.
 *
 * <p>Subclasses Flux Networks' {@link GuiTabSelection} to reuse the full FN
 * styling (background panel, network list, sort/search controls and the side
 * navigation buttons). After the player picks a network the standard FN flow
 * fires {@code setConnectedNetwork} which sends a {@code PacketTile} whose
 * server handler rejects anything that is not a {@code TileFluxCore} — so we
 * override {@link #onElementClicked(IFluxNetwork, int)} and dispatch our own
 * {@link NetworkHandlerMMCE.SetHatchNetworkMessage} instead.
 */
@SideOnly(Side.CLIENT)
public class GuiFluxInputHatch extends GuiTabSelection {

    private final TileFluxHatchBase hatch;

    public GuiFluxInputHatch(EntityPlayer player, TileFluxHatchBase hatch) {
        super(player, hatch);
        this.hatch = hatch;
    }

    @Override
    public void initGui() {
        super.initGui();
        GuiFluxHatchHome.retainSupportedNavButtons(navigationButtons);
    }

    @Override
    protected void onElementClicked(IFluxNetwork network, int mouseButton) {
        if (mouseButton != 0 || network == null) {
            return;
        }
        this.selectedNetwork = network;
        NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.SetHatchNetworkMessage(
                        hatch.getPos(),
                        hatch.getWorld().provider.getDimension(),
                        network.getNetworkID()));
    }

    /**
     * Suppress the HOME navigation button's default switch (which would call
     * {@code player.closeScreen()} for a non-{@code TileFluxCore} connector)
     * and open our custom {@link GuiFluxHatchHome} instead. Other tabs are
     * forwarded to the default handler so the player can still navigate to
     * Connection / Statistics / Settings / Wireless / Selection / Create.
     */
    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        if (button instanceof NavigationButton) {
            EnumNavigationTabs tab = ((NavigationButton) button).tab;
            if (tab == EnumNavigationTabs.TAB_HOME) {
                GuiFluxHatchHome.playClickSound();
                net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .showGuiScreen(new GuiFluxHatchHome(player, hatch));
                return;
            }
            if (tab == EnumNavigationTabs.TAB_CONNECTION) {
                GuiFluxHatchHome.playClickSound();
                net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .showGuiScreen(new GuiHatchConnections(player, hatch));
                return;
            }
        }
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
    }
}
