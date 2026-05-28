package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.fluxnetworks.api.gui.EnumNavigationTabs;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.api.network.NetworkSettings;
import sonar.fluxnetworks.api.translate.FluxTranslate;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiTabCore;
import sonar.fluxnetworks.client.gui.button.NavigationButton;
import sonar.fluxnetworks.client.gui.button.SlidedSwitchButton;
import sonar.fluxnetworks.client.gui.button.TextboxButton;

/**
 * Home tab for the Wireless Flux Input Hatch.
 *
 * <p>Mirrors the layout of {@code sonar.fluxnetworks.client.gui.GuiFluxConnectorHome}
 * so the player gets the exact same look-and-feel as a stock Flux Point home
 * page. All editable fields are wired to {@link NetworkHandlerMMCE} packets
 * because FN's own packets are hard-coded against {@code TileFluxCore}.
 */
@SideOnly(Side.CLIENT)
public class GuiFluxHatchHome extends GuiTabCore {

    private final TileFluxHatchBase hatch;

    public TextboxButton fluxName;
    public TextboxButton priority;
    public TextboxButton limit;
    public TextboxButton bufferCap;
    public SlidedSwitchButton surge;
    public SlidedSwitchButton disableLimit;
    public SlidedSwitchButton chunkLoad;

    private int timer;

    public GuiFluxHatchHome(EntityPlayer player, TileFluxHatchBase hatch) {
        super(player, hatch);
        this.hatch = hatch;
    }

    @Override
    public EnumNavigationTabs getNavigationTab() {
        return EnumNavigationTabs.TAB_HOME;
    }

    @Override
    public void initGui() {
        super.initGui();
        configureNavigationButtons(EnumNavigationTabs.TAB_HOME, navigationTabs);
        retainSupportedNavButtons(navigationButtons);

        int outline = getNetworkColor() | 0xFF000000;

        // Text fields — positions match GuiFluxConnectorHome.initGui exactly.
        fluxName = TextboxButton.create(this,
                FluxTranslate.NAME.t() + ":", 0, fontRenderer, 16, 28, 144, 12)
                .setOutlineColor(outline);
        fluxName.setMaxStringLength(24);
        fluxName.setText(hatch.getRawCustomName());

        priority = TextboxButton.create(this,
                FluxTranslate.PRIORITY.t() + ":", 1, fontRenderer, 16, 45, 144, 12)
                .setOutlineColor(outline)
                .setDigitsOnly();
        priority.setMaxStringLength(5);
        priority.setText(String.valueOf(hatch.getRawPriority()));

        limit = TextboxButton.create(this,
                FluxTranslate.TRANSFER_LIMIT.t() + ":", 2, fontRenderer, 16, 62, 144, 12)
                .setOutlineColor(outline)
                .setDigitsOnly();
        limit.setMaxStringLength(19);
        limit.setText(String.valueOf(hatch.getRawLimit()));

        bufferCap = TextboxButton.create(this,
            I18n.format("gui.mmce_complement.max_buffer") + ":", 3, fontRenderer, 16, 79, 144, 12)
            .setOutlineColor(outline)
            .setDigitsOnly();
        bufferCap.setMaxStringLength(19);
        bufferCap.setText(String.valueOf(hatch.getBufferCapacityRaw()));

        // Slide switches — positions match GuiFluxConnectorHome.initGui exactly.
        surge = new SlidedSwitchButton(140, 120, 1, guiLeft, guiTop, hatch.getSurgeMode());
        disableLimit = new SlidedSwitchButton(140, 132, 2, guiLeft, guiTop, hatch.getDisableLimit());
        switches.add(surge);
        switches.add(disableLimit);

        chunkLoad = new SlidedSwitchButton(140, 144, 3, guiLeft, guiTop, hatch.isChunkLoadingRequested());
        switches.add(chunkLoad);

        textBoxes.add(fluxName);
        textBoxes.add(priority);
        textBoxes.add(limit);
        textBoxes.add(bufferCap);
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);

        int color = getNetworkColor();
        IFluxNetwork net = hatch.getNetwork();
        String netName = (net != null && !net.isInvalid())
                ? String.valueOf(net.getSetting(NetworkSettings.NETWORK_NAME))
                : "None";

        renderNetwork(netName, color, 20, 8);
        renderTransfer(hatch, 0xFFFFFF, 30, 100);

        fontRenderer.drawString(FluxTranslate.SURGE_MODE.t(), 20, 120, color);
        fontRenderer.drawString(FluxTranslate.DISABLE_LIMIT.t(), 20, 132, color);
        fontRenderer.drawString(FluxTranslate.CHUNK_LOADING.t(), 20, 144, color);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (fluxName != null && !fluxName.isFocused()) {
            String cur = hatch.getRawCustomName();
            if (!cur.equals(fluxName.getText())) {
                fluxName.setText(cur);
            }
        }
        if (priority != null && !priority.isFocused()) {
            String cur = String.valueOf(hatch.getRawPriority());
            if (!cur.equals(priority.getText())) {
                priority.setText(cur);
            }
        }
        if (limit != null && !limit.isFocused()) {
            String cur = String.valueOf(hatch.getRawLimit());
            if (!cur.equals(limit.getText())) {
                limit.setText(cur);
            }
        }
        if (bufferCap != null && !bufferCap.isFocused()) {
            String cur = String.valueOf(hatch.getBufferCapacityRaw());
            if (!cur.equals(bufferCap.getText())) {
                bufferCap.setText(cur);
            }
        }

