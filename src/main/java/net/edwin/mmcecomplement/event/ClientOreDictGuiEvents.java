package net.edwin.mmcecomplement.event;

import appeng.container.slot.SlotFake;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
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

/** Draws the ore-dict controls directly beside MMCE's item-bus GUI. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientOreDictGuiEvents {
    private static final String MMCE_INPUT_GUI =
            "github.kasuminova.mmce.client.gui.GuiMEItemInputBus";
    private static final int ACTIVE_BUTTON_ID = 0x4D44;
    private static final Map<GuiScreen, PanelState> PANELS = new WeakHashMap<>();

    private ClientOreDictGuiEvents() {}

    @SubscribeEvent
    public static void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        TileMEOreDictInputBus tile = getTile(event.getGui());
        if (tile == null) {
            return;
        }
        GuiScreen gui = event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        int guiLeft = (gui.width - 176) / 2;
        int guiTop = (gui.height - 204) / 2;
        int left = guiLeft - 164;

        GuiTextField whitelist = new GuiTextField(0, mc.fontRenderer,
                left, guiTop + 42, 156, 18);
        whitelist.setMaxStringLength(256);
        whitelist.setText(tile.getWhitelist());
        GuiTextField blacklist = new GuiTextField(1, mc.fontRenderer,
                left, guiTop + 81, 156, 18);
        blacklist.setMaxStringLength(256);
        blacklist.setText(tile.getBlacklist());
        GuiButton active = new GuiButton(ACTIVE_BUTTON_ID, left,
                guiTop + 111, 156, 20, activeText(tile.isActivePull()));
        event.getButtonList().add(active);
        PANELS.put(gui, new PanelState(tile, whitelist, blacklist, active));
        Keyboard.enableRepeatEvents(true);
    }

    @SubscribeEvent
    public static void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        PanelState state = PANELS.get(event.getGui());
        if (state == null) {
            return;
        }
        state.whitelist.updateCursorCounter();
        state.blacklist.updateCursorCounter();
        if (!state.whitelist.isFocused()
                && !state.whitelist.getText().equals(state.tile.getWhitelist())) {
            state.whitelist.setText(state.tile.getWhitelist());
        }
        if (!state.blacklist.isFocused()
                && !state.blacklist.getText().equals(state.tile.getBlacklist())) {
            state.blacklist.setText(state.tile.getBlacklist());
        }
        state.active.displayString = activeText(state.tile.isActivePull());

        Minecraft mc = Minecraft.getMinecraft();
        int left = state.whitelist.x;
        int top = state.whitelist.y - 42;
        mc.fontRenderer.drawStringWithShadow(I18n.format(
                "gui.mmce_complement.me_ore_dict.title"), left, top + 8,
                0xFFFFFF);
        mc.fontRenderer.drawStringWithShadow(I18n.format(
                "gui.mmce_complement.me_ore_dict.whitelist"), left,
                top + 30, 0xFFFFFF);
        mc.fontRenderer.drawStringWithShadow(I18n.format(
                "gui.mmce_complement.me_ore_dict.blacklist"), left,
                top + 69, 0xFFFFFF);
        state.whitelist.drawTextBox();
        state.blacklist.drawTextBox();
    }

    @SubscribeEvent
    public static void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        PanelState state = PANELS.get(event.getGui());
        if (state == null || Mouse.getEventButton() < 0
                || !Mouse.getEventButtonState()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * event.getGui().width / mc.displayWidth;
        int mouseY = event.getGui().height
                - Mouse.getEventY() * event.getGui().height / mc.displayHeight - 1;
        int button = Mouse.getEventButton();
        boolean overField = isInside(state.whitelist, mouseX, mouseY)
                || isInside(state.blacklist, mouseX, mouseY);
        state.whitelist.mouseClicked(mouseX, mouseY, button);
        state.blacklist.mouseClicked(mouseX, mouseY, button);
        if (overField) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        PanelState state = PANELS.get(event.getGui());
        if (state == null || !Keyboard.getEventKeyState()) {
            return;
        }
        char typed = Keyboard.getEventCharacter();
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_TAB
                && (state.whitelist.isFocused() || state.blacklist.isFocused())) {
            boolean first = state.whitelist.isFocused();
            state.whitelist.setFocused(!first);
            state.blacklist.setFocused(first);
            event.setCanceled(true);
            return;
        }
        if (state.whitelist.isFocused()
                && state.whitelist.textboxKeyTyped(typed, key)) {
            sendString(state.tile, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_WHITELIST,
                    state.whitelist.getText());
            event.setCanceled(true);
        } else if (state.blacklist.isFocused()
                && state.blacklist.textboxKeyTyped(typed, key)) {
            sendString(state.tile, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_BLACKLIST,
                    state.blacklist.getText());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton().id != ACTIVE_BUTTON_ID) {
            return;
        }
        PanelState state = PANELS.get(event.getGui());
        if (state == null) {
            return;
        }
        event.getButton().playPressSound(
                Minecraft.getMinecraft().getSoundHandler());
        boolean enabled = !state.tile.isActivePull();
        // Update the open screen immediately. Waiting for the block-entity NBT
        // packet is insufficient here because MMCE replaces configInventory
        // during deserialization while the already-open SlotFake instances
        // still reference the previous inventory object.
        state.tile.setClientActivePull(enabled);
        if (!enabled) clearDisplayedConfiguration(event.getGui());
        state.active.displayString = activeText(enabled);
        sendBoolean(state.tile, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_ACTIVE,
                enabled);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onGuiChanged(GuiOpenEvent event) {
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        PanelState state = PANELS.remove(current);
        if (state == null) {
            return;
        }
        sendString(state.tile, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_WHITELIST,
                state.whitelist.getText());
        sendString(state.tile, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_BLACKLIST,
                state.blacklist.getText());
        Keyboard.enableRepeatEvents(false);
    }

    private static String activeText(boolean active) {
        return I18n.format("gui.mmce_complement.me_ore_dict.active")
                + ": " + I18n.format(active
                ? "gui.mmce_complement.me_ore_dict.on"
                : "gui.mmce_complement.me_ore_dict.off");
    }

    private static boolean isInside(GuiTextField field, int x, int y) {
        return x >= field.x && x < field.x + field.width
                && y >= field.y && y < field.y + field.height;
    }

    private static void sendString(TileMEOreDictInputBus tile,
                                   int field, String value) {
        if (field == NetworkHandlerMMCE.FIELD_ME_ORE_DICT_WHITELIST) {
            tile.setClientWhitelist(value);
        } else if (field == NetworkHandlerMMCE.FIELD_ME_ORE_DICT_BLACKLIST) {
            tile.setClientBlacklist(value);
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("v", value == null ? "" : value);
        send(tile, field, tag);
    }

    private static void sendBoolean(TileMEOreDictInputBus tile,
                                    int field, boolean value) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("v", value);
        send(tile, field, tag);
    }

    private static void send(TileMEOreDictInputBus tile,
                             int field, NBTTagCompound value) {
        if (tile.getWorld() != null) {
            NetworkHandlerMMCE.CHANNEL.sendToServer(
                    new NetworkHandlerMMCE.SetHatchFieldMessage(tile.getPos(),
                            tile.getWorld().provider.getDimension(), field, value));
        }
    }

    private static TileMEOreDictInputBus getTile(GuiScreen gui) {
        if (gui == null || !MMCE_INPUT_GUI.equals(gui.getClass().getName())
                || !(gui instanceof GuiContainer)) {
            return null;
        }
        try {
            java.lang.reflect.Field field = ReflectionHelper.findField(
                    GuiContainer.class, "inventorySlots", "field_147002_h");
            field.setAccessible(true);
            Container container = (Container) field.get(gui);
            Method getOwner = container.getClass().getMethod("getOwner");
            Object owner = getOwner.invoke(container);
            return owner instanceof TileMEOreDictInputBus
                    ? (TileMEOreDictInputBus) owner : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void clearDisplayedConfiguration(GuiScreen gui) {
        if (!(gui instanceof GuiContainer)) return;
        try {
            java.lang.reflect.Field field = ReflectionHelper.findField(
                    GuiContainer.class, "inventorySlots", "field_147002_h");
            field.setAccessible(true);
            Container container = (Container) field.get(gui);
            for (Slot slot : container.inventorySlots) {
                if (slot instanceof SlotFake) {
                    slot.putStack(net.minecraft.item.ItemStack.EMPTY);
                }
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    private static final class PanelState {
        private final TileMEOreDictInputBus tile;
        private final GuiTextField whitelist;
        private final GuiTextField blacklist;
        private final GuiButton active;

        private PanelState(TileMEOreDictInputBus tile,
                           GuiTextField whitelist, GuiTextField blacklist,
                           GuiButton active) {
            this.tile = tile;
            this.whitelist = whitelist;
            this.blacklist = blacklist;
            this.active = active;
        }
    }
}
