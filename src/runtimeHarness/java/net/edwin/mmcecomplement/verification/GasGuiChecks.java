package net.edwin.mmcecomplement.verification;

import ae2.me.helpers.IGridConnectedTile;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import github.formlessdragon.appcompat.client.gui.mmce.GuiMEGasInputBus;
import github.formlessdragon.appcompat.common.container.mmce.ContainerMEGasInputBus;
import github.kasuminova.mmce.common.tile.MEGasInputBus;
import hellfirepvp.modularmachinery.common.CommonProxy.GuiType;
import me.ramidzkh.mekae2.ae2.AEGasKey;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEGasInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MixedMEInputMarker;
import net.edwin.mmcecomplement.compat.ae2s.client.AE2SAssemblyGui;
import net.edwin.mmcecomplement.compat.ae2s.mekeng.AE2SAssemblyContainer;
import net.edwin.mmcecomplement.MMCEComplement;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.edwin.mmcecomplement.event.ClientInventoryInputGuiEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

/** Native gas containers and Complement settings, with real client/server packets. */
final class GasGuiChecks {
    final List<String> checks = new ArrayList<>();
    private final Gas hydrogen = GasRegistry.getGas("hydrogen");
    private int phase, ticks, since;
    private volatile boolean serverReady;
    private volatile Throwable serverError;

