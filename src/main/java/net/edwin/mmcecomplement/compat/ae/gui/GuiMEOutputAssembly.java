package net.edwin.mmcecomplement.compat.ae.gui;

import github.kasuminova.mmce.client.gui.GuiMEItemBus;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.ae.tile.MixedMEInputMarker;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.edwin.mmcecomplement.filter.CompactQuantityFormatter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** MMCE item-bus styled GUI for the mixed output assembly. */
public class GuiMEOutputAssembly extends GuiMEItemBus {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        "mmce_complement", "textures/gui/me_output_assembly.png");
    private final TileMEOutputAssembly tile;

    public GuiMEOutputAssembly(TileMEOutputAssembly tile,
                               EntityPlayer player) {
        super(new ContainerMEOutputAssembly(tile, player));
        this.tile = tile;
        this.ySize = 186;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_output_assembly.title"),
            8, 8, 0x404040);
        fontRenderer.drawString(I18n.format(
            "gui.mmce_complement.me_output_assembly.stored"),
            8, 24, 0x404040);
        fontRenderer.drawString(I18n.format("gui.mmce_complement.me_output_assembly.inventory"),
            8, ySize - 93, 0x404040);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
    }

    @Override
    public void drawSlot(Slot slot) {
        ItemStack stack = slot.getStack();
        FluidStack fluid = MixedMEInputMarker.getFluid(stack);
        GasStack gas = MixedMEInputMarker.getGas(stack);
        if (fluid == null && gas == null) {
            super.drawSlot(slot);
            return;
        }
        TextureAtlasSprite sprite;
        int tint;
        if (fluid != null) {
            sprite = mc.getTextureMapBlocks().getAtlasSprite(
                fluid.getFluid().getStill(fluid).toString());
            tint = fluid.getFluid().getColor(fluid);
        } else {
            if (gas == null || gas.getGas() == null) return;
            sprite = gas.getGas().getSprite();
            if (sprite == null && gas.getGas().getIcon() != null) {
                sprite = mc.getTextureMapBlocks().getAtlasSprite(
                    gas.getGas().getIcon().toString());
            }
            tint = gas.getGas().getTint();
        }
        if (sprite == null) return;
        float red = ((tint >> 16) & 0xFF) / 255F;
        float green = ((tint >> 8) & 0xFF) / 255F;
        float blue = (tint & 0xFF) / 255F;
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.color(red, green, blue, 1F);
        drawTexturedModalRect(slot.xPos, slot.yPos, sprite, 16, 16);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        String amount = CompactQuantityFormatter.format(
            MixedMEInputMarker.getAmount(stack));
        fontRenderer.drawStringWithShadow(amount,
            slot.xPos + 17 - fontRenderer.getStringWidth(amount),
            slot.yPos + 9, 0xFFFFFF);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
    }

    @Override
    protected void renderToolTip(ItemStack stack, int x, int y) {
        FluidStack fluid = MixedMEInputMarker.getFluid(stack);
        if (fluid != null) {
            List<String> lines = new ArrayList<>();
            lines.add(fluid.getLocalizedName());
            lines.add(TextFormatting.GRAY + I18n.format(
                "gui.mmce_complement.me_output_assembly.fluid_amount",
                fluid.amount));
            drawHoveringText(lines, x, y, fontRenderer);
            return;
        }
        GasStack gas = MixedMEInputMarker.getGas(stack);
        if (gas != null && gas.getGas() != null) {
            List<String> lines = new ArrayList<>();
            lines.add(gas.getGas().getLocalizedName());
            lines.add(TextFormatting.GRAY + I18n.format(
                "gui.mmce_complement.me_output_assembly.gas_amount",
                gas.amount));
            drawHoveringText(lines, x, y, fontRenderer);
            return;
        }
        super.renderToolTip(stack, x, y);
    }
}
