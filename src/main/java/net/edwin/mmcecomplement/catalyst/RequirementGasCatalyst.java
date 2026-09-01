package net.edwin.mmcecomplement.catalyst;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import mekanism.api.gas.GasStack;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequirementGasCatalyst extends ComponentRequirement.MultiCompParallelizable<Object, RequirementTypeGasCatalyst> implements ComponentRequirement.ChancedRequirement, Asyncable, CatalystLimitAware {
    public final GasStack required;
    public float chance = 1F;
    private final List<RecipeModifier> modifiers = new ArrayList<>();
    private final List<String> tooltips = new ArrayList<>();
    private static final Object MATCH_KEY = new Object();
    private boolean catalystRequired;
    private int maxCatalyst = Integer.MAX_VALUE;
    public RequirementGasCatalyst(GasStack stack) { super(CatalystTypes.GAS, IOType.INPUT); required = stack.copy(); setParallelizeUnaffected(true); }
    public void addModifier(RecipeModifier modifier) { if (modifier != null) modifiers.add(modifier); }
    public void addTooltip(String tooltip) { if (tooltip != null) tooltips.add(tooltip); }
    public List<String> getToolTipList() { return tooltips; }
    public void setMaxCatalyst(int max) { maxCatalyst = Math.max(0, max); }
    public int getMaxCatalyst() { return maxCatalyst; }
    public int getSortingWeight() { return PRIORITY_WEIGHT_FLUID; }
    public Object getComponentMatchCacheKey() { return MATCH_KEY; }
    @Nonnull @Override public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        CraftCheck check = doGasIO(components, context);
        if (check.isSuccess() && !catalystRequired && CatalystRuntime.tryAcquire(context, maxCatalyst)) { addModifierToContext(context); catalystRequired = true; return CraftCheck.success(); }
        catalystRequired = false; return CraftCheck.skipComponent();
    }
    @Override public int getMaxParallelism(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int max) {
        if (doGasIOInternal(components, context, 1) >= 1 && !catalystRequired && CatalystRuntime.tryAcquire(context, maxCatalyst)) { addModifierToContext(context); catalystRequired = true; }
        else catalystRequired = false;
        return max;
    }
    private void addModifierToContext(RecipeCraftingContext context) { for (RecipeModifier m : modifiers) context.addPermanentModifier(parallelism > 1 ? m.multiply(parallelism) : m); }
    @Override public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        if (catalystRequired) { if (chance.canWork(RecipeModifier.applyModifiers(context, RequirementTypesMM.REQUIREMENT_GAS, actionType, this.chance, true))) doGasIO(components, context); catalystRequired = false; }
    }
    @Override public RequirementGasCatalyst deepCopy() { return deepCopyModified(Collections.emptyList()); }
    @Override public RequirementGasCatalyst deepCopyModified(List<RecipeModifier> incoming) {
        GasStack copied = required.copy(); copied.amount = Math.max(1, Math.round(RecipeModifier.applyModifiers(incoming, RequirementTypesMM.REQUIREMENT_GAS, actionType, copied.amount, false)));
        RequirementGasCatalyst result = new RequirementGasCatalyst(copied); result.modifiers.addAll(modifiers); result.tooltips.addAll(tooltips); result.maxCatalyst = maxCatalyst; result.chance = RecipeModifier.applyModifiers(incoming, RequirementTypesMM.REQUIREMENT_GAS, actionType, chance, true); return result;
    }
    @Override public void setChance(float chance) { this.chance = chance; }
    @Nonnull @Override public String getMissingComponentErrorMessage(IOType ioType) { return "component.missing.mmce_complement.gas_catalyst." + ioType.name().toLowerCase(); }
    @Override public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component(); ComponentType type = cmp.getComponentType();
        return (type.equals(ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS) || component.getProvidedComponent() instanceof IExtendedGasHandler) && cmp.ioType == actionType;
    }
    public void finishCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) { }
    @Override public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) { return HybridFluidUtils.copyGasHandlerComponents(components); }
    private CraftCheck doGasIO(List<ProcessingComponent<?>> components, RecipeCraftingContext context) { return doGasIOInternal(components, context, parallelism) >= parallelism ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.gas.input"); }
    private int doGasIOInternal(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int max) {
        List<IExtendedGasHandler> handlers = HybridFluidUtils.castGasHandlerComponents(components); long amount = Math.max(1L, Math.round(RecipeModifier.applyModifiers(context, RequirementTypesMM.REQUIREMENT_GAS, actionType, (double) required.amount, false)));
        long total = HybridFluidUtils.doSimulateDrainOrFill(required.copy(), handlers, amount * max, actionType); if (total < amount) return 0;
        HybridFluidUtils.doDrainOrFill(required.copy(), total, handlers, actionType); return (int) (total / amount);
    }
    @Override public ComponentRequirement.JEIComponent provideJEIComponent() { return new JEIComponentGasCatalyst(this); }
}
