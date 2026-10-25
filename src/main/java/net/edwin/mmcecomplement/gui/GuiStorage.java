package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import net.minecraft.util.ResourceLocation;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge;
import net.edwin.mmcecomplement.variant.StorageVolumeFormatter;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Arrays;

/** Variable-height storage screen using the restrained vanilla container style. */
public class GuiStorage extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "mmce_complement", "textures/gui/storage_container.png");
    /** The same 18x18 recessed slot sprite used by vanilla containers. */
    private static final ResourceLocation VANILLA_SLOTS = new ResourceLocation(
        "mmce_complement", "textures/gui/vanilla_slot.png");
    private static final int SCROLL_X = 190;
    private static final int TRACK_TOP = ContainerStorage.STORAGE_TOP;
    private static final int SORT_X = 181;
    private static final int SORT_Y = 5;
    private static final int SORT_WIDTH = 36;
    private static final int SORT_HEIGHT = 14;
    private static final int TAB_X = 181;
    private static final int TAB_WIDTH = 36;
    private static final int TAB_HEIGHT = 18;
    private static final int TAB_SPACING = 22;
    private final ContainerStorage container;
    private final StorageController storage;
    private boolean dragging;
    private boolean handledMouseClick;

    /** Resolve the same machine object used by the original MMCE controller GUI. */
    private hellfirepvp.modularmachinery.common.machine.DynamicMachine getTitleMachine() {
        if (!(storage instanceof TileMachineController)) return null;
        TileMachineController controller = (TileMachineController) storage;
        hellfirepvp.modularmachinery.common.machine.DynamicMachine machine =
            controller.getFoundMachine();
        if (machine == null) machine = controller.getParentMachine();
        if (machine == null && storage instanceof net.minecraft.tileentity.TileEntity) {
            net.minecraft.block.Block block = ((net.minecraft.tileentity.TileEntity) storage)
                .getBlockType();
            if (block instanceof hellfirepvp.modularmachinery.common.block.BlockController) {
                machine = ((hellfirepvp.modularmachinery.common.block.BlockController) block)
                    .getParentMachine();
            }
        }
        return machine;
    }

    private String getMachineTitle() {
        hellfirepvp.modularmachinery.common.machine.DynamicMachine machine = getTitleMachine();
        if (machine != null && machine.getLocalizedName() != null
            && !machine.getLocalizedName().isEmpty()) {
            return machine.getLocalizedName();
        }
        if (storage instanceof TileMachineController) {
            String formed = ((TileMachineController) storage).getFormedMachineName();
            if (formed != null && !formed.isEmpty()) return formed;
        }
        return I18n.format("gui.mmce_complement.storage.title");
    }


    public GuiStorage(EntityPlayer player, StorageController storage) {
        super(new ContainerStorage(storage, player));
        this.container = (ContainerStorage) inventorySlots;
        this.storage = storage;
        xSize = 222;
        ySize = container.getHotbarTop() + 30;
    }

    @Override
    public void initGui() {
        container.refreshLayout();
        ySize = container.getHotbarTop() + 30;
        super.initGui();
    }

    private void refreshLayout() {
        container.refreshLayout();
        ySize = container.getHotbarTop() + 30;
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
    }

    @Override
    public void updateScreen() {
        refreshLayout();
        super.updateScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // GuiContainer captures guiTop before drawing the background. Update
        // the origin first so items, text, background and hitboxes agree.
        refreshLayout();
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        int tank = container.getTankAt(mouseX - guiLeft, mouseY - guiTop);
        if (tank >= 0) {
            TankDisplay display = tankDisplay(tank);
            drawHoveringText(Arrays.asList(display.name == null
                ? I18n.format("gui.mmce_complement.storage.empty") : display.name,
                StorageVolumeFormatter.formatWithUnit(display.amount) + " / "
                    + StorageVolumeFormatter.formatWithUnit(container.getTankCapacity())), mouseX, mouseY);
        } else {
            renderHoveredToolTip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                    int mouseX, int mouseY) {
        int rows = container.getVisibleRows();
        int storageBottom = ContainerStorage.STORAGE_TOP + rows * 18;
        int playerTop = container.getPlayerInventoryTop();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.color(1F, 1F, 1F, 1F);
        // The texture contains only the panel and border; slot recesses are
        // drawn below at the exact coordinates used by ContainerStorage.
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        // Draw at native pixel scale. The 256px canvas is intentionally larger
        // than the tallest screen, so dynamic row counts do not stretch the
        // border or shift the slot grid.
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        panel(guiLeft + 5, guiTop + ContainerStorage.STORAGE_TOP - 4,
            guiLeft + 178, guiTop + storageBottom + 3, 0xFFC6C6C6);
        panel(guiLeft + 5, guiTop + playerTop - 4,
            guiLeft + 178, guiTop + container.getHotbarTop() + 18 + 3,
            0xFFC6C6C6);

        Minecraft.getMinecraft().getTextureManager().bindTexture(VANILLA_SLOTS);
        for (int i = 0; i < container.getVisibleSlotCount(); i++) {
            int x = ContainerStorage.SLOT_X
                + (i % ContainerStorage.COLUMNS) * 18;
            int y = ContainerStorage.STORAGE_TOP
                + (i / ContainerStorage.COLUMNS) * 18;
            vanillaSlot(guiLeft + x, guiTop + y);
        }
        // Draw every recess through the same path, before any icons or text.
        for (int i = 0; i < container.getVisibleTankCount(); i++) {
            vanillaSlot(guiLeft + ContainerStorage.SLOT_X + i % ContainerStorage.COLUMNS * 18,
                guiTop + ContainerStorage.STORAGE_TOP + i / ContainerStorage.COLUMNS * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < ContainerStorage.COLUMNS; col++) {
                vanillaSlot(guiLeft + ContainerStorage.SLOT_X + col * 18,
                    guiTop + playerTop + row * 18);
            }
        }
        for (int col = 0; col < ContainerStorage.COLUMNS; col++) {
            vanillaSlot(guiLeft + ContainerStorage.SLOT_X + col * 18,
                guiTop + container.getHotbarTop());
        }
        drawScrollbar(mouseX, mouseY);
        drawSortButton(mouseX, mouseY);
        if (container.getMode() != ContainerStorage.MODE_ITEMS) drawTankIcons();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private TankDisplay tankDisplay(int index) {
        if (container.getMode() == ContainerStorage.MODE_FLUIDS) {
            IFluidHandler handler = container.getFluidTank(index);
            IFluidTankProperties[] properties = handler == null ? new IFluidTankProperties[0] : handler.getTankProperties();
            FluidStack stack = properties.length == 0 ? null : properties[0].getContents();
            return stack == null ? TankDisplay.EMPTY : new TankDisplay(stack.amount,
                stack.getFluid().getStill(stack), stack.getFluid().getColor(stack), stack.getLocalizedName());
        }
        Object gas = container.getGasStorage();
        return gas == null ? TankDisplay.EMPTY : new TankDisplay(StorageGasBridge.amount(gas, index),
            StorageGasBridge.texture(gas, index), StorageGasBridge.tint(gas, index), StorageGasBridge.name(gas, index));
    }

    /** Text is deliberately kept out of the background's texture pass. */
    private void drawTankIcons() {
        for (int i = 0; i < container.getVisibleTankCount(); i++) {
            TankDisplay display = tankDisplay(container.getPage() * ContainerStorage.COLUMNS + i);
            if (display.amount <= 0) continue;
            TextureAtlasSprite sprite = display.texture == null ? mc.getTextureMapBlocks().getMissingSprite()
                : mc.getTextureMapBlocks().getAtlasSprite(display.texture.toString());
            GlStateManager.enableTexture2D();
            GlStateManager.color((display.tint >> 16 & 255) / 255F,
                (display.tint >> 8 & 255) / 255F, (display.tint & 255) / 255F, 1F);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            drawTexturedModalRect(guiLeft + ContainerStorage.SLOT_X + i % ContainerStorage.COLUMNS * 18,
                guiTop + ContainerStorage.STORAGE_TOP + i / ContainerStorage.COLUMNS * 18, sprite, 16, 16);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawTankAmounts() {
        for (int i = 0; i < container.getVisibleTankCount(); i++) {
            TankDisplay display = tankDisplay(container.getPage() * ContainerStorage.COLUMNS + i);
            if (display.amount <= 0) continue;
            String text = StorageVolumeFormatter.format(display.amount);
            int x = ContainerStorage.SLOT_X + i % ContainerStorage.COLUMNS * 18;
            int y = ContainerStorage.STORAGE_TOP + i / ContainerStorage.COLUMNS * 18;
            // Keep the native count font and baseline, with a 3px inset
            // from RenderItem's right-aligned count position.
            fontRenderer.drawStringWithShadow(text,
                x + 14 - fontRenderer.getStringWidth(text), y + 9, 0xFFFFFF);
        }
    }

    private static final class TankDisplay {
        private static final TankDisplay EMPTY = new TankDisplay(0, null, 0xFFFFFF, null);
        private final int amount, tint;
        private final ResourceLocation texture;
        private final String name;
        private TankDisplay(int amount, ResourceLocation texture, int tint, String name) {
            this.amount = amount; this.texture = texture; this.tint = tint; this.name = name;
        }
    }

    private void panel(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, bottom, color);
        drawRect(left, top, right, top + 1, 0xFF8B8B8B);
        drawRect(left, top + 1, left + 1, bottom, 0xFF8B8B8B);
        drawRect(right - 1, top + 1, right, bottom, 0xFF373737);
        drawRect(left, bottom - 1, right, bottom, 0xFF373737);
    }

    private void vanillaSlot(int x, int y) {
        GlStateManager.enableTexture2D();
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(VANILLA_SLOTS);
        // Slot.xPos/slot.yPos are the item origin; vanilla draws the 18px
        // recess one pixel above and to the left of that origin.
        // vanilla_slot.png is a standalone 18x18 texture rather than a
        // 256x256 atlas, so use its real dimensions for UV scaling.
        drawModalRectWithCustomSizedTexture(x - 1, y - 1, 0, 0,
            18, 18, 18, 18);
    }

    private void drawSortButton(int mouseX, int mouseY) {
        int left = guiLeft + SORT_X;
        int top = guiTop + SORT_Y;
        boolean hover = mouseX >= left && mouseX < left + SORT_WIDTH
            && mouseY >= top && mouseY < top + SORT_HEIGHT;
        int fill = hover ? 0xFFC6C6C6 : 0xFF9E9E9E;
        drawRect(left, top, left + SORT_WIDTH, top + SORT_HEIGHT, 0xFF373737);
        drawRect(left + 1, top + 1, left + SORT_WIDTH - 1,
            top + SORT_HEIGHT - 1, fill);
        drawRect(left + 1, top + 1, left + SORT_WIDTH - 1, top + 2,
            0xFFE0E0E0);
        drawRect(left + 1, top + SORT_HEIGHT - 2, left + SORT_WIDTH - 1,
            top + SORT_HEIGHT - 1, 0xFF666666);
    }

    private void drawScrollbar(int mouseX, int mouseY) {
        int trackHeight = container.getVisibleRows() * 18;
        int x = guiLeft + SCROLL_X;
        int top = guiTop + TRACK_TOP;
        // Recessed vanilla-style rail: one dark edge, one light edge and a
        // muted centre. It blends into the panel instead of looking like a
        // separate coloured widget.
        drawRect(x, top, x + 10, top + trackHeight, 0xFF373737);
        drawRect(x + 1, top + 1, x + 9, top + trackHeight - 1,
            0xFF8B8B8B);
        drawRect(x + 2, top + 2, x + 8, top + trackHeight - 2,
            0xFF555555);
        if (container.getPageCount() <= 1) {
            drawRect(x + 2, top + 2, x + 8, top + trackHeight - 2,
                0xFF777777);
            return;
        }
        int totalCount = container.getMode() == ContainerStorage.MODE_ITEMS
            ? container.getTotalSlots() : container.getTankCount();
        int totalRows = Math.max(container.getVisibleRows(),
            (totalCount + ContainerStorage.COLUMNS - 1) / ContainerStorage.COLUMNS);
        int handle = Math.max(10, trackHeight * container.getVisibleRows()
            / Math.max(1, totalRows));
        handle = Math.min(trackHeight, handle);
        int travel = trackHeight - handle;
        int handleTop = top + travel * container.getPage()
            / (container.getPageCount() - 1);
        boolean hover = mouseX >= x && mouseX < x + 10
            && mouseY >= handleTop + guiTop - guiTop
            && mouseY < handleTop + handle + guiTop - guiTop;
        int handleColor = hover || dragging ? 0xFFBDBDBD : 0xFF9E9E9E;
        drawRect(x + 1, handleTop, x + 9, handleTop + handle,
            0xFF373737);
        drawRect(x + 2, handleTop + 1, x + 8, handleTop + handle - 1,
            handleColor);
        drawRect(x + 2, handleTop + 1, x + 8, handleTop + 2,
            0xFFE0E0E0);
        drawRect(x + 2, handleTop + handle - 2, x + 8,
            handleTop + handle - 1, 0xFF666666);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawTankAmounts();
        drawTabs(mouseX, mouseY);
        fontRenderer.drawString(getMachineTitle(), 8, 6, 0x404040);
        String sortText = I18n.format("gui.mmce_complement.storage.sort");
        fontRenderer.drawString(sortText,
            SORT_X + (SORT_WIDTH - fontRenderer.getStringWidth(sortText)) / 2,
            SORT_Y + 3, 0x404040);
        boolean full = storage instanceof TileMachineController
            && net.edwin.mmcecomplement.tile.TileStorageController.FULL_MESSAGE.equals(
                ((TileMachineController) storage).getControllerStatus().getUnlocMessage());
        String capacity = container.getMode() == ContainerStorage.MODE_ITEMS
            ? I18n.format("gui.mmce_complement.storage.capacity", container.getTotalSlots())
            : I18n.format(container.getMode() == ContainerStorage.MODE_FLUIDS
                ? "gui.mmce_complement.storage.fluid_capacity"
                : "gui.mmce_complement.storage.gas_capacity",
                container.getTankCount(), StorageVolumeFormatter.formatWithUnit(container.getTankCapacity()));
        fontRenderer.drawString(full && container.getMode() == ContainerStorage.MODE_ITEMS
            ? I18n.format(net.edwin.mmcecomplement.tile.TileStorageController.FULL_MESSAGE) : capacity,
            8, 16, full ? 0xAA3333 : 0x404040);
    }

    private void drawTabs(int mouseX,int mouseY) {
        int y = container.getPlayerInventoryTop();
        String[] labels={I18n.format("gui.mmce_complement.storage.items"),I18n.format("gui.mmce_complement.storage.fluids"),I18n.format("gui.mmce_complement.storage.gases")};
        for(int i=0;i<3;i++){
            if(!container.isModeAvailable(i)) continue;
            int color=container.getMode()==i?0xFFE0E0E0:0xFF909090;
            drawRect(TAB_X,y,TAB_X+TAB_WIDTH,y+TAB_HEIGHT,0xFF373737);
            drawRect(TAB_X+1,y+1,TAB_X+TAB_WIDTH-1,y+TAB_HEIGHT-1,color);
            fontRenderer.drawString(labels[i],
                TAB_X+(TAB_WIDTH-fontRenderer.getStringWidth(labels[i]))/2,
                y+(TAB_HEIGHT-fontRenderer.FONT_HEIGHT)/2,0xFF404040);
            y += TAB_SPACING;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        refreshLayout();
        handledMouseClick = true;
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;
        if (mouseButton == 0 && localX >= TAB_X && localX < TAB_X + TAB_WIDTH) {
            int y = container.getPlayerInventoryTop();
            for (int i = 0; i < 3; i++) {
                if (!container.isModeAvailable(i)) continue;
                if (localY >= y && localY < y + TAB_HEIGHT) {
                    dragging = false;
                    container.setMode(i);
                    refreshLayout();
                    NetworkHandlerMMCE.CHANNEL.sendToServer(
                        new NetworkHandlerMMCE.StorageTabMessage(i));
                    return;
                }
                y += TAB_SPACING;
            }
        }
        int tank = container.getTankAt(localX, localY);
        if ((mouseButton == 0 || mouseButton == 1) && tank >= 0) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(new NetworkHandlerMMCE.StorageTankMessage(tank));
            return;
        }
        if (mouseButton == 0 && localX >= SORT_X
            && localX < SORT_X + SORT_WIDTH && localY >= SORT_Y
            && localY < SORT_Y + SORT_HEIGHT) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.StorageSortMessage());
            return;
        }
        // Inventory Sorter uses the middle mouse button as its sort action.
        // Keep the same gesture for the storage variant and let the server
        // perform the actual reorder.
        if (mouseButton == 2 && (isStorageSlot(localX, localY) || tank >= 0)) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.StorageSortMessage());
            return;
        }
        int trackHeight = container.getVisibleRows() * 18;
        if (mouseButton == 0 && localX >= SCROLL_X && localX < SCROLL_X + 10
            && localY >= TRACK_TOP && localY < TRACK_TOP + trackHeight) {
            dragging = true;
            updatePageFromMouse(localY, trackHeight);
            return;
        }
        handledMouseClick = false;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isStorageSlot(int localX, int localY) {
        int minX = ContainerStorage.SLOT_X - 1;
        int maxX = minX + ContainerStorage.COLUMNS * 18;
        int minY = ContainerStorage.STORAGE_TOP - 1;
        int maxY = minY + container.getVisibleRows() * 18;
        if (localX < minX || localX >= maxX || localY < minY
            || localY >= maxY) return false;
        int col = (localX - minX) / 18;
        int row = (localY - minY) / 18;
        int localIndex = row * ContainerStorage.COLUMNS + col;
        return localIndex >= 0
            && localIndex < container.getVisibleSlotCount();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        if (dragging) {
            updatePageFromMouse(mouseY - guiTop,
                container.getVisibleRows() * 18);
            return;
        }
        if (handledMouseClick) return;
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton,
            timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        // A tab can move the player inventory underneath the cursor. Its
        // release must not become a second vanilla inventory click.
        if (handledMouseClick) {
            handledMouseClick = false;
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && container.getPageCount() > 1) {
            sendPage(container.getPage() + (wheel < 0 ? 1 : -1));
        }
    }

    private void updatePageFromMouse(int localY, int trackHeight) {
        if (container.getPageCount() <= 1) return;
        int totalCount = container.getMode() == ContainerStorage.MODE_ITEMS
            ? container.getTotalSlots() : container.getTankCount();
        int totalRows = Math.max(container.getVisibleRows(),
            (totalCount + ContainerStorage.COLUMNS - 1) / ContainerStorage.COLUMNS);
        int handle = Math.max(10, trackHeight * container.getVisibleRows()
            / Math.max(1, totalRows));
        handle = Math.min(trackHeight, handle);
        int travel = Math.max(1, trackHeight - handle);
        int offset = Math.max(0, Math.min(travel, localY - TRACK_TOP - handle / 2));
        sendPage(Math.round(offset * (container.getPageCount() - 1)
            / (float) travel));
    }

    private void sendPage(int page) {
        int clamped = Math.max(0, Math.min(page, container.getPageCount() - 1));
        if (clamped == container.getPage()) return;
        container.setPage(clamped);
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.StoragePageMessage(clamped));
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

