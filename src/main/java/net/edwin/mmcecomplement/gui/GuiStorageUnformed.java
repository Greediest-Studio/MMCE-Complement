package net.edwin.mmcecomplement.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.edwin.mmcecomplement.variant.StorageController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Vanilla-style notice shown until the player closes it. */
public final class GuiStorageUnformed extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "mmce_complement", "textures/gui/storage_unformed.png");
    private final String machineTitle;

    public GuiStorageUnformed(StorageController storage) {
        super(new ContainerStorageUnformed());
        machineTitle = getMachineTitle(storage);
        xSize = 222;
        ySize = 64;
    }

    private static DynamicMachine getTitleMachine(StorageController storage) {
        if (!(storage instanceof TileMachineController)) return null;
        TileMachineController controller = (TileMachineController) storage;
        DynamicMachine machine = controller.getFoundMachine();
        if (machine == null) machine = controller.getParentMachine();
        if (machine == null && storage instanceof TileEntity) {
            Block block = ((TileEntity) storage).getBlockType();
            if (block instanceof hellfirepvp.modularmachinery.common.block.BlockController) {
                machine = ((hellfirepvp.modularmachinery.common.block.BlockController) block)
                    .getParentMachine();
            }
        }
        return machine;
    }

    private static String getMachineTitle(StorageController storage) {
        DynamicMachine machine = getTitleMachine(storage);
        if (machine != null && machine.getLocalizedName() != null
            && !machine.getLocalizedName().isEmpty()) return machine.getLocalizedName();
        if (storage instanceof TileMachineController) {
            String formed = ((TileMachineController) storage).getFormedMachineName();
            if (formed != null && !formed.isEmpty()) return formed;
        }
        return I18n.format("gui.mmce_complement.storage.title");
    }


    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,
                                                               int mouseX, int mouseY) {
        // Use the dedicated unformed-state panel texture; its border highlights,
        // dark lower edge, and light-gray background have exactly the same
        // palette as the full-size storage GUI.
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(machineTitle,
            8, 6, 0x404040);
        String message = I18n.format("gui.mmce_complement.storage.unformed");
        fontRenderer.drawString(message,
            (xSize - fontRenderer.getStringWidth(message)) / 2, 37, 0x404040);
    }
}