    boolean tick(Minecraft client) throws Exception {
        ticks++;
        if (serverError != null) throw new IllegalStateException("Gas GUI server check failed", serverError);
        if (phase != 0 && ticks - since > 220) throw new IllegalStateException("Gas GUI timed out in phase " + phase);
        ContainerMEGasInputBus container = client.player.openContainer instanceof ContainerMEGasInputBus
            ? (ContainerMEGasInputBus) client.player.openContainer : null;
        switch (phase) {
            case 0:
                if (!GasBusChecks.completed) break;
                open(client, GasBusChecks.INPUT); next(); break;
            case 1:
                if (!(client.currentScreen instanceof GuiMEGasInputBus) || container == null || ticks - since < 12) break;
                container.sendSetConfigGas(0, hydrogen); next(); break;
            case 2:
                if (container == null || container.tankGases.amountAt(0) != 8000
                    || !AEGasKey.of(hydrogen).equals(container.configGases.keyAt(0))) break;
                require(container.configGases.amountAt(0) == 1, "Native gas filter changed its marker amount");
                GuiWorkflowChecks.screenshot(client, "original-gas-input.png");
                checks.add("native-original-gas-gui-config-packet-and-8000-buffer-sync");
                container.sendSetConfigGas(0, null); next(); break;
            case 3:
                if (container == null || container.configGases.keyAt(0) != null || container.tankGases.amountAt(0) != 0) break;
                open(client, GasBusChecks.INVENTORY); next(); break;
            case 4:
                if (!(client.currentScreen instanceof GuiMEGasInputBus) || container == null || ticks - since < 12) break;
                Object panel = GuiWorkflowChecks.panel(ClientInventoryInputGuiEvents.class, client.currentScreen);
                GuiTextField reserve = (GuiTextField) GuiWorkflowChecks.field(panel, "reserve");
                require(reserve.x >= 0 && reserve.y >= 0, "Gas inventory controls are off screen");
                require("1000".equals(reserve.getText())
                    && !((MEInventoryInputBus) container.getOwner()).isActivePull(),
                    "Gas inventory initial settings were not synchronized from server");
                reserve.setText("1250");
                GuiWorkflowChecks.invokePrivate(ClientInventoryInputGuiEvents.class, "normalizeAndSendReserve", panel);
                GuiWorkflowChecks.clickEvent(client.currentScreen, 0x4D49);
                next(); break;
            case 5:
                if (container == null || container.tankGases.amountAt(0) != 18150) break;
                require(container.capacity == Integer.MAX_VALUE, "Native container shrank the inventory buffer");
                require(container.configGases.amountAt(0) == 1, "Inventory filter lost unit amount");
                TileMEGasInventoryInputBus bus = (TileMEGasInventoryInputBus) client.world.getTileEntity(GasBusChecks.INVENTORY);
                if (!bus.isActivePull() || bus.getPermanentReserve() != 1250) break;
                serverTask(client, () -> {
                    TileMEGasInventoryInputBus actual = (TileMEGasInventoryInputBus) GuiWorkflowChecks.serverTile(client, GasBusChecks.INVENTORY);
                    require(actual.isActivePull() && actual.getPermanentReserve() == 1250, "Gas settings packet did not reach server");
                    require(actual.getTanks().getGasStack(0).amount == 18150, "Open gas GUI truncated inventory");
                    require(((IGridConnectedTile) actual).getMainNode().getGrid().getStorageService().getInventory()
                        .getAvailableStacks().get(AEGasKey.of(hydrogen)) == 1250, "Gas GUI pull ignored permanent reserve");
                });
                GuiWorkflowChecks.screenshot(client, "gas-inventory.png"); next(); break;
            case 6:
                if (!serverReady) break;
                checks.add("native-gas-inventory-panel-active-reserve-packets-and-large-buffer-preserved");
                GuiWorkflowChecks.clickEvent(client.currentScreen, 0x4D49); next(); break;
            case 7:
                if (container == null || container.tankGases.amountAt(0) != 0) break;
                serverTask(client, () -> {
                    MEGasInputBus actual = (MEGasInputBus) GuiWorkflowChecks.serverTile(client, GasBusChecks.INVENTORY);
                    require(((IGridConnectedTile) actual).getMainNode().getGrid().getStorageService().getInventory()
                        .getAvailableStacks().get(AEGasKey.of(hydrogen)) == 19400, "Gas GUI standby action lost gas");
                });
                next(); break;
            case 8:
                if (!serverReady) break;
                checks.add("native-gas-gui-standby-returns-exact-buffer-to-network");
                GuiWorkflowChecks.open(client, GasBusChecks.MIXED_INPUT, MMCEComplement.instance, MMCEComplement.GUI_ME_INPUT_ASSEMBLY);
                next(); break;
            case 9:
                if (!(client.currentScreen instanceof AE2SAssemblyGui) || ticks - since < 12) break;
                AE2SAssemblyContainer mixed = (AE2SAssemblyContainer) client.player.openContainer;
                ((AE2SAssemblyContainer.MarkerSlot) mixed.inventorySlots.get(2)).setFilterTo(
                    GenericStack.wrapInItemStack(new GenericStack(AEGasKey.of(hydrogen), 300)));
                ((AE2SAssemblyContainer.MarkerSlot) mixed.inventorySlots.get(3)).setFilterTo(
                    GenericStack.wrapInItemStack(new GenericStack(AEFluidKey.of(FluidRegistry.WATER), 150)));
                ((AE2SAssemblyContainer.MarkerSlot) mixed.inventorySlots.get(4)).setFilterTo(
                    GenericStack.wrapInItemStack(new GenericStack(AEItemKey.of(Items.APPLE), 4)));
                next(); break;
            case 10:
                if (!(client.player.openContainer instanceof AE2SAssemblyContainer) || ticks - since < 20) break;
                AE2SAssemblyContainer synced = (AE2SAssemblyContainer) client.player.openContainer;
                if (MixedMEInputMarker.getAmount(synced.inventorySlots.get(18).getStack()) != 300
                    || MixedMEInputMarker.getAmount(synced.inventorySlots.get(19).getStack()) != 150
                    || synced.inventorySlots.get(20).getStack().getCount() != 4) break;
                serverTask(client, () -> {
                    TileMEInputAssembly actual = (TileMEInputAssembly) GuiWorkflowChecks.serverTile(client, GasBusChecks.MIXED_INPUT);
                    require(actual.getGasTanks().getGasStack(2).amount == 300
                        && actual.getFluidTanks().getFluidInSlot(3).getStackSize() == 150
                        && actual.getInternalInventory().getStackInSlot(4).getCount() == 4,
                        "Mixed GUI filter actions did not reach real scheduler");
                });
                GuiWorkflowChecks.screenshot(client, "mixed-input-assembly.png"); next(); break;
            case 11:
                if (!serverReady) break;
                checks.add("native-mixed-assembly-item-fluid-gas-filter-actions-and-buffer-sync");
                GuiWorkflowChecks.open(client, GasBusChecks.MIXED_OUTPUT, MMCEComplement.instance, MMCEComplement.GUI_ME_OUTPUT_ASSEMBLY);
                next(); break;
            case 12:
                if (!(client.currentScreen instanceof AE2SAssemblyGui) || ticks - since < 15) break;
                require(client.player.openContainer.inventorySlots.size() == 52, "Mixed output GUI slot layout is incomplete");
                require(!client.player.openContainer.inventorySlots.get(0).canTakeStack(client.player), "Mixed output display allows taking resources");
                AE2SAssemblyGui outputGui = (AE2SAssemblyGui) client.currentScreen;
                require(outputGui.getBounds(false).width == 176 && outputGui.getBounds(false).height == 204,
                    "Mixed output GUI crops its background or hotbar");
                for (int i = 16; i < 52; i++) {
                    net.minecraft.inventory.Slot playerSlot = client.player.openContainer.inventorySlots.get(i);
                    int expectedY = i < 43 ? 123 + (i - 16) / 9 * 18 : 181;
                    require(playerSlot.xPos == 8 + (i - 16) % 9 * 18 && playerSlot.yPos == expectedY,
                        "Mixed output player slots are not aligned with the background");
                    require(playerSlot.yPos + 16 <= outputGui.getBounds(false).height - 7,
                        "Mixed output player slot extends outside the panel");
                }
                GuiWorkflowChecks.screenshot(client, "mixed-output-assembly.png");
                checks.add("native-mixed-output-assembly-complete-204px-panel-aligned-player-slots-and-display-only-slots");
                GuiWorkflowChecks.open(client, GasBusChecks.MIXED_INVENTORY, MMCEComplement.instance, MMCEComplement.GUI_ME_INVENTORY_INPUT_ASSEMBLY);
                next(); break;
            case 13:
                if (!(client.currentScreen instanceof AE2SAssemblyGui) || ticks - since < 12) break;
                Object mixedPanel = GuiWorkflowChecks.panel(ClientInventoryInputGuiEvents.class, client.currentScreen);
                GuiTextField mixedReserve = (GuiTextField) GuiWorkflowChecks.field(mixedPanel, "reserve");
                require("7".equals(mixedReserve.getText())
                    && !((MEInventoryInputBus) ((AE2SAssemblyContainer) client.player.openContainer).getOwner()).isActivePull(),
                    "Mixed inventory initial settings were not synchronized from server");
                mixedReserve.setText("125");
                GuiWorkflowChecks.invokePrivate(ClientInventoryInputGuiEvents.class, "normalizeAndSendReserve", mixedPanel);
                require(MixedMEInputMarker.getAmount(client.player.openContainer.inventorySlots.get(2).getStack()) == 1,
                    "Mixed inventory GUI filter is not type-only");
                next(); break;
            case 14:
                if (ticks - since < 20) break;
                serverTask(client, () -> require(((MEInventoryInputBus) GuiWorkflowChecks.serverTile(client, GasBusChecks.MIXED_INVENTORY))
                    .getPermanentReserve() == 125, "Mixed inventory settings packet did not reach server"));
                GuiWorkflowChecks.screenshot(client, "mixed-inventory-assembly.png"); next(); break;
            case 15:
                if (!serverReady) break;
                checks.add("native-mixed-inventory-gui-unit-markers-and-reserve-panel-server-sync");
                GuiWorkflowChecks.open(client, GasBusChecks.FULL_EXPOSURE, MMCEComplement.instance, MMCEComplement.GUI_ME_FULL_EXPOSURE_ASSEMBLY);
                next(); break;
            case 16:
                if (!(client.currentScreen instanceof AE2SAssemblyGui) || ticks - since < 15) break;
                Object fullPanel = GuiWorkflowChecks.panel(ClientInventoryInputGuiEvents.class, client.currentScreen);
                GuiTextField fullReserve = (GuiTextField) GuiWorkflowChecks.field(fullPanel, "reserve");
                require("7".equals(fullReserve.getText())
                    && !((MEInventoryInputBus) ((AE2SAssemblyContainer) client.player.openContainer).getOwner()).isActivePull(),
                    "Full exposure initial settings were not synchronized from server");
                for (int i = 0; i < 16; i++) require(!(client.player.openContainer.inventorySlots.get(i) instanceof AE2SAssemblyContainer.MarkerSlot),
                    "Full exposure GUI allows editing a filter");
                GuiWorkflowChecks.screenshot(client, "full-exposure-assembly.png");
                client.player.closeScreen();
                next(); break;
            case 17:
                if (ticks - since < 10) break;
                serverTask(client, () -> require(((MEInventoryInputBus) GuiWorkflowChecks.serverTile(client, GasBusChecks.FULL_EXPOSURE))
                    .getPermanentReserve() == 7, "Closing full exposure GUI overwrote server reserve"));
                next(); break;
            case 18:
                if (!serverReady) break;
                checks.add("native-full-exposure-gui-initial-settings-read-only-filter-and-close-preserves-reserve");
                return true;
            default: throw new IllegalStateException("Unknown gas GUI phase " + phase);
        }
        return false;
    }
    private void next() { phase++; since = ticks; }
    private static void open(Minecraft client, BlockPos pos) {
        GuiWorkflowChecks.open(client, pos, "modularmachinery", GuiType.ME_GAS_INPUT_BUS.ordinal());
    }
    private void serverTask(Minecraft client, Runnable action) {
        serverReady = false;
        client.getIntegratedServer().addScheduledTask(() -> {
            try { action.run(); serverReady = true; } catch (Throwable failure) { serverError = failure; }
        });
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
