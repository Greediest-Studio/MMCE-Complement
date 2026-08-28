package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.ModuleRecipeConditions;
import net.edwin.mmcecomplement.attachment.ModuleRecipeData;
import net.edwin.mmcecomplement.batch.BatchProcessingLogic;
import net.edwin.mmcecomplement.batch.BatchRecipeData;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;

@Mixin(value = ActiveMachineRecipe.class, remap = false)
public abstract class MixinActiveMachineRecipe implements BatchRecipeData {

    @Unique
    private static final String mmceComplement$NBT_BATCH_BASE_PARALLELISM =
        "mmceComplementBatchBaseParallelism";

    @Unique
    private static final String mmceComplement$NBT_BATCH_FACTOR =
        "mmceComplementBatchFactor";

    @Unique
    private static final String mmceComplement$NBT_BATCH_PREPARED =
        "mmceComplementBatchPrepared";

    @Shadow
    @Final
    private MachineRecipe recipe;

    @Shadow
    private int maxParallelism;

    @Shadow
    private int parallelism;

    @Unique
    private int mmceComplement$batchBaseMaxParallelism = 1;

    /** The portion of the budget which belongs to ordinary factory threads. */
    @Unique
    private int mmceComplement$batchEligibleParallelism = 1;

    /** Factory custom/extra threads are retained, but never multiplied. */
    @Unique
    private int mmceComplement$batchExcludedParallelism;

    @Unique
    private int mmceComplement$batchFactor = 1;

    /** Last maxParallelism value written after applying the Batch Hatch. */
    @Unique
    private int mmceComplement$batchOutputMaxParallelism = 1;

    /** Whether the previous calculation actually changed maxParallelism. */
    @Unique
    private boolean mmceComplement$batchParallelismApplied;

    /**
     * Set when MMCE, ZenScript, a parallel controller, or another addon uses
     * ActiveMachineRecipe#setMaxParallelism between batch calculations.
     */
    @Unique
    private boolean mmceComplement$maxParallelismExplicitlyUpdated;

    @Unique
    private boolean mmceComplement$batchPrepared;

    @Unique
    private boolean mmceComplement$batchCalculationActive;

