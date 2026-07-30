package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.edwin.mmcecomplement.cycle.CycleItemRecipeHandler;
import net.edwin.mmcecomplement.filter.FilteredItemRecipeHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes MMCE count only the quota that a cycle output really accepted. */
@Mixin(value = ItemUtils.class, remap = false)
public abstract class MixinItemUtilsCycle {
    @Inject(method = "insertAll", at = @At("HEAD"), cancellable = true)
    private static void mmceComplement$insertSelfCycleExactly(
        ItemStack stack, IItemHandlerModifiable handler, int amount,
        CallbackInfoReturnable<Integer> cir) {
        if (handler instanceof CycleItemRecipeHandler) {
            cir.setReturnValue(((CycleItemRecipeHandler) handler)
                .mmceComplement$insertCycleOutput(stack, amount));
        } else if (handler instanceof FilteredItemRecipeHandler) {
            cir.setReturnValue(((FilteredItemRecipeHandler) handler)
                .insertFilteredOutput(stack, amount, false));
        }
    }
}
