package net.edwin.mmcecomplement.mixin;

import crafttweaker.api.item.IIngredient;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import mekanism.api.gas.GasStack;
import mekanism.common.integration.crafttweaker.gas.IGasStack;
import net.edwin.mmcecomplement.catalyst.RequirementGasCatalyst;
import net.edwin.mmcecomplement.gas.GasModifierRequirement;
import net.edwin.mmcecomplement.integration.crafttweaker.AdvancedGasModifierCT;
import net.edwin.mmcecomplement.preview.GasTooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import stanhebben.zenscript.annotations.ZenMethod;

/** Mekanism-only RecipePrimer additions. This mixin is loaded conditionally. */
@Mixin(value = RecipePrimer.class, remap = false)
public abstract class MixinRecipePrimerMekanism {

    @Shadow public abstract void appendComponent(ComponentRequirement<?, ?> component);
    @Shadow private ComponentRequirement<?, ?> lastComponent;

    @ZenMethod
    public RecipePrimer addGasCatalystInput(IGasStack gas, String[] tips,
                                             RecipeModifier[] mods) {
        if (gas != null && gas.getInternal() instanceof GasStack) {
            RequirementGasCatalyst catalyst =
                new RequirementGasCatalyst((GasStack) gas.getInternal());
            if (tips != null) for (String tip : tips) catalyst.addTooltip(tip);
            if (mods != null) for (RecipeModifier mod : mods) catalyst.addModifier(mod);
            appendComponent(catalyst);
        }
        return (RecipePrimer) (Object) this;
    }

    /** Adds a controller-aware modifier to the most recent gas output. */
    @ZenMethod
    public RecipePrimer addGasModifier(AdvancedGasModifierCT modifier) {
        if (modifier == null) {
            crafttweaker.CraftTweakerAPI.logWarning(
                "[MMCE Complement] addGasModifier requires a non-null modifier function!");
        } else if (lastComponent instanceof
            hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas
            && lastComponent.getActionType() == IOType.OUTPUT) {
            ((GasModifierRequirement) (Object) lastComponent)
                .mmceComplement$addGasModifier((controller, stack) -> {
                    IIngredient modified = modifier.apply(controller,
                        new mekanism.common.integration.crafttweaker.gas.CraftTweakerGasStack(stack));
                    if (modified == null || !(modified.getInternal() instanceof GasStack)) {
                        return null;
                    }
                    return ((GasStack) modified.getInternal()).copy();
                });
        } else {
            crafttweaker.CraftTweakerAPI.logWarning(
                "[MMCE Complement] addGasModifier(AdvancedGasModifier) can only be applied to a gas output!");
        }
        return (RecipePrimer) (Object) this;
    }

    @ZenMethod
    public RecipePrimer addGasTooltip(String... lines) {
        if (lastComponent instanceof
            hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas
            && lastComponent instanceof GasTooltipData) {
            GasTooltipData tooltip = (GasTooltipData) (Object) lastComponent;
            if (lines != null) {
                for (String line : lines) tooltip.mmceComplement$addGasTooltip(line);
            }
        } else {
            crafttweaker.CraftTweakerAPI.logWarning(
                "[MMCE Complement] addGasTooltip(String...) can only be applied to a gas component.");
        }
        return (RecipePrimer) (Object) this;
    }

    @ZenMethod
    public RecipePrimer setGasTooltip(String... lines) {
        if (lastComponent instanceof
            hellfirepvp.modularmachinery.common.crafting.requirement.RequirementGas
            && lastComponent instanceof GasTooltipData) {
            GasTooltipData tooltip = (GasTooltipData) (Object) lastComponent;
            tooltip.mmceComplement$clearGasTooltip();
            return addGasTooltip(lines);
        }
        crafttweaker.CraftTweakerAPI.logWarning(
            "[MMCE Complement] setGasTooltip(String...) can only be applied to a gas component.");
        return (RecipePrimer) (Object) this;
    }
}