    @Inject(method = "<init>(Lhellfirepvp/modularmachinery/common/crafting/MachineRecipe;I)V",
        at = @At("RETURN"))
    private void mmceComplement$initializeBatchData(MachineRecipe recipe,
                                                    int maxParallelism,
                                                    CallbackInfo ci) {
        // The constructor argument already contains MMCE's machine/controller
        // budget. Do not collapse non-parallel recipes to one here: a Batch
        // Hatch can opt them into parallel checks, and integrations may still
        // replace the value through setMaxParallelism before the check starts.
        mmceComplement$batchBaseMaxParallelism = Math.max(1, maxParallelism);
        mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
        mmceComplement$batchOutputMaxParallelism = this.maxParallelism;
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"))
    private void mmceComplement$readBatchData(NBTTagCompound compound, CallbackInfo ci) {
        if (compound.hasKey(mmceComplement$NBT_BATCH_BASE_PARALLELISM)) {
            mmceComplement$batchBaseMaxParallelism = Math.max(1,
                compound.getInteger(mmceComplement$NBT_BATCH_BASE_PARALLELISM));
            mmceComplement$batchFactor = Math.max(1,
                compound.getInteger(mmceComplement$NBT_BATCH_FACTOR));
            mmceComplement$batchPrepared =
                compound.getBoolean(mmceComplement$NBT_BATCH_PREPARED);
            mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
            mmceComplement$batchExcludedParallelism = 0;
        } else {
            mmceComplement$batchBaseMaxParallelism = Math.max(1, maxParallelism);
            mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
        }
        mmceComplement$batchOutputMaxParallelism = Math.max(1, maxParallelism);
        mmceComplement$batchParallelismApplied = mmceComplement$batchPrepared
            && mmceComplement$batchFactor > 1
            && mmceComplement$batchOutputMaxParallelism
                != mmceComplement$batchBaseMaxParallelism;
        mmceComplement$maxParallelismExplicitlyUpdated = false;
    }

    @Inject(method = "serialize", at = @At("RETURN"))
    private void mmceComplement$writeBatchData(
        CallbackInfoReturnable<NBTTagCompound> cir) {
        NBTTagCompound compound = cir.getReturnValue();
        compound.setInteger(mmceComplement$NBT_BATCH_BASE_PARALLELISM,
            mmceComplement$batchBaseMaxParallelism);
        compound.setInteger(mmceComplement$NBT_BATCH_FACTOR,
            mmceComplement$batchFactor);
        compound.setBoolean(mmceComplement$NBT_BATCH_PREPARED,
            mmceComplement$batchPrepared);
    }

    @Inject(method = "reset", at = @At("RETURN"))
    private void mmceComplement$resetBatchData(CallbackInfo ci) {
        // ActiveMachineRecipe.reset() intentionally resets maxParallelism to
        // one. Its owner then supplies the current machine/factory budget via
        // setMaxParallelism. Restoring a constructor-time cache here used to
        // overwrite runtime parallel controllers and script changes.
        mmceComplement$batchBaseMaxParallelism = Math.max(1, maxParallelism);
        mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
        mmceComplement$batchExcludedParallelism = 0;
        mmceComplement$batchFactor = 1;
        mmceComplement$batchOutputMaxParallelism = Math.max(1, maxParallelism);
        mmceComplement$batchParallelismApplied = false;
        mmceComplement$maxParallelismExplicitlyUpdated = false;
        mmceComplement$batchPrepared = false;
        mmceComplement$batchCalculationActive = false;
    }

    @Inject(method = "setMaxParallelism", at = @At("RETURN"))
    private void mmceComplement$observeParallelismOverride(
        int maxParallelism, CallbackInfo ci) {
        mmceComplement$maxParallelismExplicitlyUpdated = true;
    }

    @Inject(method = "calculateExtraParallelism", at = @At("HEAD"))
    private void mmceComplement$beginBatchCalculation(
        RecipeCraftingContext context, CallbackInfo ci) {
        int runtimeBase = BatchProcessingLogic.restoreUnbatchedParallelism(
            maxParallelism,
            mmceComplement$batchBaseMaxParallelism,
            mmceComplement$batchOutputMaxParallelism,
            mmceComplement$batchParallelismApplied,
            mmceComplement$maxParallelismExplicitlyUpdated);
        maxParallelism = runtimeBase;
        mmceComplement$batchBaseMaxParallelism = runtimeBase;
        mmceComplement$maxParallelismExplicitlyUpdated = false;
        mmceComplement$batchParallelismApplied = false;

        TileMultiblockMachineController controller = context.getMachineController();
        int maxBatchTime = controller instanceof net.edwin.mmcecomplement.batch.BatchController
            ? ((net.edwin.mmcecomplement.batch.BatchController) controller)
                .mmceComplement$getMaxBatchTime()
            : 0;
        if (maxBatchTime <= 0) {
            mmceComplement$batchCalculationActive = false;
            mmceComplement$batchPrepared = false;
            mmceComplement$batchFactor = 1;
            mmceComplement$batchExcludedParallelism = 0;
            mmceComplement$batchEligibleParallelism = runtimeBase;
            mmceComplement$batchOutputMaxParallelism = runtimeBase;
            return;
        }
        int baseParallelism = runtimeBase;
        int excludedParallelism = mmceComplement$getExcludedParallelism(controller,
            baseParallelism);
        mmceComplement$batchExcludedParallelism = excludedParallelism;
        mmceComplement$batchEligibleParallelism = Math.max(0,
            baseParallelism - excludedParallelism);
        mmceComplement$batchFactor = 1;
        mmceComplement$batchPrepared = false;
        mmceComplement$batchCalculationActive = true;
    }

    @Inject(method = "calculateExtraParallelism", at = @At("RETURN"))
    private void mmceComplement$finishBatchCalculation(
        RecipeCraftingContext context, CallbackInfo ci) {
        if (!mmceComplement$batchCalculationActive) {
            return;
        }
        // Treat the value produced by MMCE and every other mixin as
        // authoritative. Batch processing only adds its own factor on top;
        // it must never reconstruct the value from the constructor argument.
        int unbatchedRuntimeParallelism = Math.max(1, maxParallelism);
        int batchedRuntimeParallelism = mmceComplement$batchFactor <= 1
            ? unbatchedRuntimeParallelism
            : BatchProcessingLogic.multiplyParallelismExcluding(
                unbatchedRuntimeParallelism,
                mmceComplement$batchExcludedParallelism,
                mmceComplement$batchFactor);
        maxParallelism = batchedRuntimeParallelism;
        mmceComplement$batchOutputMaxParallelism = batchedRuntimeParallelism;
        mmceComplement$batchParallelismApplied =
            batchedRuntimeParallelism != unbatchedRuntimeParallelism;
        mmceComplement$finishBatchCalculation();
    }

    /**
     * Enforce attachment conditions at the actual processing boundary.  The
     * recipe search runs asynchronously and must not inspect controller state;
     * this method is called from the server thread immediately before the
     * recipe performs its per-tick IO, so a stale/unsupported recipe can never
     * consume inputs or produce outputs.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesWhileRunning(
        TileMultiblockMachineController controller,
        RecipeCraftingContext context,
        CallbackInfoReturnable<CraftingStatus> cir) {
        if (!(recipe instanceof ModuleRecipeData)) {
            return;
        }
        ModuleRecipeData moduleRecipe = (ModuleRecipeData) (Object) recipe;
        if (!ModuleRecipeConditions.hasRestrictions(moduleRecipe)) {
            return;
        }
        Collection<String> activeModules = controller instanceof AttachmentController
            ? ((AttachmentController) controller)
                .mmceComplement$getActiveAttachmentModules()
            : Collections.emptySet();
        ModuleRecipeConditions.Failure failure = ModuleRecipeConditions.evaluate(
            moduleRecipe, activeModules);
        if (failure != ModuleRecipeConditions.Failure.NONE) {
            cir.setReturnValue(CraftingStatus.failure(failure.getMessage()));
        }
    }

    @Inject(method = {"canStartCrafting", "canRestartCrafting"}, at = @At("RETURN"))
    private void mmceComplement$adjustBatchToAvailableInputs(
        RecipeCraftingContext context,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        if (mmceComplement$batchPrepared
            && cir.getReturnValue() != null
            && cir.getReturnValue().isSuccess()) {
            mmceComplement$adjustBatchFactorToActualParallelism(parallelism);
        }
    }

    @Override
    public int mmceComplement$getOrCalculateBatchFactor(float theoreticalDuration,
                                                        int maxBatchTime) {
        if (mmceComplement$batchPrepared) {
            return mmceComplement$batchFactor;
        }
        if (mmceComplement$batchEligibleParallelism <= 0) {
            mmceComplement$batchFactor = 1;
            return 1;
        }
        int maxFactor = Integer.MAX_VALUE
            / mmceComplement$batchEligibleParallelism;
        mmceComplement$batchFactor = BatchProcessingLogic.calculateFactor(
            theoreticalDuration, maxBatchTime, maxFactor);
        return mmceComplement$batchFactor;
    }

    @Override
    public void mmceComplement$beginBatchCalculation(int baseMaxParallelism) {
        mmceComplement$batchBaseMaxParallelism = Math.max(1, baseMaxParallelism);
        mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
        mmceComplement$batchExcludedParallelism = 0;
        maxParallelism = mmceComplement$batchBaseMaxParallelism;
        mmceComplement$batchFactor = 1;
        mmceComplement$batchOutputMaxParallelism = maxParallelism;
        mmceComplement$batchParallelismApplied = false;
        mmceComplement$maxParallelismExplicitlyUpdated = false;
        mmceComplement$batchPrepared = false;
        mmceComplement$batchCalculationActive = true;
    }

    @Override
    public void mmceComplement$finishBatchCalculation() {
        mmceComplement$batchCalculationActive = false;
        mmceComplement$batchPrepared = true;
    }

    @Override
    public void mmceComplement$adjustBatchFactorToActualParallelism(int actualParallelism) {
        int eligibleActual = Math.max(1,
            actualParallelism - mmceComplement$batchExcludedParallelism);
        mmceComplement$batchFactor = BatchProcessingLogic.factorForActualParallelism(
            eligibleActual, mmceComplement$batchEligibleParallelism,
            mmceComplement$batchFactor);
    }

    @Override
    public int mmceComplement$getBaseMaxParallelism() {
        return mmceComplement$batchBaseMaxParallelism;
    }

    @Unique
    private int mmceComplement$getExcludedParallelism(
        TileMultiblockMachineController controller, int maxParallelism) {
        if (!(controller instanceof TileFactoryController)) {
            return 0;
        }
        int extraThreads = Math.max(0,
            ((TileFactoryController) controller).getExtraThreadCount());
        return Math.min(Math.max(0, maxParallelism), extraThreads);
    }
}
