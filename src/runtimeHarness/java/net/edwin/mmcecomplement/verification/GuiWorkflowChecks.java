package net.edwin.mmcecomplement.verification;

import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.tile.MEPatternProvider.WorkModeSetting;
import hellfirepvp.modularmachinery.common.CommonProxy.GuiType;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.tile.IMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae2s.AE2SPatternProviderContainer;
import net.edwin.mmcecomplement.compat.ae2s.client.AE2SDataPatternProviderGui;
import net.edwin.mmcecomplement.event.ClientInventoryInputGuiEvents;
import net.edwin.mmcecomplement.event.ClientOreDictGuiEvents;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Actual installed client containers, GUI callbacks and server packet validation. */
final class GuiWorkflowChecks {
    final List<String> checks = new ArrayList<>();
    private int phase, ticks, since, busIndex, dataIndex;
    private volatile boolean serverReady;
    private volatile String serverError;
    private int expectedMode;
    private boolean expectedActive;

    boolean tick(Minecraft client) throws Exception {
        ticks++;
        if (serverError != null) throw new IllegalStateException(serverError);
        if (phase != 4 && ticks - since > 200) throw new IllegalStateException("GUI workflow timed out in phase " + phase);
        GuiScreen gui = client.currentScreen;
        switch (phase) {
            case 0:
                AE2SPatternProviderContainer container = (AE2SPatternProviderContainer) client.player.openContainer;
                expectedMode = (container.workMode + 1) % WorkModeSetting.values().length;
                clickButton(gui, 0);
                next(1); break;
            case 1:
                if (((AE2SPatternProviderContainer) client.player.openContainer).workMode != expectedMode) break;
                serverTask(client, () -> {
                    MEPatternProvider provider = (MEPatternProvider) serverTile(client, RuntimeVerification.PROVIDER);
                    require(provider.getWorkMode().ordinal() == expectedMode, "Work mode GUI action did not reach server");
                    provider.getInfHandler().appendItem(new ItemStack(Items.APPLE, 3));
                    provider.getInfHandler().fill(new FluidStack(FluidRegistry.WATER, 250), true);
                });
                next(2); break;
            case 2:
                if (!serverReady || ((AE2SPatternProviderContainer) client.player.openContainer).stackList.stacks().isEmpty()) break;
                clickButton(gui, 1); next(3); break;
            case 3:
                if (!((AE2SPatternProviderContainer) client.player.openContainer).stackList.stacks().isEmpty()) break;
                checks.add("native-work-mode-button-and-return-cache-action-server-sync");
                next(4); break;
            case 4:
                if (!ProviderModeChecks.completed) break;
                openBus(client); next(5); break;
            case 5:
                if (!matchesBus(gui) || ticks - since < 12) break;
                Object panel = panel(ClientInventoryInputGuiEvents.class, gui);
                GuiTextField reserve = (GuiTextField) field(panel, "reserve");
                require(reserve.x >= 0 && reserve.y >= 0, "Inventory controls are off screen");
                MEInventoryInputBus bus = (MEInventoryInputBus) client.world.getTileEntity(busPosition());
                expectedActive = !bus.isActivePull();
                reserve.setText(busIndex == 0 ? "7" : "125");
                invokePrivate(ClientInventoryInputGuiEvents.class, "normalizeAndSendReserve", panel);
                clickEvent(gui, 0x4D49);
                next(6); break;
            case 6:
                if (ticks - since < 16) break;
                MEInventoryInputBus clientBus = (MEInventoryInputBus) client.world.getTileEntity(busPosition());
                long value = busIndex == 0 ? 7 : 125;
                if (clientBus.isActivePull() != expectedActive || clientBus.getPermanentReserve() != value) break;
                serverTask(client, () -> {
                    MEInventoryInputBus actual = (MEInventoryInputBus) serverTile(client, busPosition());
                    require(actual.isActivePull() == expectedActive && actual.getPermanentReserve() == value,
                        "Inventory native container owner rejected active/reserve packet");
                });
                screenshot(client, busIndex == 0 ? "item-inventory.png" : "fluid-inventory.png");
                next(7); break;
            case 7:
                if (!serverReady) break;
                checks.add(busIndex == 0 ? "native-item-inventory-active-reserve-panel-and-server-packets"
                    : "native-fluid-inventory-active-reserve-panel-and-server-packets");
                if (++busIndex < 2) { openBus(client); next(5); }
                else { open(client, ProviderModeChecks.ORE, "modularmachinery", GuiType.ME_ITEM_INPUT_BUS.ordinal()); next(8); }
                break;
            case 8:
                if (gui == null || !gui.getClass().getName().endsWith(".GuiMEItemInputBus") || ticks - since < 12) break;
                Object orePanel = panel(ClientOreDictGuiEvents.class, gui);
                ((GuiTextField) field(orePanel, "whitelist")).setText("ingot*");
                ((GuiTextField) field(orePanel, "blacklist")).setText("ingotGold");
                ((GuiTextField) field(orePanel, "reserve")).setText("11");
                TileMEOreDictInputBus ore = (TileMEOreDictInputBus) client.world.getTileEntity(ProviderModeChecks.ORE);
                Method send = ClientOreDictGuiEvents.class.getDeclaredMethod("sendString", TileMEOreDictInputBus.class, int.class, String.class);
                send.setAccessible(true);
                send.invoke(null, ore, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_WHITELIST, "ingot*");
                send.invoke(null, ore, NetworkHandlerMMCE.FIELD_ME_ORE_DICT_BLACKLIST, "ingotGold");
                invokePrivate(ClientOreDictGuiEvents.class, "normalizeAndSendReserve", orePanel);
                expectedActive = !ore.isActivePull();
                clickEvent(gui, 0x4D44); next(9); break;
            case 9:
                if (ticks - since < 20) break;
                TileMEOreDictInputBus clientOre = (TileMEOreDictInputBus) client.world.getTileEntity(ProviderModeChecks.ORE);
                if (!"ingot*".equals(clientOre.getWhitelist()) || !"ingotGold".equals(clientOre.getBlacklist())
                    || clientOre.getPermanentReserve() != 11 || clientOre.isActivePull() != expectedActive) break;
                serverTask(client, () -> {
                    TileMEOreDictInputBus actual = (TileMEOreDictInputBus) serverTile(client, ProviderModeChecks.ORE);
                    require("ingot*".equals(actual.getWhitelist()) && "ingotGold".equals(actual.getBlacklist())
                        && actual.getPermanentReserve() == 11 && actual.isActivePull() == expectedActive,
                        "Ore dict native container rejected its configuration packets");
                });
                screenshot(client, "ore-dict.png"); next(10); break;
            case 10:
                if (!serverReady) break;
                checks.add("native-ore-dict-whitelist-blacklist-active-reserve-and-server-packets");
                openData(client); next(11); break;
            case 11:
                if (!(gui instanceof AE2SDataPatternProviderGui) || ticks - since < 15) break;
                int last = dataIndex == 0 ? 35 : 143;
                Slot slot = client.player.openContainer.inventorySlots.get(last);
                if (slot.getStack().isEmpty()) break;
                screenshot(client, dataIndex == 0 ? "data-provider-i.png" : "data-provider-ii.png");
                mouse(gui, coordinate(gui, "guiLeft", "field_147003_i") + slot.xPos + 8,
                    coordinate(gui, "guiTop", "field_147009_r") + slot.yPos + 8, 2);
                require(((Integer) field(gui, "configuredSlot")) == last, "Middle click did not open final-slot data editor");
                ((GuiTextField) field(gui, "valueField")).setText(dataIndex == 0 ? "42.25" : "-18.5");
                key(gui, '\n', Keyboard.KEY_RETURN); next(12); break;
            case 12:
                if (ticks - since < 20) break;
                final int dataSlot = dataIndex == 0 ? 35 : 143;
                final float marker = dataIndex == 0 ? 42.25F : -18.5F;
                final BlockPos dataPos = dataPosition();
                if (ticks - since == 20) serverTask(client, () -> {
                    MEPatternProvider actual = (MEPatternProvider) serverTile(client, dataPos);
                    require(actual instanceof IMEDataPatternProvider, "Expected a data tile at " + dataPos + ", got " + actual.getClass());
                    require(((IMEDataPatternProvider) actual).getPatternValue(dataSlot) == marker,
                        "Data editor server value=" + ((IMEDataPatternProvider) actual).getPatternValue(dataSlot)
                            + ", expected=" + marker + ", cacheEmpty=" + actual.getInfHandler().isEmpty()
                            + ", mode=" + actual.getWorkMode());
                });
                IMEDataPatternProvider data = (IMEDataPatternProvider) client.world.getTileEntity(dataPosition());
                if (data.getPatternValue(dataSlot) != marker) break;
                serverTask(client, () -> {
                    MEPatternProvider actual = (MEPatternProvider) serverTile(client, dataPos);
                    require(((IMEDataPatternProvider) actual).getPatternValue(dataSlot) == marker,
                        "Data editor packet was not accepted by the server");
                    NBTTagCompound nbt = actual.writeToNBT(new NBTTagCompound());
                    require(!nbt.getKeySet().isEmpty(), "Data editor tile cannot save its state");
                });
                screenshot(client, dataIndex == 0 ? "data-provider-i-editor.png" : "data-provider-ii-editor.png");
                key(gui, (char) 0, Keyboard.KEY_ESCAPE); next(13); break;
            case 13:
                if (!serverReady) break;
                checks.add(dataIndex == 0 ? "native-data-provider-i-final-slot-35-middle-click-edit-server-and-client-sync"
                    : "native-data-provider-ii-final-slot-143-middle-click-edit-server-and-client-sync");
                if (++dataIndex < 2) { openData(client); next(11); }
                else return true;
                break;
            default: throw new IllegalStateException("Unknown GUI phase " + phase);
        }
        return false;
    }

