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

    @Unique
    private boolean mmceComplement$batchPrepared;

    @Unique
    private boolean mmceComplement$batchCalculationActive;

    @Inject(method = "<init>(Lhellfirepvp/modularmachinery/common/crafting/MachineRecipe;I)V",
        at = @At("RETURN"))
    private void mmceComplement$initializeBatchData(MachineRecipe recipe,
                                                    int maxParallelism,
                                                    CallbackInfo ci) {
        mmceComplement$batchBaseMaxParallelism = recipe.isParallelized()
            ? Math.max(1, maxParallelism)
            : 1;
        mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
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
            mmceComplement$batchBaseMaxParallelism = recipe != null && recipe.isParallelized()
                ? Math.max(1, maxParallelism)
                : 1;
            mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
        }
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
        maxParallelism = Math.max(1, mmceComplement$batchBaseMaxParallelism);
        mmceComplement$batchEligibleParallelism = mmceComplement$batchBaseMaxParallelism;
        mmceComplement$batchExcludedParallelism = 0;
        mmceComplement$batchFactor = 1;
        mmceComplement$batchPrepared = false;
        mmceComplement$batchCalculationActive = false;
    }

    @Inject(method = "calculateExtraParallelism", at = @At("HEAD"))
    private void mmceComplement$beginBatchCalculation(
        RecipeCraftingContext context, CallbackInfo ci) {
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
            mmceComplement$batchEligibleParallelism =
                mmceComplement$batchBaseMaxParallelism;
            maxParallelism = Math.max(1, mmceComplement$batchBaseMaxParallelism);
            return;
        }
        int baseParallelism = mmceComplement$batchBaseMaxParallelism;
        int excludedParallelism = mmceComplement$getExcludedParallelism(controller,
            baseParallelism);
        mmceComplement$batchExcludedParallelism = excludedParallelism;
        mmceComplement$batchEligibleParallelism = Math.max(1,
            baseParallelism - excludedParallelism);
        // Keep the original unbatched budget until the return hook.  This is
        // important when a machine has both normal and custom threads: only
        // the normal portion is scaled below.
        maxParallelism = Math.max(1, baseParallelism);
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
        int unbatchedFactor = BatchProcessingLogic.factorForActualParallelism(
            maxParallelism, mmceComplement$batchBaseMaxParallelism,
            Integer.MAX_VALUE);
        int combinedFactor = BatchProcessingLogic.multiplyParallelismSaturated(
            unbatchedFactor, mmceComplement$batchFactor);
        maxParallelism = BatchProcessingLogic.multiplyParallelismExcluding(
            mmceComplement$batchBaseMaxParallelism,
            mmceComplement$batchExcludedParallelism,
            combinedFactor);
        mmceComplement$finishBatchCalculation();
    }

    @Inject(method = "canStartCrafting", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesBeforeStart(
        RecipeCraftingContext context,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        mmceComplement$rejectCheckIfNeeded(context.getMachineController(), cir);
    }

    @Inject(method = "canRestartCrafting", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesBeforeRestart(
        RecipeCraftingContext context,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        mmceComplement$rejectCheckIfNeeded(context.getMachineController(), cir);
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

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$checkModulesWhileRunning(
        TileMultiblockMachineController controller,
        RecipeCraftingContext context,
        CallbackInfoReturnable<CraftingStatus> cir) {
        ModuleRecipeConditions.Failure failure = mmceComplement$evaluate(controller);
        if (failure != ModuleRecipeConditions.Failure.NONE) {
            cir.setReturnValue(CraftingStatus.failure(failure.getMessage()));
        }
    }

    @Unique
    private void mmceComplement$rejectCheckIfNeeded(
        TileMultiblockMachineController controller,
        CallbackInfoReturnable<RecipeCraftingContext.CraftingCheckResult> cir) {
        ModuleRecipeConditions.Failure failure = mmceComplement$evaluate(controller);
        if (failure == ModuleRecipeConditions.Failure.NONE) {
            return;
        }
        RecipeCraftingContext.CraftingCheckResult result =
            new RecipeCraftingContext.CraftingCheckResult();
        result.addError(failure.getMessage());
        cir.setReturnValue(result);
    }

    @Unique
    private ModuleRecipeConditions.Failure mmceComplement$evaluate(
        TileMultiblockMachineController controller) {
        Collection<String> activeModules = controller instanceof AttachmentController
            ? ((AttachmentController) controller).mmceComplement$getActiveAttachmentModules()
            : Collections.emptySet();
        return ModuleRecipeConditions.evaluate((ModuleRecipeData) (Object) recipe, activeModules);
    }

    @Override
    public int mmceComplement$getOrCalculateBatchFactor(float theoreticalDuration,
                                                        int maxBatchTime) {
        if (mmceComplement$batchPrepared) {
            return mmceComplement$batchFactor;
        }
        int maxFactor = Integer.MAX_VALUE
            / Math.max(1, mmceComplement$batchEligibleParallelism);
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
