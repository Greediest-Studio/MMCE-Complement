package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.preview.PreviewNBTData;
import net.edwin.mmcecomplement.preview.GasTooltipData;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

/** Adds display-only preview NBT to MMCE's Mekanism gas requirement. */
@Pseudo
@Mixin(targets =
    "hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas",
    remap = false)
public abstract class MixinRequirementGasPreviewNBT
    implements PreviewNBTData, GasTooltipData {

    @Unique
    private NBTTagCompound mmceComplement$previewNBT;

    @Unique
    private final List<String> mmceComplement$gasTooltip = new ArrayList<>();

    @Override
    public void mmceComplement$setPreviewNBT(NBTTagCompound tag) {
        mmceComplement$previewNBT = tag == null ? null : tag.copy();
    }

    @Override
    public NBTTagCompound mmceComplement$getPreviewNBT() {
        return mmceComplement$previewNBT == null
            ? null : mmceComplement$previewNBT.copy();
    }

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
    private void mmceComplement$copyPreviewNBT(
        List<?> modifiers, CallbackInfoReturnable<RequirementGas> cir) {
        PreviewNBTData copy = (PreviewNBTData) (Object) cir.getReturnValue();
        copy.mmceComplement$setPreviewNBT(mmceComplement$previewNBT);
        GasTooltipData tooltipCopy = (GasTooltipData) (Object) cir.getReturnValue();
        tooltipCopy.mmceComplement$clearGasTooltip();
        for (String line : mmceComplement$gasTooltip) {
            tooltipCopy.mmceComplement$addGasTooltip(line);
        }
    }
}
