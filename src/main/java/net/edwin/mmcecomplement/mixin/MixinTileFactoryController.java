package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.edwin.mmcecomplement.block.BlockThreadHatch;
import net.edwin.mmcecomplement.config.ModConfig;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.mechannel.MEChannelReservationLifecycle;
import net.edwin.mmcecomplement.thread.ThreadHatchLogic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds normal recipe-thread scaling supplied by formed thread hatches. */
@Mixin(value = TileFactoryController.class, remap = false)
public abstract class MixinTileFactoryController {

    @Unique
    private static final String mmceComplement$NBT_THREAD_HATCH_TIER =
        "mmceComplementThreadHatchTier";

    @Unique
    private static final String mmceComplement$NBT_THREAD_HATCH_COUNTS =
        "mmceComplementThreadHatchCounts";

    @Unique
    private final int[] mmceComplement$threadHatchCounts =
        new int[BlockThreadHatch.ThreadHatchType.values().length];

    /**
     * Runs after attachment handling has replaced foundPattern with the combined
     * main/module pattern, so hatches in active modules are included as well.
     */
    @Inject(method = "updateComponents", at = @At("RETURN"))
    private void mmceComplement$refreshThreadHatchTier(CallbackInfo ci) {
        TileFactoryController controller = (TileFactoryController) (Object) this;
        World world = controller.getWorld();
        TaggedPositionBlockArray foundPattern = controller.getFoundPattern();
        if (foundPattern == null || world == null) {
            java.util.Arrays.fill(mmceComplement$threadHatchCounts, 0);
            return;
        }

        int[] counts = new int[mmceComplement$threadHatchCounts.length];
        BlockPos controllerPos = controller.getPos();
        for (BlockPos relativePos : foundPattern.getPattern().keySet()) {
            IBlockState state = world.getBlockState(controllerPos.add(relativePos));
            if (state.getBlock() == ModBlocks.THREAD_HATCH) {
                counts[BlockThreadHatch.getTier(state) - 1]++;
            }
        }
        System.arraycopy(counts, 0, mmceComplement$threadHatchCounts, 0, counts.length);
    }

    @Inject(method = "getMaxThreads", at = @At("RETURN"), cancellable = true)
    private void mmceComplement$applyThreadHatch(CallbackInfoReturnable<Integer> cir) {
        TileFactoryController controller = (TileFactoryController) (Object) this;
        DynamicMachine foundMachine = controller.getFoundMachine();
        if (!mmceComplement$hasThreadHatch() || foundMachine == null) {
            return;
        }
        int baseThreads = foundMachine.getMaxThreads();
        int extraThreads = cir.getReturnValue() - baseThreads;
        cir.setReturnValue(ThreadHatchLogic.apply(
            baseThreads,
            extraThreads,
            mmceComplement$threadHatchCounts,
            ModConfig.threadHatch.getMultipliers(),
            ModConfig.threadHatch.allowStacking));
    }

    @Inject(method = "resetMachine", at = @At("RETURN"))
    private void mmceComplement$resetThreadHatchTier(boolean clearData, CallbackInfo ci) {
        java.util.Arrays.fill(mmceComplement$threadHatchCounts, 0);
    }

    /**
     * MMCE's factory reset clears both thread collections directly instead of
     * returning their crafting contexts to the pool.  Release dynamic ME
     * channel reservations before those contexts become unreachable.
     */
    @Inject(method = "resetRecipe", at = @At("HEAD"))
    private void mmceComplement$releaseFactoryMEChannels(CallbackInfo ci) {
        TileFactoryController controller =
            (TileFactoryController) (Object) this;
        for (FactoryRecipeThread thread
            : controller.getFactoryRecipeThreadList()) {
            MEChannelReservationLifecycle.release(thread.getContext());
        }
        for (FactoryRecipeThread thread
            : controller.getCoreRecipeThreads().values()) {
            MEChannelReservationLifecycle.release(thread.getContext());
        }
    }

    @Inject(method = "writeCustomNBT", at = @At("RETURN"))
    private void mmceComplement$writeThreadHatchTier(NBTTagCompound compound, CallbackInfo ci) {
        compound.setIntArray(mmceComplement$NBT_THREAD_HATCH_COUNTS,
            java.util.Arrays.copyOf(mmceComplement$threadHatchCounts,
                mmceComplement$threadHatchCounts.length));
        compound.setByte(mmceComplement$NBT_THREAD_HATCH_TIER,
            (byte) mmceComplement$getHighestThreadHatchTier());
    }

    @Inject(method = "readCustomNBT", at = @At("RETURN"))
    private void mmceComplement$readThreadHatchTier(NBTTagCompound compound, CallbackInfo ci) {
        java.util.Arrays.fill(mmceComplement$threadHatchCounts, 0);
        int[] savedCounts = compound.getIntArray(mmceComplement$NBT_THREAD_HATCH_COUNTS);
        if (savedCounts.length > 0) {
            int length = Math.min(savedCounts.length, mmceComplement$threadHatchCounts.length);
            for (int tier = 0; tier < length; tier++) {
                mmceComplement$threadHatchCounts[tier] = Math.max(0, savedCounts[tier]);
            }
            return;
        }

        // Backward compatibility with worlds saved by the first Thread Hatch build.
        int oldTier = Math.max(0, Math.min(mmceComplement$threadHatchCounts.length,
            compound.getByte(mmceComplement$NBT_THREAD_HATCH_TIER)));
        if (oldTier > 0) {
            mmceComplement$threadHatchCounts[oldTier - 1] = 1;
        }
    }

    @Unique
    private boolean mmceComplement$hasThreadHatch() {
        for (int count : mmceComplement$threadHatchCounts) {
            if (count > 0) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private int mmceComplement$getHighestThreadHatchTier() {
        for (int tier = mmceComplement$threadHatchCounts.length - 1; tier >= 0; tier--) {
            if (mmceComplement$threadHatchCounts[tier] > 0) {
                return tier + 1;
            }
        }
        return 0;
    }
}
