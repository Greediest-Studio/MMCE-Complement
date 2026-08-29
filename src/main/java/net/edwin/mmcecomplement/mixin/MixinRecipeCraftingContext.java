package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.helper.RequirementComponents;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementInterfaceNumInput;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.batch.BatchController;
import net.edwin.mmcecomplement.cycle.CycleComponentHandler;
import net.edwin.mmcecomplement.cycle.CycleRuntime;
import net.edwin.mmcecomplement.mechannel.MEChannelReservationLifecycle;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
public abstract class MixinRecipeCraftingContext {

    @Shadow
    private TileMultiblockMachineController controller;

    @Shadow
    private Map<Long, Collection<ProcessingComponent<?>>> typeComponents;

    @Shadow
    private Map<Long, List<RequirementComponents>> requirementComponents;

    @Inject(method = "startCrafting(J)V", at = @At("HEAD"))
    private void mmceComplement$beginSelfCycle(long seed, CallbackInfo ci) {
        CycleRuntime.begin((RecipeCraftingContext) (Object) this);
    }

    @Inject(method = "finishCrafting(J)V", at = @At("RETURN"))
    private void mmceComplement$clearSelfCycle(long seed, CallbackInfo ci) {
        CycleRuntime.clear((RecipeCraftingContext) (Object) this);
        MEChannelReservationLifecycle.release(
            (RecipeCraftingContext) (Object) this);
    }

    @Inject(method = {"reset", "resetAll"}, at = @At("HEAD"))
    private void mmceComplement$clearResetSelfCycle(
        CallbackInfoReturnable<RecipeCraftingContext> cir) {
        CycleRuntime.clear((RecipeCraftingContext) (Object) this);
        MEChannelReservationLifecycle.release(
            (RecipeCraftingContext) (Object) this);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void mmceComplement$clearDestroyedSelfCycle(CallbackInfo ci) {
        CycleRuntime.clear((RecipeCraftingContext) (Object) this);
        MEChannelReservationLifecycle.release(
            (RecipeCraftingContext) (Object) this);
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
        for (Map.Entry<Long, Map<TileEntity, ProcessingComponent<?>>> entry
            : components.entrySet()) {
            List<ProcessingComponent<?>> expanded = new ArrayList<>(
                typeComponents.get(entry.getKey()));
            for (Map.Entry<TileEntity, ProcessingComponent<?>> componentEntry
                : entry.getValue().entrySet()) {
                if (componentEntry.getKey()
                    instanceof TileSelfCycleAssemblyHatch) {
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
                    expanded.add(new ProcessingComponent<>(
                        hatch.getFluidProvider(), hatch,
                        componentEntry.getValue().getTag()));
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
                    expanded.add(new ProcessingComponent<>(provider, provider,
                        componentEntry.getValue().getTag()));
                }
                expanded.add(new ProcessingComponent<>(
                    hatch.getFluidProvider(), hatch,
                    componentEntry.getValue().getTag()));
            }
            typeComponents.put(entry.getKey(), expanded);
        }
    }

    /**
     * Strong priority: within a recipe group, the presence of any combined
     * data-item hatch makes ordinary smart interfaces ineligible for numeric
     * data requirements. Multiple combined hatches in that same group remain
     * valid alternatives to each other.
     */
    @Inject(method = "updateRequirementComponents", at = @At("RETURN"))
    private void mmceComplement$prioritizeDataItemComponents(CallbackInfo ci) {
        for (List<RequirementComponents> group : requirementComponents.values()) {
            for (RequirementComponents entry : group) {
                entry.components().sort((left, right) -> {
                    int leftPriority = cyclePriority(left);
                    int rightPriority = cyclePriority(right);
                    return Integer.compare(leftPriority, rightPriority);
                });
                if (!(entry.requirement()
                    instanceof RequirementInterfaceNumInput)) {
                    continue;
                }
                boolean hasPriorityHatch = false;
                for (ProcessingComponent<?> component : entry.components()) {
                    if (component.getProvidedComponent()
                        instanceof TileDataItemInputHatch.DataItemInterfaceProvider) {
                        hasPriorityHatch = true;
                        break;
                    }
                }
                if (hasPriorityHatch) {
                    entry.components().removeIf(component ->
                        !(component.getProvidedComponent()
                            instanceof TileDataItemInputHatch.DataItemInterfaceProvider));
                }
            }
        }
    }

    private static int cyclePriority(ProcessingComponent<?> component) {
        Object provided = component.getProvidedComponent();
        if (!(provided instanceof CycleComponentHandler)) return 1;
        return 0;
    }
}
