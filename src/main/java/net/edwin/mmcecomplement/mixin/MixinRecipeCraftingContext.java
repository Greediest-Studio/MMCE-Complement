package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.helper.RequirementComponents;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementInterfaceNumInput;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.edwin.mmcecomplement.batch.BatchController;
import net.edwin.mmcecomplement.cycle.CycleComponentHandler;
import net.edwin.mmcecomplement.cycle.CycleRuntime;
import net.edwin.mmcecomplement.catalyst.CatalystRuntime;
import net.edwin.mmcecomplement.mechannel.MEChannelReservationLifecycle;
import net.edwin.mmcecomplement.mechannel.RequirementMEChannel;
import net.edwin.mmcecomplement.compat.ae.tile.IMEDataPatternProvider;
import net.edwin.mmcecomplement.tile.PrioritySmartInterfaceProvider;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Allows a Batch Hatch to batch recipes that did not opt into normal parallelism. */
@Mixin(value = RecipeCraftingContext.class, remap = false)
public abstract class MixinRecipeCraftingContext implements CatalystRuntime.Context {

    @Unique private int mmceComplement$catalystsUsed;
    @Unique private boolean mmceComplement$hasCycleComponents;
    @Unique private boolean mmceComplement$hasMEChannelRequirements;
    @Unique private boolean mmceComplement$cycleStateActive;
    @Unique private boolean mmceComplement$channelCleanupPending;

    @Override
    public boolean tryAcquireCatalyst(int max) {
        if (max <= 0 || mmceComplement$catalystsUsed >= max) return false;
        mmceComplement$catalystsUsed++;
        return true;
    }

    @Override
    public void releaseCatalyst() {
        if (mmceComplement$catalystsUsed > 0) mmceComplement$catalystsUsed--;
    }

    @Inject(method = {
        "canStartCrafting()Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext$CraftingCheckResult;",
        "canRestartCrafting"
    }, at = @At("HEAD"))
    private void mmceComplement$beginCatalystSelection(
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        mmceComplement$catalystsUsed = 0;
    }

    @Shadow
    private TileMultiblockMachineController controller;

    @Shadow
    private Map<Long, Collection<ProcessingComponent<?>>> typeComponents;

    @Shadow
    private Map<Long, List<RequirementComponents>> requirementComponents;

    @Inject(method = "startCrafting(J)V", at = @At("HEAD"))
    private void mmceComplement$beginSelfCycle(long seed, CallbackInfo ci) {
        if (mmceComplement$hasCycleComponents) {
            CycleRuntime.begin((RecipeCraftingContext) (Object) this);
            mmceComplement$cycleStateActive = true;
        }
        if (mmceComplement$hasMEChannelRequirements) {
            mmceComplement$channelCleanupPending = true;
        }
    }

    @Inject(method = "finishCrafting(J)V", at = @At("RETURN"))
    private void mmceComplement$clearSelfCycle(long seed, CallbackInfo ci) {
        mmceComplement$clearExtensionState();
    }

