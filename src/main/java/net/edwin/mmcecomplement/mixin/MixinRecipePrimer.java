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
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.edwin.mmcecomplement.catalyst.RequirementFluidCatalyst;
import net.edwin.mmcecomplement.fluid.FluidModifierRequirement;
import net.edwin.mmcecomplement.integration.crafttweaker.AdvancedFluidModifierCT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stanhebben.zenscript.annotations.ZenMethod;

@Mixin(value = RecipePrimer.class, remap = false)
public abstract class MixinRecipePrimer implements ModuleRecipeData {
    @Shadow public abstract void appendComponent(hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement<?, ?> c);
    @Shadow public abstract java.util.List<ComponentRequirement<?, ?>> getComponents();
    @Shadow private ComponentRequirement<?, ?> lastComponent;

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

    /** Adds a controller-aware modifier to the most recent fluid output. */
    @ZenMethod
    public RecipePrimer addFluidModifier(AdvancedFluidModifierCT modifier) {
        if (modifier == null) {
            crafttweaker.CraftTweakerAPI.logWarning(
                "[MMCE Complement] addFluidModifier requires a non-null "
                    + "modifier function!");
        } else if (lastComponent instanceof hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid
            && lastComponent.getActionType()
                == hellfirepvp.modularmachinery.common.machine.IOType.OUTPUT) {
            ((FluidModifierRequirement) (Object) lastComponent)
                .mmceComplement$addFluidModifier((controller, stack) -> {
                    ILiquidStack modified = modifier.apply(
                        controller,
                        CraftTweakerMC.getILiquidStack(stack));
                    return CraftTweakerMC.getLiquidStack(modified);
                });
        } else {
            crafttweaker.CraftTweakerAPI.logWarning(
                "[MMCE Complement] addFluidModifier(AdvancedFluidModifier) "
                    + "can only be applied to a fluid output!");
        }
        return (RecipePrimer) (Object) this;
    }

    /** Applies display-only NBT to the most recent fluid component. */
    @Unique
    private void mmceComplement$applyPreviewNBT(crafttweaker.api.data.IData data) {
        NBTTagCompound tag = CraftTweakerMC.getNBTCompound(data);
        if (lastComponent instanceof
            hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid) {
            ((hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid)
                lastComponent).setDisplayNBTTag(tag);
        } else {
            crafttweaker.CraftTweakerAPI.logWarning(
                "[MMCE Complement] setPreViewNBT(IData) can only be applied "
                    + "to a fluid component (or an item component via MMCE).");
        }
    }

    /** Extends MMCE's canonical `setPreViewNBT` method to fluid components. */
    @Inject(method = "setPreViewNBT", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$setPreviewNBT(
        crafttweaker.api.data.IData data,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<RecipePrimer> cir) {
        if (lastComponent instanceof
            hellfirepvp.modularmachinery.common.crafting.requirement.RequirementFluid) {
            mmceComplement$applyPreviewNBT(data);
            cir.setReturnValue((RecipePrimer) (Object) this);
        }
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
