package net.edwin.mmcecomplement.compat.ae2s.mixin;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.tile.inventory.AppEngInternalInventory;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.compat.ae2s.AE2SNetwork;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Decode the addon's legacy pattern callbacks through the installed AE2S registry. */
@Mixin(value = MEPatternProvider.class, remap = false, priority = 900)
public abstract class MixinMEPatternProvider {
    @Shadow public abstract AppEngInternalInventory getPatterns();
    @Shadow @Final protected ICraftingPatternDetails[] details;
    @Shadow protected MEPatternProvider.WorkModeSetting workMode;
    @Shadow protected int currentPatternIdx;
    @Shadow protected ICraftingPatternDetails currentPattern;
    @Shadow private void resetCurrentPattern() { throw new AssertionError(); }

    @Inject(method = "refreshPattern", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$decodeNativePattern(int slot, CallbackInfo ci) {
        if (!getClass().getName().startsWith("net.edwin.mmcecomplement.")) return;
        if (slot >= 0 && slot < details.length) {
            ICraftingPatternDetails decoded = AE2SNetwork.decodePattern(getPatterns().getStackInSlot(slot),
                ((MEMachineComponent) (Object) this).getWorld());
            details[slot] = decoded != null && !decoded.isCraftable() ? decoded : null;
            if (workMode == MEPatternProvider.WorkModeSetting.ENHANCED_BLOCKING_MODE && slot == currentPatternIdx) {
                if (currentPattern == null) currentPattern = details[slot];
                else if (!currentPattern.equals(details[slot])) resetCurrentPattern();
            }
        }
        ci.cancel();
    }
    @Inject(method = "openTerminalPatternContainerGui", at = @At("HEAD"), cancellable = true, require = 1)
    private void mmceComplement$openTerminal(net.minecraft.entity.player.EntityPlayer player, CallbackInfo ci) {
        Object tile = this;
        int id;
        if (tile instanceof net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII)
            id = net.edwin.mmcecomplement.MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II;
        else if (tile instanceof net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII)
            id = net.edwin.mmcecomplement.MMCEComplement.GUI_ME_PATTERN_PROVIDER_II;
        else if (tile instanceof net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider)
            id = net.edwin.mmcecomplement.MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER;
        else return;
        MEMachineComponent component = (MEMachineComponent) tile;
        player.openGui(net.edwin.mmcecomplement.MMCEComplement.instance, id, component.getWorld(),
            component.getPos().getX(), component.getPos().getY(), component.getPos().getZ());
        ci.cancel();
    }

}
