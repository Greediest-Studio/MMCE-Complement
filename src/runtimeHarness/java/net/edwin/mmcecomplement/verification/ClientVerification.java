package net.edwin.mmcecomplement.verification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.edwin.mmcecomplement.compat.ae2s.client.AE2SPatternProviderGui;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae2s.AE2SPatternProviderContainer;
import net.edwin.mmcecomplement.MMCEComplement;
import appeng.helpers.InventoryAction;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@SideOnly(Side.CLIENT)
public final class ClientVerification {
    private static volatile boolean completed;
    private static volatile boolean interactionsComplete;
    private static volatile String interactionError;
    private boolean launched, guiOpened;
    private int rendered, guiTicks, interactionPhase, interactionTick;
    private boolean screenshot;
    private boolean fluidComplete;
    private boolean workflowsComplete;
    private final GuiWorkflowChecks workflows = new GuiWorkflowChecks();
    private GasGuiChecks gasGuiChecks;
    public static boolean interactionsComplete() { return interactionsComplete; }
    public static void install() { MinecraftForge.EVENT_BUS.register(new ClientVerification()); }
    public static void finish() { completed = true; }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        if (!launched && client.currentScreen instanceof GuiMainMenu) {
            launched = true;
            client.gameSettings.pauseOnLostFocus = false;
            client.gameSettings.guiScale = 2;
            client.launchIntegratedServer("verification-world", "MMCE Complement AE2S Verification",
                new WorldSettings(5L, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands());
        }
        if (client.world != null && client.player != null) {
            rendered++;
            if (!guiOpened && rendered > 60 && client.world.getTileEntity(RuntimeVerification.PROVIDER) instanceof TileMEPatternProviderII) {
                guiOpened = true;
                client.getIntegratedServer().addScheduledTask(() -> {
                    net.minecraft.entity.player.EntityPlayerMP player = client.getIntegratedServer().getPlayerList().getPlayerByUUID(client.player.getUniqueID());
                    player.world.setBlockState(RuntimeVerification.PROVIDER.add(0, -1, 3), net.minecraft.init.Blocks.STONE.getDefaultState(), 3);
                    player.setPositionAndUpdate(4.5, 80, 3.5);
                    player.openGui(MMCEComplement.instance, MMCEComplement.GUI_ME_PATTERN_PROVIDER_II,
                        player.world, RuntimeVerification.PROVIDER.getX(), RuntimeVerification.PROVIDER.getY(), RuntimeVerification.PROVIDER.getZ());
                });
            }
            if (client.currentScreen instanceof AE2SPatternProviderGui) {
                guiTicks++;
                if (guiTicks >= 20 && !fluidComplete && interactionError == null) {
                    try { interact(client); } catch (Throwable failure) { fail(failure.toString()); }
                }
            }
            if (fluidComplete && !interactionsComplete) {
                try {
                    if (!workflowsComplete) workflowsComplete = workflows.tick(client);
                    if (workflowsComplete) {
                        if (!net.minecraftforge.fml.common.Loader.isModLoaded("mekeng")) interactionsComplete = true;
                        else {
                            if (gasGuiChecks == null) gasGuiChecks = new GasGuiChecks();
                            interactionsComplete = gasGuiChecks.tick(client);
                        }
                    }
                }
                catch (Throwable failure) { failure.printStackTrace(); fail(failure.toString()); }
            }
        }
        if (completed && rendered >= 100 && guiTicks >= 20) {
            JsonObject report = new JsonParser().parse(new String(Files.readAllBytes(Paths.get("verification-report.json")), StandardCharsets.UTF_8)).getAsJsonObject();
            report.addProperty("clientWorldRenderedTicks", rendered); report.addProperty("patternProviderGuiTicks", guiTicks);
            report.addProperty("clientFluidSlotInteraction", fluidComplete);
            report.add("clientGuiChecks", new com.google.gson.Gson().toJsonTree(workflows.checks));
            if (gasGuiChecks != null) report.add("clientGasGuiChecks", new com.google.gson.Gson().toJsonTree(gasGuiChecks.checks));
            if (interactionError != null) { report.addProperty("result", "FAIL"); report.addProperty("clientError", interactionError); }
            report.addProperty("clientScreenshot", "provider-ii.png");
            Files.write(Paths.get("verification-report.json"), report.toString().getBytes(StandardCharsets.UTF_8));
            client.shutdown();
        }
    }

    private void interact(Minecraft client) {
        if (!(client.player.openContainer instanceof AE2SPatternProviderContainer)) throw new IllegalStateException("Provider II has no server-backed container");
        if (client.player.openContainer.inventorySlots.size() != 189) throw new IllegalStateException("Provider II slot layout is incomplete");
        if (interactionPhase == 0) {
            interactionPhase = 1; interactionTick = guiTicks;
            client.getIntegratedServer().addScheduledTask(() -> {
                net.minecraft.entity.player.EntityPlayerMP player = client.getIntegratedServer().getPlayerList().getPlayerByUUID(client.player.getUniqueID());
                player.inventory.setItemStack(new ItemStack(Items.WATER_BUCKET));
                player.connection.sendPacket(new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
            });
        } else if (interactionPhase == 1 && guiTicks > interactionTick + 10) {
            interactionPhase = 2; interactionTick = guiTicks;
            ((AE2SPatternProviderContainer) client.player.openContainer).sendFluidAction(1, false);
        } else if (interactionPhase == 2 && guiTicks > interactionTick + 20) {
            AE2SPatternProviderContainer container = (AE2SPatternProviderContainer) client.player.openContainer;
            if (container.subFluid.get(1) == null || container.subFluid.amountAt(1) != 1000
                || client.player.inventory.getItemStack().getItem() != Items.BUCKET) {
                if (guiTicks > interactionTick + 100) fail("Fluid empty action or server-to-client tank synchronization failed");
                return;
            }
            if (!screenshot) {
                ScreenShotHelper.saveScreenshot(new java.io.File("."), "provider-ii.png", client.displayWidth, client.displayHeight, client.getFramebuffer());
                screenshot = true;
            }
            interactionPhase = 3; interactionTick = guiTicks;
            ((AE2SPatternProviderContainer) client.player.openContainer).sendFluidAction(1, true);
        } else if (interactionPhase == 3 && guiTicks > interactionTick + 20) {
            AE2SPatternProviderContainer container = (AE2SPatternProviderContainer) client.player.openContainer;
            if (container.subFluid.get(1) != null || client.player.inventory.getItemStack().getItem() != Items.WATER_BUCKET) {
                if (guiTicks > interactionTick + 100) fail("Fluid fill action or held-item synchronization failed");
                return;
            }
            interactionPhase = 4;
            fluidComplete = true;
        }
    }
    private void fail(String message) { interactionError = message; interactionsComplete = true; interactionPhase = 4; }
}
