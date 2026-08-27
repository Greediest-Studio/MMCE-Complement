package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** Adds the active-pull/standby switch to MMCE's three input-bus GUIs. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientInventoryInputGuiEvents {
    private static final String ITEM_GUI =
        "github.kasuminova.mmce.client.gui.GuiMEItemInputBus";
    private static final String FLUID_GUI =
        "github.kasuminova.mmce.client.gui.GuiMEFluidInputBus";
    private static final String GAS_GUI =
        "github.kasuminova.mmce.client.gui.GuiMEGasInputBus";
    private static final String MIXED_GUI =
        "net.edwin.mmcecomplement.compat.ae.gui.GuiMEInputAssembly";
    private static final int ACTIVE_BUTTON_ID = 0x4D49;
    private static final int PANEL_WIDTH = 156;
    private static final int PANEL_GAP = 8;
    private static final Map<GuiScreen, PanelState> PANELS =
        new WeakHashMap<>();

    private ClientInventoryInputGuiEvents() { }

    @SubscribeEvent
    public static void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        PanelState state = findState(event.getGui());
        if (state == null) return;
        String guiName = event.getGui().getClass().getName();
        int guiHeight = ITEM_GUI.equals(guiName) || MIXED_GUI.equals(guiName)
            ? 204 : 231;
        int guiLeft = getGuiCoordinate(event.getGui(), "guiLeft",
            "field_147003_i", (event.getGui().width - 176) / 2);
        int guiTop = getGuiCoordinate(event.getGui(), "guiTop",
            "field_147009_r", (event.getGui().height - guiHeight) / 2);
        int left = guiLeft - PANEL_GAP - PANEL_WIDTH;
        // Match the ore-dictionary bus control exactly: same vanilla widget,
        // left panel offset, vertical offset, width and height.
        state.button = new GuiButton(ACTIVE_BUTTON_ID, left,
            guiTop + 111, PANEL_WIDTH, 20,
            activeText(state.bus.isActivePull()));
        // GuiTextField draws its border one pixel outside x/width. Inset it
        // so that the visible border aligns with the button above.
        state.reserve = new GuiTextField(0x4D4A,
            Minecraft.getMinecraft().fontRenderer, left + 1, guiTop + 153,
            PANEL_WIDTH - 2, 18);
        state.reserve.setMaxStringLength(19);
        state.reserve.setValidator(
            ClientInventoryInputGuiEvents::isValidReserveText);
        state.reserve.setText(Long.toString(
            state.bus.getPermanentReserve()));
        state.milliBuckets = !ITEM_GUI.equals(guiName)
            && !MIXED_GUI.equals(guiName);
        event.getButtonList().add(state.button);
        PANELS.put(event.getGui(), state);
        Keyboard.enableRepeatEvents(true);
    }

    @SubscribeEvent
    public static void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        PanelState state = PANELS.get(event.getGui());
        if (state == null || state.button == null || state.reserve == null) return;
        state.button.displayString = activeText(state.bus.isActivePull());
        state.reserve.updateCursorCounter();
        if (!state.reserve.isFocused()) {
            String actual = Long.toString(state.bus.getPermanentReserve());
            if (!actual.equals(state.reserve.getText())) {
                state.reserve.setText(actual);
            }
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawStringWithShadow(I18n.format(state.milliBuckets
            ? "gui.mmce_complement.me_inventory.reserve_mb"
            : "gui.mmce_complement.me_inventory.reserve"),
            state.reserve.x, state.reserve.y - 13, 0xFFFFFF);
        state.reserve.drawTextBox();
    }

    @SubscribeEvent
    public static void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        PanelState state = PANELS.get(event.getGui());
        if (state == null || state.reserve == null
            || Mouse.getEventButton() < 0 || !Mouse.getEventButtonState()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * event.getGui().width
            / mc.displayWidth;
        int mouseY = event.getGui().height
            - Mouse.getEventY() * event.getGui().height
            / mc.displayHeight - 1;
        boolean overField = isInside(state.reserve, mouseX, mouseY);
        state.reserve.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
        if (overField) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKeyboard(
        GuiScreenEvent.KeyboardInputEvent.Pre event) {
        PanelState state = PANELS.get(event.getGui());
        if (state == null || state.reserve == null
            || !state.reserve.isFocused() || !Keyboard.getEventKeyState()) {
            return;
        }
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_TAB || key == Keyboard.KEY_RETURN
            || key == Keyboard.KEY_NUMPADENTER) {
            state.reserve.setFocused(false);
            normalizeAndSendReserve(state);
            event.setCanceled(true);
            return;
        }
        if (state.reserve.textboxKeyTyped(Keyboard.getEventCharacter(), key)) {
            sendReserve(state, parseReserve(state.reserve.getText()));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAction(
        GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton().id != ACTIVE_BUTTON_ID) return;
        PanelState state = PANELS.get(event.getGui());
        if (state == null) return;
        event.getButton().playPressSound(
            Minecraft.getMinecraft().getSoundHandler());
        boolean enabled = !state.bus.isActivePull();
        state.bus.setClientActivePull(enabled);
        state.button.displayString = activeText(enabled);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("v", enabled);
        TileEntity tile = state.tile;
        if (tile.getWorld() != null) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.SetHatchFieldMessage(tile.getPos(),
                    tile.getWorld().provider.getDimension(),
                    NetworkHandlerMMCE.FIELD_ME_INVENTORY_ACTIVE, tag));
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onGuiChanged(GuiOpenEvent event) {
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        PanelState state = PANELS.remove(current);
        if (state == null) return;
        normalizeAndSendReserve(state);
        Keyboard.enableRepeatEvents(false);
    }

    private static String activeText(boolean active) {
        return I18n.format("gui.mmce_complement.me_inventory.active")
            + ": " + I18n.format(active
            ? "gui.mmce_complement.me_inventory.on"
            : "gui.mmce_complement.me_inventory.standby");
    }

    private static boolean isInside(GuiTextField field, int x, int y) {
        return x >= field.x && x < field.x + field.width
            && y >= field.y && y < field.y + field.height;
    }

    private static int getGuiCoordinate(GuiScreen gui, String name,
                                        String mappedName, int fallback) {
        try {
            java.lang.reflect.Field field = ReflectionHelper.findField(
                GuiContainer.class, name, mappedName);
            field.setAccessible(true);
            return field.getInt(gui);
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static boolean isValidReserveText(String value) {
        if (value == null || value.isEmpty()) return true;
        try {
            return Long.parseLong(value) >= 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static long parseReserve(String value) {
        if (value == null || value.isEmpty()) return 0L;
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static void normalizeAndSendReserve(PanelState state) {
        long value = parseReserve(state.reserve.getText());
        state.reserve.setText(Long.toString(value));
        sendReserve(state, value);
    }

    private static void sendReserve(PanelState state, long value) {
        state.bus.setClientPermanentReserve(value);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("v", value);
        if (state.tile.getWorld() != null) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                new NetworkHandlerMMCE.SetHatchFieldMessage(
                    state.tile.getPos(),
                    state.tile.getWorld().provider.getDimension(),
                    NetworkHandlerMMCE.FIELD_ME_INVENTORY_RESERVE, tag));
        }
    }

    private static PanelState findState(GuiScreen gui) {
        if (gui == null || !(gui instanceof GuiContainer)) return null;
        String name = gui.getClass().getName();
        if (!ITEM_GUI.equals(name) && !FLUID_GUI.equals(name)
            && !GAS_GUI.equals(name) && !MIXED_GUI.equals(name)) return null;
        try {
            java.lang.reflect.Field field = ReflectionHelper.findField(
                GuiContainer.class, "inventorySlots", "field_147002_h");
            field.setAccessible(true);
            Container container = (Container) field.get(gui);
            Method getOwner = container.getClass().getMethod("getOwner");
            Object owner = getOwner.invoke(container);
            if (owner instanceof MEInventoryInputBus
                && owner instanceof TileEntity) {
                return new PanelState((MEInventoryInputBus) owner,
                    (TileEntity) owner);
            }
        } catch (ReflectiveOperationException ignored) { }
        return null;
    }

    private static final class PanelState {
        private final MEInventoryInputBus bus;
        private final TileEntity tile;
        private GuiButton button;
        private GuiTextField reserve;
        private boolean milliBuckets;

        private PanelState(MEInventoryInputBus bus, TileEntity tile) {
            this.bus = bus;
            this.tile = tile;
        }
    }
}
