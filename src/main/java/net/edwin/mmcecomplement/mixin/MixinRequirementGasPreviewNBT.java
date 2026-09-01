package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import net.edwin.mmcecomplement.preview.GasTooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

/** Adds custom JEI tooltip lines to MMCE's Mekanism gas requirement. */
@Pseudo
@Mixin(targets =
    "hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas",
    remap = false)
public abstract class MixinRequirementGasPreviewNBT implements GasTooltipData {

    @Unique
    private final List<String> mmceComplement$gasTooltip = new ArrayList<>();

    @Override
    public void mmceComplement$addGasTooltip(String line) {
        if (line != null && !line.isEmpty()) {
            mmceComplement$gasTooltip.add(line);
        }
    }

    @Override
    public void mmceComplement$clearGasTooltip() {
        mmceComplement$gasTooltip.clear();
    }

    @Override
    public List<String> mmceComplement$getGasTooltip() {
        return new ArrayList<>(mmceComplement$gasTooltip);
    }

    @Inject(method = "deepCopyModified(Ljava/util/List;)"
        + "Lhellfirepvp/modularmachinery/common/crafting/requirement/"
        + "RequirementGas;", at = @At("RETURN"))
    private void mmceComplement$copyGasTooltip(
        List<?> modifiers, CallbackInfoReturnable<RequirementGas> cir) {
        GasTooltipData tooltipCopy = (GasTooltipData) (Object) cir.getReturnValue();
        tooltipCopy.mmceComplement$clearGasTooltip();
        for (String line : mmceComplement$gasTooltip) {
            tooltipCopy.mmceComplement$addGasTooltip(line);
        }
    }
}