    @Inject(method = {"reset", "resetAll"}, at = @At("HEAD"))
    private void mmceComplement$clearResetSelfCycle(
        CallbackInfoReturnable<RecipeCraftingContext> cir) {
        mmceComplement$catalystsUsed = 0;
        mmceComplement$clearExtensionState();
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void mmceComplement$clearDestroyedSelfCycle(CallbackInfo ci) {
        mmceComplement$clearExtensionState();
    }

    @Unique
    private void mmceComplement$clearExtensionState() {
        RecipeCraftingContext context = (RecipeCraftingContext) (Object) this;
        if (mmceComplement$cycleStateActive) {
            CycleRuntime.clear(context);
            mmceComplement$cycleStateActive = false;
        }
        if (mmceComplement$channelCleanupPending) {
            MEChannelReservationLifecycle.release(context);
            mmceComplement$channelCleanupPending = false;
        }
    }

    @Redirect(
        method = "canStartCrafting()Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext$CraftingCheckResult;",
        at = @At(value = "INVOKE",
            target = "Lhellfirepvp/modularmachinery/common/crafting/MachineRecipe;isParallelized()Z"))
    private boolean mmceComplement$enableBatchParallelism(MachineRecipe recipe) {
        return recipe.isParallelized()
            || controller instanceof BatchController
            && ((BatchController) controller).mmceComplement$getMaxBatchTime() > 0;
    }

    /**
     * MMCE stores components in a map keyed by TileEntity, so two components
     * from the same block and recipe group would overwrite one another. Add
     * the smart-interface component directly to the context beside the item
     * component instead.
     */
    @Inject(
        method = "updateComponents",
        at = @At(value = "INVOKE",
            target = "Lhellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext;updateRequirementComponents()V"))
    private void mmceComplement$addDataItemComponents(
        Map<Long, Map<TileEntity, ProcessingComponent<?>>> components,
        CallbackInfo ci) {
        boolean hasCycleComponents = false;
        for (Map.Entry<Long, Map<TileEntity, ProcessingComponent<?>>> entry
            : components.entrySet()) {
            Collection<ProcessingComponent<?>> original =
                typeComponents.get(entry.getKey());
            List<ProcessingComponent<?>> expanded = null;
            for (Map.Entry<TileEntity, ProcessingComponent<?>> componentEntry
                : entry.getValue().entrySet()) {
                if (componentEntry.getKey()
                    instanceof TileSelfCycleAssemblyHatch) {
                    hasCycleComponents = true;
                    expanded = mmceComplement$copyForExpansion(
                        expanded, original);
                    TileSelfCycleAssemblyHatch hatch =
                        (TileSelfCycleAssemblyHatch) componentEntry.getKey();
                    expanded.add(new ProcessingComponent<>(
                        hatch.getItemOutputProvider(),
                        hatch.getItemOutputProvider().getContainerProvider(),
                        componentEntry.getValue().getTag()));
                    expanded.add(new ProcessingComponent<>(
                        hatch.getFluidProvider(),
                        hatch.getFluidProvider().getContainerProvider(),
                        componentEntry.getValue().getTag()));
                    expanded.add(new ProcessingComponent<>(
                        hatch.getFluidOutputProvider(),
                        hatch.getFluidOutputProvider().getContainerProvider(),
                        componentEntry.getValue().getTag()));
                    continue;
                }
                if (componentEntry.getKey() instanceof TileItemOutputAssemblyHatch) {
                    TileItemOutputAssemblyHatch hatch =
                        (TileItemOutputAssemblyHatch) componentEntry.getKey();
                    expanded = mmceComplement$copyForExpansion(
                        expanded, original);
                    expanded.add(new ProcessingComponent<>(
                        hatch.getFluidProvider(), hatch,
                        componentEntry.getValue().getTag()));
                    continue;
                }
                if (componentEntry.getKey()
                    instanceof IMEDataPatternProvider) {
                    IMEDataPatternProvider provider =
                        (IMEDataPatternProvider) componentEntry.getKey();
                    TileSmartInterface.SmartInterfaceProvider primary =
                        provider.getPrimaryDataProvider();
                    String registeredType = controller
                        .getFoundSmartInterfaces().get(primary);
                    SmartInterfaceData binding = primary.getMachineData(
                        controller.getPos());
                    if (registeredType == null || binding == null
                        || !registeredType.equals(binding.getType())) {
                        /*
                         * boundData can retain a pending association so the
                         * GUI can offer this controller's interface classes.
                         * It must not become a recipe component until MMCE's
                         * per-controller uniqueness check has registered the
                         * physical primary provider.
                         */
                        continue;
                    }
                    if (!provider.isDataProviderActiveForGroup(
                        entry.getKey())) {
                        // Isolation wrappers are registered for every pattern group
                        // by MMCE.  An unsent slot must not become a numeric
                        // data candidate or suppress ordinary interfaces.
                        continue;
                    }
                    TileSmartInterface.SmartInterfaceProvider data =
                        provider.getDataProviderForGroup(entry.getKey());
                    if (data != null) {
                        expanded = mmceComplement$copyForExpansion(
                            expanded, original);
                        expanded.add(new ProcessingComponent<>(data, data,
                            componentEntry.getValue().getTag()));
                    }
                    continue;
                }
                if (!(componentEntry.getKey()
                    instanceof TileDataItemInputHatch)) {
                    continue;
                }
                TileDataItemInputHatch hatch =
                    (TileDataItemInputHatch) componentEntry.getKey();
                TileDataItemInputHatch.DataItemInterfaceProvider provider =
                    hatch.getDataProvider();
                // The plain item input assembly intentionally has no smart
                // data channel, but it still exposes the hybrid fluid tanks.
                // Do not let that marker suppress registration of its fluid
                // component when an item and fluid requirement share a hatch.
                if (!(componentEntry.getKey() instanceof TileItemInputAssemblyHatch)) {
                    expanded = mmceComplement$copyForExpansion(
                        expanded, original);
                    expanded.add(new ProcessingComponent<>(provider, provider,
                        componentEntry.getValue().getTag()));
                }
                expanded = mmceComplement$copyForExpansion(expanded, original);
                expanded.add(new ProcessingComponent<>(
                    hatch.getFluidProvider(), hatch,
                    componentEntry.getValue().getTag()));
            }
            if (expanded != null) {
                typeComponents.put(entry.getKey(), expanded);
            }
        }
        mmceComplement$hasCycleComponents = hasCycleComponents;
    }

    /**
     * Strong priority: within a recipe group, the presence of a combined data
     * component makes ordinary smart interfaces ineligible for numeric data
     * requirements. Multiple priority components in that group remain valid
     * alternatives to each other.
     */
    @Inject(method = "updateRequirementComponents", at = @At("RETURN"))
    private void mmceComplement$prioritizeDataItemComponents(CallbackInfo ci) {
        boolean hasMEChannelRequirements = false;
        for (List<RequirementComponents> group : requirementComponents.values()) {
            for (RequirementComponents entry : group) {
                if (entry.requirement() instanceof RequirementMEChannel) {
                    hasMEChannelRequirements = true;
                }
                boolean hasCycleComponent = false;
                boolean hasOrdinaryComponent = false;
                for (ProcessingComponent<?> component : entry.components()) {
                    if (component.getProvidedComponent()
                        instanceof CycleComponentHandler) {
                        hasCycleComponent = true;
                    } else {
                        hasOrdinaryComponent = true;
                    }
                    if (hasCycleComponent && hasOrdinaryComponent) {
                        break;
                    }
                }
                if (hasCycleComponent && hasOrdinaryComponent) {
                    entry.components().sort((left, right) ->
                        Integer.compare(cyclePriority(left),
                            cyclePriority(right)));
                }
                if (!(entry.requirement()
                    instanceof RequirementInterfaceNumInput)) {
                    continue;
                }
                boolean hasPriorityHatch = false;
                for (ProcessingComponent<?> component : entry.components()) {
                    if (component.getProvidedComponent()
                        instanceof PrioritySmartInterfaceProvider
                        && ((PrioritySmartInterfaceProvider) component
                            .getProvidedComponent()).isPriorityActive()) {
                        hasPriorityHatch = true;
                        break;
                    }
                }
                if (hasPriorityHatch) {
                    entry.components().removeIf(component ->
                        !(component.getProvidedComponent()
                            instanceof PrioritySmartInterfaceProvider));
                }
            }
        }
        mmceComplement$hasMEChannelRequirements = hasMEChannelRequirements;
    }

    @Unique
    private static List<ProcessingComponent<?>> mmceComplement$copyForExpansion(
        List<ProcessingComponent<?>> expanded,
        Collection<ProcessingComponent<?>> original) {
        if (expanded != null) {
            return expanded;
        }
        return original == null ? new ArrayList<>()
            : new ArrayList<>(original);
    }

    private static int cyclePriority(ProcessingComponent<?> component) {
        Object provided = component.getProvidedComponent();
        if (!(provided instanceof CycleComponentHandler)) return 1;
        return 0;
    }
}
