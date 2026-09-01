package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentGas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.preview.GasTooltipData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Shows custom gas tooltip lines in the JEI hover tooltip. */
@Pseudo
@Mixin(targets =
    "hellfirepvp.modularmachinery.common.crafting.requirement.jei.JEIComponentGas",
    remap = false)
public abstract class MixinJEIComponentGasPreviewNBT {
    @Shadow @Final private RequirementGas requirement;

    @Inject(method = "onJEIHoverTooltip(IZLmekanism/api/gas/GasStack;Ljava/util/List;)V",
        at = @At("RETURN"))
    private void mmceComplement$appendGasTooltip(
        int slotIndex, boolean input, GasStack stack, List<String> tooltip,
        CallbackInfo ci) {
        if (requirement instanceof GasTooltipData) {
            tooltip.addAll(((GasTooltipData) requirement)
                .mmceComplement$getGasTooltip());
        }
    }
}
