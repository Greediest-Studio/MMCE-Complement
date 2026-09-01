package net.edwin.mmcecomplement.mixin;

import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.LinkedHashSet;
import java.util.Set;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import mekanism.common.integration.crafttweaker.gas.IGasStack;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.edwin.mmcecomplement.catalyst.RequirementFluidCatalyst;
import net.edwin.mmcecomplement.catalyst.RequirementGasCatalyst;
import net.minecraftforge.fluids.FluidStack;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fml.common.Optional;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stanhebben.zenscript.annotations.ZenMethod;

@Mixin(value = RecipePrimer.class, remap = false)
public abstract class MixinRecipePrimer implements ModuleRecipeData {
    @Shadow public abstract void appendComponent(hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement<?, ?> c);
    @Shadow public abstract java.util.List<ComponentRequirement<?, ?>> getComponents();

    @Unique
    private int mmceComplement$maxCatalyst = Integer.MAX_VALUE;

    /** Limits how many catalyst requirements may activate and be consumed per run. */
    @ZenMethod
    public RecipePrimer setMaxCatalyst(int maxCatalyst) {
        mmceComplement$maxCatalyst = Math.max(0, maxCatalyst);
        for (ComponentRequirement<?, ?> component : getComponents()) {
            if (component instanceof net.edwin.mmcecomplement.catalyst.CatalystLimitAware) {
                ((net.edwin.mmcecomplement.catalyst.CatalystLimitAware) component).setMaxCatalyst(mmceComplement$maxCatalyst);
            }
        }
        return (RecipePrimer) (Object) this;
    }

    @Inject(method = "appendComponent", at = @At("RETURN"), remap = false)
    private void mmceComplement$propagateCatalystLimit(ComponentRequirement<?, ?> component, CallbackInfo ci) {
        if (component instanceof net.edwin.mmcecomplement.catalyst.CatalystLimitAware) {
            ((net.edwin.mmcecomplement.catalyst.CatalystLimitAware) component).setMaxCatalyst(mmceComplement$maxCatalyst);
        }
    }
    @ZenMethod public RecipePrimer addFluidCatalystInput(ILiquidStack liquid, String[] tips, RecipeModifier[] mods) {
        FluidStack stack = CraftTweakerMC.getLiquidStack(liquid);
        if (stack != null) {
            RequirementFluidCatalyst c = new RequirementFluidCatalyst(stack);
            if (tips != null) for (String tip : tips) c.addTooltip(tip);
            if (mods != null) for (RecipeModifier mod : mods) c.addModifier(mod);
            appendComponent(c);
        }
        return (RecipePrimer) (Object) this;
    }

    @ZenMethod
    @Optional.Method(modid = "mekanism")
    public RecipePrimer addGasCatalystInput(IGasStack gas, String[] tips, RecipeModifier[] mods) {
        if (gas != null && gas.getInternal() instanceof GasStack) {
            RequirementGasCatalyst c = new RequirementGasCatalyst((GasStack) gas.getInternal());
            if (tips != null) for (String tip : tips) c.addTooltip(tip);
            if (mods != null) for (RecipeModifier mod : mods) c.addModifier(mod);
            appendComponent(c);
        }
        return (RecipePrimer) (Object) this;
    }

    @Unique
    private final Set<String> mmceComplement$requiredModules = new LinkedHashSet<>();

    @Unique
    private final Set<String> mmceComplement$forbiddenModules = new LinkedHashSet<>();

    @Override
    public Set<String> mmceComplement$getRequiredModules() {
        return mmceComplement$requiredModules;
    }

    @Override
    public Set<String> mmceComplement$getForbiddenModules() {
        return mmceComplement$forbiddenModules;
    }
}
