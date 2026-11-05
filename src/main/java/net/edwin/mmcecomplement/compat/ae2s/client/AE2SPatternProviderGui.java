// Adapted from AppliedCompatibility 1.0.1 (5247000), LGPL-3.0-or-later.
// Complement owns the expanded layout and its three fluid slots.
package net.edwin.mmcecomplement.compat.ae2s.client;

import github.formlessdragon.appcompat.client.gui.mmce.GuiMEBusBase;
import github.formlessdragon.appcompat.client.gui.mmce.PatternProviderTextureButton;
import net.edwin.mmcecomplement.compat.ae2s.AE2SPatternProviderContainer;

import github.formlessdragon.appcompat.bridge.mmce.MMCEEnums;
import ae2.api.client.AEKeyRendering;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AmountFormat;
import ae2.api.stacks.GenericStack;
import ae2.client.gui.me.common.StackSizeRenderer;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.GuiStyleManager;
import ae2.core.localization.Tooltips;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.tile.MEPatternProvider.WorkModeSetting;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AE2SPatternProviderGui extends GuiMEBusBase<AE2SPatternProviderContainer> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("modularmachinery", "textures/gui/mepatternprovider.png");
    private final int LIST_X;
    private static final int LIST_Y = 27;
    private static final int LIST_WIDTH = 69;
    private static final int LIST_HEIGHT = 126;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_COLUMNS = 3;
    private final int SCROLL_X;
    private static final int SCROLL_Y = 28;
    private static final int SCROLL_WIDTH = 9;
    private static final int SCROLL_HEIGHT = 124;
    private static final int SCROLL_KNOB_HEIGHT = 18;
    private static final int VISIBLE_ROWS = 7;
    private final int TANK_X;
    private final int TANK_Y;
    private static final int TANK_SIZE = 16;

    protected final AE2SPatternProviderContainer container;
    private PatternProviderTextureButton workModeButton;
    private int scrollRow;
    private boolean draggingScrollbar;

    public AE2SPatternProviderGui(final MEPatternProvider te, final EntityPlayer player) {
        super(new AE2SPatternProviderContainer(te, player), player.inventory,
            GuiStyleManager.loadStyleDoc(te.getPatterns().getSlots() > 36 ? "/screens/mmce_complement_provider_ii.json" : "/screens/appcompat_me_pattern_provider.json"));
        this.container = (AE2SPatternProviderContainer) this.inventorySlots;
        LIST_X = container.isExpanded() ? 342 : 180;
        SCROLL_X = LIST_X + 59;
        TANK_X = container.isExpanded() ? 397 : 232;
        TANK_Y = container.isExpanded() ? 208 : 172;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        // AE2S style grids have nine columns. Position the expanded inventory
        // after the normal style pass to keep all 144 slots in eighteen columns.
        if (container.isExpanded()) {
            int patterns = container.getOwner().getPatterns().getSlots();
            int subItems = container.getOwner().getSubItemHandler().getSlots();
            for (int i = 0; i < patterns; i++) {
                net.minecraft.inventory.Slot slot = inventorySlots.inventorySlots.get(i);
                slot.xPos = 8 + i % 18 * 18;
                slot.yPos = 28 + i / 18 * 18;
            }
            for (int i = 0; i < subItems; i++) {
                net.minecraft.inventory.Slot slot = inventorySlots.inventorySlots.get(patterns + i);
                slot.xPos = 343 + i % 3 * 18;
                slot.yPos = 208 + i / 3 * 18;
            }
        }
        this.addButton(new PatternProviderTextureButton(1, this.guiLeft + LIST_X + 35, this.guiTop + 7, 16, 16,
            TEXTURE, 176, 194, 212, 214, this::buildReturnTooltip, this.container::sendReturnItems));
        this.workModeButton = new PatternProviderTextureButton(0, this.guiLeft + LIST_X + 53, this.guiTop + 7, 16, 16,
            TEXTURE, 194, 212, 230, 196, this::buildWorkModeTooltip, this.container::sendCycleWorkMode);
        this.refreshWorkModeButton();
        this.addButton(this.workModeButton);
        this.addButton(new PatternProviderTextureButton(2, this.guiLeft + LIST_X + 60, this.guiTop + (container.isExpanded() ? 193 : 157), 9, 11,
            TEXTURE, 230, 241, 241, 214, this::buildSingleInvTooltip, () -> {
        }));
    }

    @Override
    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button instanceof PatternProviderTextureButton) {
            ((PatternProviderTextureButton) button).press();
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
        final int relX = mouseX - guiLeft;
        final int relY = mouseY - guiTop;
        for (final GuiButton button : this.buttonList) {
            if (button instanceof PatternProviderTextureButton && ((PatternProviderTextureButton) button).mousePressed(this.mc, mouseX, mouseY)) {
                ((PatternProviderTextureButton) button).press();
                return;
            }
        }
        if (isInSubFluidTank(relX, relY)) {
            if (mouseButton == 0) {
                this.container.sendFluidAction(fluidSlotAt(relY), false);
                return;
            }
            if (mouseButton == 1) {
                this.container.sendFluidAction(fluidSlotAt(relY), true);
                return;
            }
            return;
        }
        if (isInScrollbar(relX, relY) && getScrollableRows() > 0) {
            this.draggingScrollbar = true;
            updateScrollFromMouse(relY);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(final int mouseX, final int mouseY, final int state) {
        this.draggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        final int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        final int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        final int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        final int relX = mx - this.guiLeft;
        final int relY = my - this.guiTop;
        if (isInVirtualList(relX, relY) || isInScrollbar(relX, relY)) {
            this.scrollBy(wheel > 0 ? -1 : 1);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.refreshWorkModeButton();
        this.scrollRow = MathHelper.clamp(this.scrollRow, 0, getScrollableRows());
        if (this.draggingScrollbar) {
            final int relY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1 - this.guiTop;
            updateScrollFromMouse(relY);
        }
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        for (int i = 0; i < container.subFluid.size(); i++) drawSubFluidTank(i);
        drawVirtualItems(mouseX, mouseY);
        drawScrollbar(mouseX, mouseY);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY, final float partialTicks) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderHoveredToolTip(final int mouseX, final int mouseY) {
        for (final GuiButton button : this.buttonList) {
            if (button instanceof PatternProviderTextureButton && ((PatternProviderTextureButton) button).isMouseOver(mouseX, mouseY)) {
                final List<ITextComponent> tooltip = ((PatternProviderTextureButton) button).getTooltipMessage();
                if (!tooltip.isEmpty()) {
                    final List<String> lines = new ObjectArrayList<>(tooltip.size());
                    for (final ITextComponent line : tooltip) {
                        lines.add(line.getFormattedText());
                    }
                    this.drawHoveringText(lines, mouseX, mouseY);
                    return;
                }
            }
        }
        final int relX = mouseX - this.guiLeft;
        final int relY = mouseY - this.guiTop;
        if (isInSubFluidTank(relX, relY)) {
            final List<String> tooltip = buildSubFluidTooltip(fluidSlotAt(relY));
            if (!tooltip.isEmpty()) {
                this.drawHoveringText(tooltip, mouseX, mouseY);
                return;
            }
        }
        final List<String> stackTooltip = buildVirtualStackTooltip(relX, relY);
        if (!stackTooltip.isEmpty()) {
            this.drawHoveringText(stackTooltip, mouseX, mouseY);
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private void drawVirtualItems(final int mouseX, final int mouseY) {
        final int relMouseX = mouseX - this.guiLeft;
        final int relMouseY = mouseY - this.guiTop;
        GlStateManager.pushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((this.guiLeft + LIST_X) * this.mc.displayWidth / this.width,
            this.mc.displayHeight - (this.guiTop + LIST_Y + LIST_HEIGHT) * this.mc.displayHeight / this.height,
            LIST_WIDTH * this.mc.displayWidth / this.width,
            LIST_HEIGHT * this.mc.displayHeight / this.height);
        GlStateManager.color(1F, 1F, 1F, 1F);
        final List<GenericStack> stacks = this.container.stackList.stacks();
        final int startIndex = this.scrollRow * SLOT_COLUMNS;
        final int visibleCount = MathHelper.clamp(stacks.size() - startIndex, 0, VISIBLE_ROWS * SLOT_COLUMNS);
        try {
            for (int i = 0; i < visibleCount; i++) {
                final GenericStack stack = stacks.get(startIndex + i);
                if (stack == null) {
                    continue;
                }
                final int column = i % SLOT_COLUMNS;
                final int row = i / SLOT_COLUMNS;
                final int x = LIST_X + column * SLOT_SIZE;
                final int y = LIST_Y + row * SLOT_SIZE;
                GlStateManager.color(1F, 1F, 1F, 1F);
                Blitter.texture(TEXTURE).src(220, 232, 18, 18).dest(x, y, 18, 18).blit();
                GlStateManager.color(1F, 1F, 1F, 1F);
                if (stack.what() instanceof AEKey) {
                    AEKeyRendering.drawInGui(this.mc, x + 1, y + 1, stack.what());
                } else {
                    throw new IllegalStateException("Unsupported generic stack type: " + stack.what());
                }
                GlStateManager.color(1F, 1F, 1F, 1F);
                drawStackAmount(stack, x + 1, y + 1);
                if (isInVirtualList(relMouseX, relMouseY) && isPointInRegion(x, y, 16, 16, mouseX, mouseY)) {
                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    this.drawGradientRect(x, y, x + 16, y + 16, 0x669cd3ff, 0x669cd3ff);
                    GlStateManager.enableDepth();
                    GlStateManager.disableLighting();
                }
            }
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.popMatrix();
            restoreGuiRenderState();
        }
    }

    private void drawStackAmount(final GenericStack stack, final int x, final int y) {
        final String text = stack.what().formatAmount(stack.amount(), AmountFormat.SLOT);
        StackSizeRenderer.renderSizeLabel(this.fontRenderer, x, y, text, false);
    }

    private void drawScrollbar(final int mouseX, final int mouseY) {
        final int knobY = getScrollbarKnobY();
        final int relMouseX = mouseX - this.guiLeft;
        final int relMouseY = mouseY - this.guiTop;
        final int scrollU = getScrollableRows() > 0 ? (this.draggingScrollbar ? 198 : isInScrollbar(relMouseX, relMouseY) ? 187 : 176) : 209;
        Blitter.texture(TEXTURE).src(scrollU, 232, SCROLL_WIDTH, SCROLL_KNOB_HEIGHT).dest(SCROLL_X, knobY, SCROLL_WIDTH, SCROLL_KNOB_HEIGHT).blit();
    }

    private void drawSubFluidTank(int slot) {
        final GenericStack stack = this.container.subFluid.get(slot);
        if (stack == null || !(stack.what() instanceof AEFluidKey)) {
            return;
        }
        final FluidStack fluidStack = ((AEFluidKey) stack.what()).toStack((int) Math.min(stack.amount(), Integer.MAX_VALUE));
        final Fluid fluid = fluidStack.getFluid();
        if (fluid == null) {
            return;
        }
        final ResourceLocation still = fluid.getStill(fluidStack);
        final ResourceLocation texture = still != null ? still : fluid.getStill();
        if (texture == null) {
            return;
        }
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        final int color = fluid.getColor();
        GlStateManager.color(
            ((color >> 16) & 255) / 255F,
            ((color >> 8) & 255) / 255F,
            (color & 255) / 255F);
        try {
            final TextureAtlasSprite sprite = this.mc.getTextureMapBlocks().getAtlasSprite(texture.toString());
            final int remainder = TANK_SIZE % 16;
            if (remainder > 0) {
                this.drawTexturedModalRect(TANK_X, (TANK_Y + slot * 18) + TANK_SIZE - remainder, sprite, 16, remainder);
            }
            for (int i = 0; i < TANK_SIZE / 16; i++) {
                this.drawTexturedModalRect(TANK_X, (TANK_Y + slot * 18) + TANK_SIZE - remainder - (i + 1) * 16, sprite, 16, 16);
            }
        } finally {
            restoreGuiRenderState();
        }
    }

    private void restoreGuiRenderState() {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        this.mc.getTextureManager().bindTexture(TEXTURE);
    }

    private List<ITextComponent> buildReturnTooltip() {
        final List<ITextComponent> tooltip = new ObjectArrayList<>();
        tooltip.add(new TextComponentString(I18n.format("gui.mepatternprovider.return_items")));
        tooltip.add(new TextComponentString(I18n.format("gui.mepatternprovider.return_items.desc")));
        return tooltip;
    }

    private List<ITextComponent> buildWorkModeTooltip() {
        final List<ITextComponent> tooltip = new ObjectArrayList<>();
        final WorkModeSetting current = currentMode();
        tooltip.add(new TextComponentString(I18n.format("gui.mepatternprovider.work_mode.desc")));
        tooltip.add(new TextComponentString(prefixCurrent(current == WorkModeSetting.DEFAULT) + I18n.format("gui.mepatternprovider.default.desc")));
        tooltip.add(new TextComponentString(prefixCurrent(current == WorkModeSetting.BLOCKING_MODE) + I18n.format("gui.mepatternprovider.blocking_mode.desc")));
        tooltip.add(new TextComponentString(prefixCurrent(current == WorkModeSetting.CRAFTING_LOCK_MODE) + I18n.format("gui.mepatternprovider.crafting_lock_mode.desc")));
        tooltip.add(new TextComponentString(prefixCurrent(current == WorkModeSetting.ENHANCED_BLOCKING_MODE) + I18n.format("gui.mepatternprovider.enhanced_blocking_mode.desc")));
        tooltip.add(new TextComponentString(prefixCurrent(current == WorkModeSetting.ISOLATION_INPUT) + I18n.format("gui.mepatternprovider.isolation_input.desc")));
        return tooltip;
    }

    private List<ITextComponent> buildSingleInvTooltip() {
        return Collections.singletonList(new TextComponentString(I18n.format("gui.mepatternprovider.single_inv.desc")));
    }

    private String prefixCurrent(final boolean current) {
        return current ? I18n.format("gui.mepatternprovider.current") : "";
    }

    private void refreshWorkModeButton() {
        if (this.workModeButton == null) {
            return;
        }
        final WorkModeSetting mode = currentMode();
        final int overlayU;
        switch (mode) {
            case DEFAULT: overlayU = 140; break;
            case BLOCKING_MODE: overlayU = 158; break;
            case CRAFTING_LOCK_MODE: overlayU = 176; break;
            case ENHANCED_BLOCKING_MODE: overlayU = 122; break;
            default: overlayU = 104; break;
        }
        this.workModeButton.setOverlay(overlayU, 196, 16, 16);
    }

    private WorkModeSetting currentMode() {
        return MMCEEnums.workMode(this.container.workMode);
    }

    private List<String> buildSubFluidTooltip(int slot) {
        final List<String> tooltip = new ObjectArrayList<>();
        final GenericStack stack = this.container.subFluid.get(slot);
        if (stack != null && stack.what() instanceof AEFluidKey) {
            final long amount = stack.amount();
            final int capacity = getSubFluidCapacity();
            tooltip.add(stack.what().getDisplayName().getFormattedText());
            tooltip.add(amount + " / " + capacity + " mB");
        }
        tooltip.add(TextFormatting.GRAY + Tooltips.getMouseButtonText(0).getFormattedText() + ": " + I18n.format("gui.ae2.caner.fill"));
        tooltip.add(TextFormatting.GRAY + Tooltips.getMouseButtonText(1).getFormattedText() + ": " + I18n.format("gui.ae2.caner.empty"));
        return tooltip;
    }

    private int getSubFluidCapacity() { return Math.max(1, container.getOwner().getSubFluidHandler().getCapacity()); }

    private List<String> buildVirtualStackTooltip(final int relX, final int relY) {
        final int index = getVirtualStackIndex(relX, relY);
        if (index < 0) {
            return Collections.emptyList();
        }
        final GenericStack stack = this.container.stackList.stacks().get(index);
        if (stack == null) {
            return Collections.emptyList();
        }
        final List<String> tooltip = new ObjectArrayList<>();
        tooltip.add(stack.what().getDisplayName().getFormattedText());
        tooltip.add(TextFormatting.GRAY + stack.what().formatAmount(stack.amount(), AmountFormat.FULL));
        return tooltip;
    }

    private int getVirtualStackIndex(final int relX, final int relY) {
        if (!isInVirtualList(relX, relY)) {
            return -1;
        }
        final int column = (relX - LIST_X) / SLOT_SIZE;
        final int row = (relY - LIST_Y) / SLOT_SIZE;
        if (column < 0 || column >= SLOT_COLUMNS || row < 0 || row >= VISIBLE_ROWS) {
            return -1;
        }
        final int index = this.scrollRow * SLOT_COLUMNS + row * SLOT_COLUMNS + column;
        if (index < 0 || index >= this.container.stackList.stacks().size()) {
            return -1;
        }
        return index;
    }

    private boolean isInVirtualList(final int relX, final int relY) {
        return relX >= LIST_X && relX < LIST_X + LIST_WIDTH && relY >= LIST_Y && relY < LIST_Y + LIST_HEIGHT;
    }

    private int fluidSlotAt(int relY) { return (relY - TANK_Y) / 18; }

    public MEPatternProvider getOwner() { return container.getOwner(); }

    private boolean isInSubFluidTank(final int relX, final int relY) {
        return relX >= TANK_X && relX < TANK_X + TANK_SIZE && relY >= TANK_Y && relY < TANK_Y + container.subFluid.size() * 18 && (relY - TANK_Y) % 18 < TANK_SIZE;
    }

    private boolean isInScrollbar(final int relX, final int relY) {
        return relX >= SCROLL_X && relX < SCROLL_X + SCROLL_WIDTH && relY >= SCROLL_Y && relY < SCROLL_Y + SCROLL_HEIGHT;
    }

    private void scrollBy(final int delta) {
        this.scrollRow = MathHelper.clamp(this.scrollRow + delta, 0, getScrollableRows());
    }

    private int getScrollableRows() {
        final int totalRows = (int) Math.ceil(this.container.stackList.stacks().size() / (double) SLOT_COLUMNS);
        return Math.max(0, totalRows - VISIBLE_ROWS);
    }

    private int getScrollbarKnobY() {
        final int scrollableRows = getScrollableRows();
        if (scrollableRows <= 0) {
            return SCROLL_Y;
        }
        final int track = SCROLL_HEIGHT - SCROLL_KNOB_HEIGHT;
        return SCROLL_Y + Math.round(track * (this.scrollRow / (float) scrollableRows));
    }

    private void updateScrollFromMouse(final int relY) {
        final int scrollableRows = getScrollableRows();
        if (scrollableRows <= 0) {
            this.scrollRow = 0;
            return;
        }
        final int track = SCROLL_HEIGHT - SCROLL_KNOB_HEIGHT;
        final float progress = MathHelper.clamp((relY - SCROLL_Y - (SCROLL_KNOB_HEIGHT / 2.0F)) / Math.max(1.0F, track), 0.0F, 1.0F);
        this.scrollRow = Math.round(progress * scrollableRows);
    }
}