    private void next(int target) { phase = target; since = ticks; }
    private BlockPos busPosition() { return busIndex == 0 ? RuntimeVerification.ITEMS : RuntimeVerification.FLUIDS; }
    private BlockPos dataPosition() { return dataIndex == 0 ? ProviderModeChecks.DATA_I : ProviderModeChecks.DATA_II; }
    private boolean matchesBus(GuiScreen gui) {
        return gui != null && gui.getClass().getName().equals("github.formlessdragon.appcompat.client.gui.mmce.GuiME"
            + (busIndex == 0 ? "Item" : "Fluid") + "InputBus");
    }
    private void openBus(Minecraft client) {
        open(client, busPosition(), "modularmachinery", (busIndex == 0 ? GuiType.ME_ITEM_INPUT_BUS : GuiType.ME_FLUID_INPUT_BUS).ordinal());
    }
    private void openData(Minecraft client) {
        open(client, dataPosition(), MMCEComplement.instance, dataIndex == 0 ? MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER
            : MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II);
    }
    static void open(Minecraft client, BlockPos pos, Object mod, int id) {
        client.getIntegratedServer().addScheduledTask(() -> {
            EntityPlayerMP player = client.getIntegratedServer().getPlayerList().getPlayerByUUID(client.player.getUniqueID());
            player.world.setBlockState(pos.add(0, -1, 3), net.minecraft.init.Blocks.STONE.getDefaultState(), 3);
            player.setPositionAndUpdate(pos.getX() + .5, pos.getY(), pos.getZ() + 3.5);
            player.inventory.setItemStack(ItemStack.EMPTY);
            player.openGui(mod, id, player.world, pos.getX(), pos.getY(), pos.getZ());
        });
    }
    static net.minecraft.tileentity.TileEntity serverTile(Minecraft client, BlockPos pos) {
        return client.getIntegratedServer().getWorld(0).getTileEntity(pos);
    }
    private void serverTask(Minecraft client, Runnable action) {
        serverReady = false;
        client.getIntegratedServer().addScheduledTask(() -> {
            try { action.run(); serverReady = true; }
            catch (Throwable failure) { serverError = failure.toString(); }
        });
    }
    @SuppressWarnings("unchecked") private static List<GuiButton> buttons(GuiScreen gui) throws Exception {
        return (List<GuiButton>) fieldAny(gui, "buttonList", "field_146292_n");
    }
    private static void clickButton(GuiScreen gui, int id) throws Exception {
        for (GuiButton b : buttons(gui)) if (b.id == id) { mouse(gui, b.x + b.width / 2, b.y + b.height / 2, 0); return; }
        throw new IllegalStateException("Missing native provider button " + id);
    }
    static void clickEvent(GuiScreen gui, int id) throws Exception {
        List<GuiButton> buttons = buttons(gui);
        for (GuiButton b : buttons) if (b.id == id) {
            require(MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Pre(gui, b, buttons)),
                "Custom panel action was not handled");
            return;
        }
        throw new IllegalStateException("Missing inventory/ore panel button " + id);
    }
    static Object panel(Class<?> events, GuiScreen gui) throws Exception {
        Field f = events.getDeclaredField("PANELS"); f.setAccessible(true);
        Object panel = ((Map<?, ?>) f.get(null)).get(gui);
        require(panel != null, "Addon controls were not attached to the native GUI"); return panel;
    }
    static void invokePrivate(Class<?> owner, String name, Object argument) throws Exception {
        Method method = owner.getDeclaredMethod(name, argument.getClass()); method.setAccessible(true); method.invoke(null, argument);
    }
    static Object field(Object owner, String name) throws Exception { return fieldAny(owner, name); }
    private static Object fieldAny(Object owner, String... names) throws Exception {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) for (String name : names) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(owner); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(names[0]);
    }
    private static int coordinate(GuiScreen gui, String... names) throws Exception { return (Integer) fieldAny(gui, names); }
    private static void mouse(GuiScreen gui, int x, int y, int button) throws Exception {
        mapped(gui, new String[]{"mouseClicked", "func_73864_a"}, new Class<?>[]{int.class, int.class, int.class}, x, y, button);
    }
    private static void key(GuiScreen gui, char typed, int key) throws Exception {
        mapped(gui, new String[]{"keyTyped", "func_73869_a"}, new Class<?>[]{char.class, int.class}, typed, key);
    }
    private static void mapped(GuiScreen gui, String[] names, Class<?>[] types, Object... args) throws Exception {
        for (Class<?> c = gui.getClass(); c != null; c = c.getSuperclass()) for (String name : names) {
            try { Method m = c.getDeclaredMethod(name, types); m.setAccessible(true); m.invoke(gui, args); return; }
            catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(names[0]);
    }
    static void screenshot(Minecraft client, String file) {
        ScreenShotHelper.saveScreenshot(new java.io.File("."), file, client.displayWidth, client.displayHeight, client.getFramebuffer());
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
