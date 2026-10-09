package net.edwin.mmcecomplement.mixin;

import github.kasuminova.mmce.client.world.BlockModelHider;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.attachment.AttachmentMachine;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.edwin.mmcecomplement.attachment.AttachmentModule;
import net.edwin.mmcecomplement.attachment.AttachmentPatternResolver;
import net.edwin.mmcecomplement.attachment.AttachmentModelMode;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Rebuilds MMCE's client-side hidden-block list after attachments are merged. */
@Mixin(value = TileMultiblockMachineController.class, remap = false)
public abstract class MixinTileMultiblockMachineControllerClient {

    @Shadow
    protected DynamicMachine foundMachine;

    @Shadow
    protected net.minecraft.util.EnumFacing controllerRotation;

    @Shadow
    protected TaggedPositionBlockArray foundPattern;

    /** The native client pattern before attachment modules are appended. */
    private TaggedPositionBlockArray mmceComplement$clientMainPattern;

    /** The pattern currently published to the controller after appending modules. */
    private TaggedPositionBlockArray mmceComplement$clientCombinedPattern;

    @Inject(method = "updateComponents", at = @At("RETURN"))
    private void mmceComplement$refreshAttachmentModelHiddenBlocks(
        CallbackInfo ci) {
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) (Object) this;
        if (controller.getWorld() == null || !controller.getWorld().isRemote) {
            return;
        }
        DynamicMachine machine = foundMachine;
        if (machine == null) {
            return;
        }
        AttachmentModelMode mode = ((AttachmentMachine) (Object) machine)
            .mmceComplement$getAttachmentModelMode();
        if (mode == AttachmentModelMode.HIDE
            || mode == AttachmentModelMode.SEPARATE) {
            mmceComplement$rebuildClientAttachmentPattern(controller);
            mmceComplement$removePreviousHiddenModel(controller.getPos());
            mmceComplement$addHiddenModelBlocks(controller);
            BlockModelHider.hideOrShowBlocks(controller);
        }
    }

    /**
     * The native MMCE hider is normally scheduled from readCustomNBT and can
     * run before updateComponents on a freshly received client tile.  Hook the
     * getter it uses so the combined attachment pattern is available at the
     * exact point where the hider reads it.
     */
    @Inject(method = "getFoundPattern", at = @At("RETURN"), cancellable = true)
    private void mmceComplement$publishPatternToModelHider(
        CallbackInfoReturnable<TaggedPositionBlockArray> cir) {
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) (Object) this;
        if (controller.getWorld() == null || !controller.getWorld().isRemote
            || foundMachine == null) {
            return;
        }
        AttachmentModelMode mode = ((AttachmentMachine) (Object) foundMachine)
            .mmceComplement$getAttachmentModelMode();
        if (mode != AttachmentModelMode.HIDE
            && mode != AttachmentModelMode.SEPARATE) {
            return;
        }
        mmceComplement$rebuildClientAttachmentPattern(controller);
        cir.setReturnValue(foundPattern);
    }

    /**
     * Attachment matching is intentionally server-side.  MMCE's model hider,
     * however, reads the client controller's foundPattern, so publish the
     * server-synchronised active modules into a client-side combined pattern
     * before invoking it.  Keep the native pattern as a separate base to avoid
     * repeatedly appending the same modules on every component update.
     */
    private void mmceComplement$rebuildClientAttachmentPattern(
        TileMultiblockMachineController controller) {
        if (foundPattern == null || foundMachine == null
            || controllerRotation == null) {
            mmceComplement$clientMainPattern = null;
            mmceComplement$clientCombinedPattern = null;
            return;
        }

        TaggedPositionBlockArray main = foundPattern;
        if (foundPattern == mmceComplement$clientCombinedPattern
            && mmceComplement$clientMainPattern != null) {
            main = mmceComplement$clientMainPattern;
        } else {
            mmceComplement$clientMainPattern = main;
        }

        Set<String> active = ((AttachmentController) (Object) controller)
            .mmceComplement$getActiveAttachmentModules();
        Map<String, AttachmentModule> definitions =
            ((AttachmentMachine) (Object) foundMachine)
                .mmceComplement$getAttachmentModules();
        if (active == null || active.isEmpty() || definitions.isEmpty()) {
            foundPattern = main;
            mmceComplement$clientCombinedPattern = null;
            return;
        }

        TaggedPositionBlockArray combined = new TaggedPositionBlockArray(main);
        for (String id : active) {
            AttachmentModule module = definitions.get(id);
            if (module == null) {
                continue;
            }
            TaggedPositionBlockArray effective = module.getEffectivePattern(
                foundMachine.getPattern(), definitions);
            TaggedPositionBlockArray rotated =
                AttachmentPatternResolver.getRotatedPattern(
                    effective, controllerRotation);
            if (rotated != null) {
                AttachmentPatternResolver.appendPreservingParent(combined, rotated);
            }
        }
        combined.flushTileBlocksCache();
        mmceComplement$clientCombinedPattern = combined;
        foundPattern = combined;
    }

    @Inject(method = "readCustomNBT", at = @At("RETURN"))
    private void mmceComplement$refreshAttachmentModelAfterSync(
        net.minecraft.nbt.NBTTagCompound compound,
        CallbackInfo ci) {
        mmceComplement$refreshAttachmentModelHiddenBlocks();
    }

    private void mmceComplement$refreshAttachmentModelHiddenBlocks() {
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) (Object) this;
        if (controller.getWorld() == null || !controller.getWorld().isRemote
            || foundMachine == null) {
            return;
        }
        AttachmentModelMode mode = ((AttachmentMachine) (Object) foundMachine)
            .mmceComplement$getAttachmentModelMode();
        if (mode != AttachmentModelMode.HIDE
            && mode != AttachmentModelMode.SEPARATE) {
            return;
        }
        mmceComplement$rebuildClientAttachmentPattern(controller);
        mmceComplement$removePreviousHiddenModel(controller.getPos());
        mmceComplement$addHiddenModelBlocks(controller);
        BlockModelHider.hideOrShowBlocks(controller);
    }

    /**
     * MMCE's helper refuses to create a hidden-block entry when the model
     * registry has not finished loading (or when a model is supplied by a
     * compat renderer rather than DynamicMachineModelRegistry).  The hider's
     * persistence API itself has no such restriction, so publish the exact
     * block set directly as well.  This also makes the operation reliable when
     * the client receives the attachment NBT before the model reload callback.
     */
    private void mmceComplement$addHiddenModelBlocks(
        TileMultiblockMachineController controller) {
        if (foundPattern == null) {
            return;
        }
        List<BlockPos> blocks = new ArrayList<>(foundPattern.getPattern().size() + 1);
        BlockPos origin = controller.getPos();
        blocks.add(origin);
        for (BlockPos relative : foundPattern.getPattern().keySet()) {
            blocks.add(origin.add(relative));
        }
        try {
            Class<?> data = Class.forName(
                "com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData");
            data.getMethod("addDisableModel", BlockPos.class, Collection.class)
                .invoke(null, origin, blocks);
        } catch (ReflectiveOperationException ignored) {
            // Component Model Hider is optional; MMCE's helper remains the
            // fallback when this persistence class is not installed.
        }
    }

    /** Component Model Hider keeps one immutable entry per controller position. */
    private static void mmceComplement$removePreviousHiddenModel(BlockPos pos) {
        try {
            Class<?> data = Class.forName(
                "com.cleanroommc.multiblocked.persistence.MultiblockWorldSavedData");
            data.getMethod("removeDisableModel", BlockPos.class).invoke(null, pos);
        } catch (ReflectiveOperationException ignored) {
            // The optional hider may not be installed; MMCE's helper is a no-op then.
        }
    }
}
