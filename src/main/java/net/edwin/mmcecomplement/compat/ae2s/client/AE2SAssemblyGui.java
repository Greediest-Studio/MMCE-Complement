package net.edwin.mmcecomplement.compat.ae2s.client;

import ae2.api.behaviors.EmptyingAction;
import ae2.api.stacks.GenericStack;
import ae2.client.gui.style.GuiStyleManager;
import ae2.container.slot.AppEngSlot;
import ae2.integration.modules.hei.GenericIngredientHelper;
import github.formlessdragon.appcompat.client.gui.mmce.GuiMEBusBase;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.ae.tile.*;
import net.edwin.mmcecomplement.compat.ae2s.mekeng.AE2SAssemblyContainer;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Mouse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** Mixed resource rendering and editing over native AE2S container actions. */
public final class AE2SAssemblyGui extends GuiMEBusBase<AE2SAssemblyContainer> {
    private final MEMachineComponent owner;
    public AE2SAssemblyGui(MEMachineComponent owner, EntityPlayer player) {
        super(new AE2SAssemblyContainer(owner, player), player.inventory,
            GuiStyleManager.loadStyleDoc("/screens/mmce_complement_assembly_" + style(owner) + ".json"));
        this.owner = owner;
    }
    private static String style(MEMachineComponent owner) {
        if (owner instanceof TileMEFullExposureAssembly) return "full";
        if (owner instanceof TileMEInventoryInputAssembly) return "inventory";
        return owner instanceof TileMEInputAssembly ? "input" : "output";
    }
    @Override public void initGui() {
        super.initGui();
        String kind = owner instanceof TileMEFullExposureAssembly ? "full_exposure"
            : owner instanceof TileMEInventoryInputAssembly ? "inventory_input"
            : owner instanceof TileMEInputAssembly ? "input" : "output";
        String title = I18n.format("gui.mmce_complement.me_" + kind + "_assembly.title");
        int available = Math.max(1, xSize - 16);
        if (fontRenderer.getStringWidth(title) > available) {
            title = fontRenderer.trimStringToWidth(title,
                Math.max(1, available - fontRenderer.getStringWidth("..."))) + "...";
        }
        setTextContent("dialog_title", new TextComponentString(title));
    }
    @Override protected boolean shouldRenderSingleCountSlot(Slot slot) { return slot instanceof AppEngSlot; }
    @Override protected EmptyingAction getEmptyingAction(Slot slot, ItemStack stack) {
        return slot instanceof AE2SAssemblyContainer.MarkerSlot ? null : super.getEmptyingAction(slot, stack);
    }
    @Override public void drawSlot(Slot slot) {
        ItemStack stack = slot.getStack();
        if (!MixedMEInputMarker.isResourceMarker(stack)) { super.drawSlot(slot); return; }
        FluidStack fluid = MixedMEInputMarker.getFluid(stack);
        GasStack gas = MixedMEInputMarker.getGas(stack);
        TextureAtlasSprite sprite;
        int tint;
        if (fluid != null) {
            sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
            tint = fluid.getFluid().getColor(fluid);
        } else {
            if (gas == null || gas.getGas() == null) return;
            sprite = gas.getGas().getSprite();
            if (sprite == null && gas.getGas().getIcon() != null) sprite = mc.getTextureMapBlocks().getAtlasSprite(gas.getGas().getIcon().toString());
            tint = gas.getGas().getTint();
        }
        if (sprite == null) return;
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.color((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 1F);
        drawTexturedModalRect(slot.xPos, slot.yPos, sprite, 16, 16);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableBlend();
        if (!(slot instanceof AE2SAssemblyContainer.MarkerSlot && owner instanceof MEInventoryInputBus)) {
            String amount = CompactQuantityFormatter.format(MixedMEInputMarker.getAmount(stack));
            GlStateManager.disableLighting(); GlStateManager.disableDepth(); GlStateManager.disableBlend();
            fontRenderer.drawStringWithShadow(amount, slot.xPos + 17 - fontRenderer.getStringWidth(amount), slot.yPos + 9, 0xFFFFFF);
            GlStateManager.enableLighting(); GlStateManager.enableDepth();
        }
    }
    @Override protected void renderHoveredToolTip(int x, int y) {
        Slot slot = getSlotUnderMouse();
        if (slot != null && MixedMEInputMarker.isResourceMarker(slot.getStack())) {
            FluidStack fluid = MixedMEInputMarker.getFluid(slot.getStack());
            GasStack gas = MixedMEInputMarker.getGas(slot.getStack());
            String name = fluid != null ? fluid.getLocalizedName() : gas == null ? "" : gas.getGas().getLocalizedName();
            drawHoveringText(java.util.Arrays.asList(name, MixedMEInputMarker.getAmount(slot.getStack()) + " mB"), x, y);
            return;
        }
        super.renderHoveredToolTip(x, y);
    }
    @Override public Collection<? extends Slot> getHEISlots(Object ingredient) {
        GenericStack generic = GenericIngredientHelper.ingredientToStack(ingredient);
        ItemStack marker = ingredient instanceof ItemStack ? AE2SAssemblyContainer.marker((ItemStack) ingredient)
            : AE2SAssemblyContainer.marker(generic);
        if (marker.isEmpty()) return Collections.emptyList();
        Collection<Slot> slots = new ArrayList<>();
        for (Slot slot : inventorySlots.inventorySlots) if (slot instanceof AE2SAssemblyContainer.MarkerSlot) slots.add(slot);
        return slots;
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) adjustMarkerAmount(Mouse.getEventX() * width / mc.displayWidth,
            height - Mouse.getEventY() * height / mc.displayHeight - 1, wheel > 0 ? 1 : -1);
    }
    private void adjustMarkerAmount(int x, int y, int wheel) {
        if (owner instanceof MEInventoryInputBus || !(owner instanceof TileMEInputAssembly)) return;
        Slot slot = findSlot(x, y);
        if (!(slot instanceof AE2SAssemblyContainer.MarkerSlot) || slot.getStack().isEmpty()) return;
        long current = MixedMEInputMarker.getAmount(slot.getStack());
        long amount = isShiftKeyDown() && isCtrlKeyDown() ? (wheel > 0 ? current * 2 : current / 2)
            : current + wheel * (isCtrlKeyDown() ? 100 : isShiftKeyDown() ? 10 : 1);
        ItemStack marker = MixedMEInputMarker.withAmount(slot.getStack(), amount);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("slot", slot.getSlotIndex()); tag.setTag("marker", marker.writeToNBT(new NBTTagCompound()));
        tag.setLong("amount", MixedMEInputMarker.getAmount(marker));
        NetworkHandlerMMCE.CHANNEL.sendToServer(new NetworkHandlerMMCE.SetHatchFieldMessage(owner.getPos(),
            owner.getWorld().provider.getDimension(), NetworkHandlerMMCE.FIELD_ME_INPUT_ASSEMBLY_MARKER, tag));
    }
}
