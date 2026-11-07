package net.edwin.mmcecomplement.compat.ae2s.mixin;

import ae2.client.gui.AEBaseGui;
import ae2.container.AEBaseContainer;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFluidInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssemblyFallback;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the addon's titles on the native screens supplied by AppliedCompatibility. */
@Mixin(value = AEBaseGui.class, remap = false)
public abstract class MixinNativeBusTitles extends GuiContainer {
    protected MixinNativeBusTitles(Container container) { super(container); }
    @Shadow protected abstract void setTextContent(String id, ITextComponent content);

    @Inject(method = {"initGui", "func_73866_w_"}, at = @At("RETURN"), require = 1)
    private void mmceComplement$title(CallbackInfo ci) {
        if (!(inventorySlots instanceof AEBaseContainer)) return;
        Object owner = ((AEBaseContainer) inventorySlots).getTarget();
        String title = owner instanceof TileMEOreDictInputBus ? "gui.mmce_complement.me_ore_dict.bus_title"
            : owner instanceof TileMEFullExposureAssemblyFallback ? "gui.mmce_complement.me_full_exposure_assembly.title"
            : owner instanceof TileMEItemInventoryInputBus ? "gui.mmce_complement.me_inventory.item_title"
            : owner instanceof TileMEFluidInventoryInputBus ? "gui.mmce_complement.me_inventory.fluid_title" : null;
        if (title != null) {
            String text = I18n.format(title);
            int available = Math.max(1, xSize - 16);
            if (fontRenderer.getStringWidth(text) > available) {
                text = fontRenderer.trimStringToWidth(text, Math.max(1, available - fontRenderer.getStringWidth("..."))) + "...";
            }
            setTextContent("dialog_title", new TextComponentString(text));
        }
    }
}