        timer++;
        if (timer >= 20) {
            timer = 0;
        }
    }

    @Override
    public void onTextBoxChanged(TextboxButton box) {
        if (box == fluxName) {
            String v = fluxName.getText();
            if (v.length() > 24) v = v.substring(0, 24);
            NBTTagCompound t = new NBTTagCompound();
            t.setString("v", v);
            send(NetworkHandlerMMCE.FIELD_CUSTOM_NAME, t);
        } else if (box == priority) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("v", priority.getIntegerFromText(false));
            send(NetworkHandlerMMCE.FIELD_PRIORITY, t);
        } else if (box == limit) {
            long v = Math.min(limit.getLongFromText(true), hatch.getMaxTransferLimit());
            limit.setText(String.valueOf(v));
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("v", v);
            send(NetworkHandlerMMCE.FIELD_LIMIT, t);
        } else if (box == bufferCap) {
            long v = Math.min(bufferCap.getLongFromText(true), hatch.getBufferCapacityCeiling());
            bufferCap.setText(String.valueOf(v));
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("v", v);
            send(NetworkHandlerMMCE.FIELD_BUFFER_CAP, t);
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        // Intercept navigation buttons before letting GuiTabCore.switchTab run.
        // FN's static switchTab falls back to player.closeScreen() for HOME on
        // non-TileFluxCore connectors, and opens stock GuiTabSelection (whose
        // network-pick handler rejects our tile) for SELECTION — so we route
        // both to our subclasses ourselves.
        if (button instanceof NavigationButton) {
            EnumNavigationTabs tab = ((NavigationButton) button).tab;
            if (tab == EnumNavigationTabs.TAB_HOME) {
                return; // already on home
            }
            if (tab == EnumNavigationTabs.TAB_SELECTION) {
                playClickSound();
                net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .showGuiScreen(new GuiFluxInputHatch(player, hatch));
                return;
            }
            if (tab == EnumNavigationTabs.TAB_CONNECTION) {
                playClickSound();
                net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .showGuiScreen(new GuiHatchConnections(player, hatch));
                return;
            }
        }

        super.onButtonClicked(button, mouseX, mouseY, mouseButton);

        if (mouseButton != 0 || !(button instanceof SlidedSwitchButton)) {
            return;
        }
        SlidedSwitchButton sw = (SlidedSwitchButton) button;
        sw.switchButton();
        NBTTagCompound t = new NBTTagCompound();
        t.setBoolean("v", sw.slideControl);
        if (sw == surge) {
            send(NetworkHandlerMMCE.FIELD_SURGE_MODE, t);
        } else if (sw == disableLimit) {
            send(NetworkHandlerMMCE.FIELD_DISABLE_LIMIT, t);
        } else if (sw == chunkLoad) {
            send(NetworkHandlerMMCE.FIELD_CHUNK_LOAD, t);
        }
    }

    private int getNetworkColor() {
        IFluxNetwork n = hatch.getNetwork();
        if (n == null || n.isInvalid()) {
            return 0xFFFFFF;
        }
        Object o = n.getSetting(NetworkSettings.NETWORK_COLOR);
        return o instanceof Integer ? (Integer) o : 0xFFFFFF;
    }

    private void send(int fieldId, NBTTagCompound payload) {
        NetworkHandlerMMCE.CHANNEL.sendToServer(new NetworkHandlerMMCE.SetHatchFieldMessage(
                hatch.getPos(),
                hatch.getWorld().provider.getDimension(),
                fieldId,
                payload));
    }

    /**
     * Removes navigation buttons whose tabs we don't support. FN indexes the
     * underlying {@code navigationButtons} list by {@code tab.ordinal()} inside
     * {@code configureNavigationButtons}, so we must let it create all 8
     * buttons first and only prune the unwanted ones afterwards.
     */
    static void retainSupportedNavButtons(java.util.List<NavigationButton> buttons) {
        java.util.Iterator<NavigationButton> it = buttons.iterator();
        while (it.hasNext()) {
            EnumNavigationTabs tab = it.next().tab;
            if (tab != EnumNavigationTabs.TAB_HOME
                    && tab != EnumNavigationTabs.TAB_SELECTION
                    && tab != EnumNavigationTabs.TAB_CONNECTION) {
                it.remove();
            }
        }
        // FN positions buttons at x = 12 + 18*index using the original enum
        // order, so dropping middle tabs leaves visible gaps. Re-pack the x
        // coordinates of the survivors using the same formula.
        int i = 0;
        for (NavigationButton b : buttons) {
            b.x = 12 + 18 * i;
            i++;
        }
    }

    /** Plays FN's navigation button click sound on the master sound channel. */
    static void playClickSound() {
        net.minecraft.client.Minecraft.getMinecraft().getSoundHandler().playSound(
                net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(
                        sonar.fluxnetworks.common.registry.RegistrySounds.BUTTON_CLICK, 1.0F));
    }
}
