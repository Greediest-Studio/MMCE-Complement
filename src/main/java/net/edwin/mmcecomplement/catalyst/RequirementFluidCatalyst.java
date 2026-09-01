package net.edwin.mmcecomplement.catalyst;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.Asyncable;
import hellfirepvp.modularmachinery.common.util.HybridFluidUtils;
import net.minecraftforge.fluids.capability.IFluidHandler;
import javax.annotation.Nonnull;
import java.util.Collections;

public class RequirementFluidCatalyst extends ComponentRequirement.MultiCompParallelizable<Object, RequirementTypeFluidCatalyst> implements ComponentRequirement.ChancedRequirement, Asyncable, CatalystLimitAware {
    public final FluidStack required;
    public float chance = 1F;
    private final List<RecipeModifier> modifiers = new ArrayList<>();
    private final List<String> tooltips = new ArrayList<>();
    private static final Object MATCH_KEY = new Object();
    private boolean catalystRequired;
    private int maxCatalyst = Integer.MAX_VALUE;
    public RequirementFluidCatalyst(FluidStack stack) { super(CatalystTypes.FLUID, IOType.INPUT); required = stack.copy(); setParallelizeUnaffected(true); }
    public void addModifier(RecipeModifier modifier) { if (modifier != null) modifiers.add(modifier); }
    public void addTooltip(String tooltip) { if (tooltip != null) tooltips.add(tooltip); }
    public List<String> getToolTipList() { return tooltips; }
    public void setMaxCatalyst(int max) { maxCatalyst = Math.max(0, max); }
    public int getMaxCatalyst() { return maxCatalyst; }
    public int getSortingWeight() { return PRIORITY_WEIGHT_FLUID; }
    public Object getComponentMatchCacheKey() { return MATCH_KEY; }
    @Nonnull @Override public CraftCheck canStartCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context) {
        CraftCheck check = doFluidIO(components, context);
        if (check.isSuccess() && !catalystRequired && CatalystRuntime.tryAcquire(context, maxCatalyst)) { addModifierToContext(context); catalystRequired = true; return CraftCheck.success(); }
        catalystRequired = false; return CraftCheck.skipComponent();
    }
    @Override public int getMaxParallelism(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int max) {
        if (doFluidIOInternal(components, context, 1) >= 1 && !catalystRequired && CatalystRuntime.tryAcquire(context, maxCatalyst)) { addModifierToContext(context); catalystRequired = true; }
        else catalystRequired = false;
        return max;
    }
    private void addModifierToContext(RecipeCraftingContext context) { for (RecipeModifier m : modifiers) context.addPermanentModifier(parallelism > 1 ? m.multiply(parallelism) : m); }
    @Override public void startCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) {
        if (catalystRequired) { if (chance.canWork(RecipeModifier.applyModifiers(context, RequirementTypesMM.REQUIREMENT_FLUID, actionType, this.chance, true))) doFluidIO(components, context); catalystRequired = false; }
    }
    @Override public RequirementFluidCatalyst deepCopy() { return deepCopyModified(Collections.emptyList()); }
    @Override public RequirementFluidCatalyst deepCopyModified(List<RecipeModifier> incoming) {
        FluidStack copied = required.copy(); copied.amount = Math.max(1, Math.round(RecipeModifier.applyModifiers(incoming, RequirementTypesMM.REQUIREMENT_FLUID, actionType, copied.amount, false)));
        RequirementFluidCatalyst result = new RequirementFluidCatalyst(copied); result.modifiers.addAll(modifiers); result.tooltips.addAll(tooltips); result.maxCatalyst = maxCatalyst; result.chance = RecipeModifier.applyModifiers(incoming, RequirementTypesMM.REQUIREMENT_FLUID, actionType, chance, true); return result;
    }
    @Override public void setChance(float chance) { this.chance = chance; }
    @Nonnull @Override public String getMissingComponentErrorMessage(IOType ioType) { return "component.missing.mmce_complement.fluid_catalyst." + ioType.name().toLowerCase(); }
    @Override public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        MachineComponent<?> cmp = component.component(); ComponentType type = cmp.getComponentType();
        return (type.equals(ComponentTypesMM.COMPONENT_FLUID) || type.equals(ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS)) && cmp.ioType == actionType;
    }
    public void finishCrafting(List<ProcessingComponent<?>> components, RecipeCraftingContext context, ResultChance chance) { }
    @Override public List<ProcessingComponent<?>> copyComponents(List<ProcessingComponent<?>> components) { return HybridFluidUtils.copyFluidHandlerComponents(components); }
    private CraftCheck doFluidIO(List<ProcessingComponent<?>> components, RecipeCraftingContext context) { return doFluidIOInternal(components, context, parallelism) >= parallelism ? CraftCheck.success() : CraftCheck.failure("craftcheck.failure.fluid.input"); }
    private int doFluidIOInternal(List<ProcessingComponent<?>> components, RecipeCraftingContext context, int max) {
        List<IFluidHandler> handlers = HybridFluidUtils.castFluidHandlerComponents(components);
        long amount = Math.max(1L, Math.round(RecipeModifier.applyModifiers(context, RequirementTypesMM.REQUIREMENT_FLUID, actionType, (double) required.amount, false)));
        long total = HybridFluidUtils.doSimulateDrainOrFill(required.copy(), handlers, amount * max, actionType);
        if (total < amount) return 0;
        HybridFluidUtils.doDrainOrFill(required.copy(), total, handlers, actionType); return (int) (total / amount);
    }
    @Override public ComponentRequirement.JEIComponent provideJEIComponent() { return new JEIComponentFluidCatalyst(this); }
}
