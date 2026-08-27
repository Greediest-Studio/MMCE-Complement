package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.container.interfaces.IJEIGhostIngredients;
import appeng.container.slot.SlotFake;
import appeng.core.localization.GuiText;
import github.kasuminova.mmce.client.gui.GuiMEItemBus;
import mekanism.api.gas.GasStack;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.edwin.mmcecomplement.compat.ae.tile.MixedMEInputMarker;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInventoryInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly;
import net.edwin.mmcecomplement.filter.CompactQuantityFormatter;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Mouse;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Item-bus styled GUI with square rendering for all three resource types. */
public class GuiMEInputAssembly extends GuiMEItemBus
    implements IJEIGhostIngredients {

    private static final ResourceLocation BACKGROUND = new ResourceLocation(
        "modularmachinery", "textures/gui/meiteminputbus.png");

    private final TileMEInputAssembly tile;
    private final Map<IGhostIngredientHandler.Target<?>, Object> targets =
        new HashMap<>();

    public GuiMEInputAssembly(TileMEInputAssembly tile,
                              EntityPlayer player) {
        super(new ContainerMEInputAssembly(tile, player));
        this.tile = tile;
        this.ySize = 204;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        String titleKey = "gui.mmce_complement.me_input_assembly.title";
        if (tile instanceof TileMEFullExposureAssembly) {
            titleKey = "gui.mmce_complement.me_full_exposure_assembly.title";
        } else if (tile instanceof TileMEInventoryInputAssembly) {
            titleKey = "gui.mmce_complement.me_inventory_input_assembly.title";
        }
        fontRenderer.drawString(
            I18n.format(titleKey),
            8, 8, 0x404040);
        fontRenderer.drawString(GuiText.Config.getLocal(),
            8, 24, 0x404040);
        fontRenderer.drawString(GuiText.StoredItems.getLocal(),
            97, 24, 0x404040);
        fontRenderer.drawString(GuiText.inventory.getLocal(),
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
        if (!MixedMEInputMarker.isResourceMarker(stack)) {
            super.drawSlot(slot);
            return;
        }

        TextureAtlasSprite sprite;
        int tint;
        FluidStack fluid = MixedMEInputMarker.getFluid(stack);
        if (fluid != null) {
            ResourceLocation texture = fluid.getFluid().getStill(fluid);
            sprite = mc.getTextureMapBlocks().getAtlasSprite(
                texture.toString());
            tint = fluid.getFluid().getColor(fluid);
        } else {
            GasStack gas = MixedMEInputMarker.getGas(stack);
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
        float alpha = ((tint >> 24) & 0xFF) / 255F;
        if (alpha <= 0F) alpha = 1F;
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.color(red, green, blue, alpha);
        drawTexturedModalRect(slot.xPos, slot.yPos, sprite, 16, 16);
        GlStateManager.color(1F, 1F, 1F, 1F);

        String amount = CompactQuantityFormatter.format(
            MixedMEInputMarker.getAmount(stack));
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
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
                "gui.mmce_complement.me_input_assembly.fluid_amount",
                fluid.amount));
            drawHoveringText(lines, x, y, fontRenderer);
            return;
        }
        GasStack gas = MixedMEInputMarker.getGas(stack);
        if (gas != null && gas.getGas() != null) {
            List<String> lines = new ArrayList<>();
            lines.add(gas.getGas().getLocalizedName());
            lines.add(TextFormatting.GRAY + I18n.format(
                "gui.mmce_complement.me_input_assembly.gas_amount",
                gas.amount));
            drawHoveringText(lines, x, y, fontRenderer);
            return;
        }
        super.renderToolTip(stack, x, y);
    }

    /**
     * AEBaseGui only forwards a wheel event while Shift is held. MMCE's
     * ordinary input bus deliberately reads the raw event a second time so
     * plain wheel, Shift, Ctrl and Shift+Ctrl all work. Mirror that path here.
     */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height
            / mc.displayHeight - 1;
        adjustMarkerAmount(mouseX, mouseY,
            delta / Math.abs(delta));
    }

    @Override
    protected void mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        // handleMouseInput processes the raw event for all modifier states.
        // Keeping AEBaseGui's Shift-only callback empty prevents double steps.
    }

    private void adjustMarkerAmount(int mouseX, int mouseY, int wheel) {
        // Inventory-style assemblies are type-only filters.  Their marker
        // amount is intentionally fixed at one and must not react to the
        // normal input-bus wheel controls.
        if (tile instanceof TileMEInventoryInputAssembly) return;
        Slot slot = getSlot(mouseX, mouseY);
        if (!(slot instanceof SlotFake) || slot.getStack().isEmpty()) return;
        ItemStack marker = slot.getStack();
        long current = MixedMEInputMarker.getAmount(marker);
        long updated;
        if (isShiftKeyDown() && isCtrlKeyDown()) {
            updated = wheel > 0 ? Math.min(Integer.MAX_VALUE, current * 2L)
                : Math.max(1L, current / 2L);
        } else {
            long step = isCtrlKeyDown() ? 100L : isShiftKeyDown() ? 10L : 1L;
            updated = wheel > 0 ? Math.min(Integer.MAX_VALUE, current + step)
                : Math.max(1L, current - step);
        }
        sendMarker(slot.getSlotIndex(),
            MixedMEInputMarker.withAmount(marker, updated));
    }

    @Override
    public List<IGhostIngredientHandler.Target<?>> getPhantomTargets(
        Object ingredient) {
        if (tile instanceof TileMEFullExposureAssembly) {
            return Collections.emptyList();
        }
        final ItemStack marker;
        if (ingredient instanceof ItemStack) {
            marker = ((ItemStack) ingredient).copy();
        } else if (ingredient instanceof FluidStack) {
            marker = MixedMEInputMarker.fluid((FluidStack) ingredient);
        } else if (ingredient instanceof GasStack) {
            marker = MixedMEInputMarker.gas((GasStack) ingredient);
        } else {
            return Collections.emptyList();
        }
        if (marker.isEmpty()) return Collections.emptyList();

        List<IGhostIngredientHandler.Target<?>> result = new ArrayList<>();
        targets.clear();
        for (int slot = 0; slot < TileMEInputAssembly.SLOT_COUNT; slot++) {
            final int targetSlot = slot;
            Rectangle area = new Rectangle(guiLeft + 8 + (slot % 4) * 18,
                guiTop + 35 + (slot / 4) * 18, 16, 16);
            IGhostIngredientHandler.Target<Object> target =
                new IGhostIngredientHandler.Target<Object>() {
                    @Override
                    public Rectangle getArea() {
                        return area;
                    }

                    @Override
                    public void accept(Object ignored) {
                        sendMarker(targetSlot, marker.copy());
                    }
                };
            result.add(target);
            targets.put(target, ingredient);
        }
        return result;
    }

    @Override
    public Map<IGhostIngredientHandler.Target<?>, Object>
    getFakeSlotTargetMap() {
        return targets;
    }

    private void sendMarker(int slot, ItemStack marker) {
        tile.setMarker(slot, marker);
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("slot", slot);
        payload.setLong("amount", MixedMEInputMarker.getAmount(marker));
        if (!marker.isEmpty()) {
            // Vanilla ItemStack NBT stores Count as one signed byte. Preserve
            // the real int-sized item target separately, as MMCE's normal bus
            // does through its integer inventory-action packet.
            ItemStack serialized = marker.copy();
            if (MixedMEInputMarker.getType(serialized)
                == MixedMEInputMarker.TYPE_ITEM) {
                serialized.setCount(1);
            }
            NBTTagCompound markerTag = new NBTTagCompound();
            serialized.writeToNBT(markerTag);
            payload.setTag("marker", markerTag);
        }
        NetworkHandlerMMCE.CHANNEL.sendToServer(
            new NetworkHandlerMMCE.SetHatchFieldMessage(tile.getPos(),
                tile.getWorld().provider.getDimension(),
                NetworkHandlerMMCE.FIELD_ME_INPUT_ASSEMBLY_MARKER,
                payload));
    }
}
