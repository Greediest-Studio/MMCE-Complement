package net.edwin.mmcecomplement.compat.ae2s.mixin;

import ae2.api.crafting.PatternDetailsHelper;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let the original MMCE pattern slots accept AE2S's registered pattern items. */
@Mixin(value = PatternItemFilter.class, remap = false)
public abstract class MixinPatternItemFilter {
    @Inject(method = "allowInsert", at = @At("RETURN"), cancellable = true)
    private void mmceComplement$nativePattern(IItemHandler inventory, int slot, ItemStack stack,
                                             CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValueZ() && PatternDetailsHelper.isEncodedPattern(stack)) ci.setReturnValue(true);
    }
}
