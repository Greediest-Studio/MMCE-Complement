package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.tile.TileStorageController;
import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.variant.StorageMachine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fallback for existing worlds whose controller block was created by MMCE. */
@Mixin(value = BlockController.class, remap = false)
public abstract class MixinBlockControllerStorageFallback {
    private boolean mmceComplement$isStorage() {
        DynamicMachine machine = ((BlockController) (Object) this).getParentMachine();
        return machine instanceof StorageMachine
            && ((StorageMachine) (Object) machine).mmceComplement$isStorageVariant();
    }

    @Inject(method = "createTileEntity", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$storageTile(World world, IBlockState state,
                                             CallbackInfoReturnable<TileEntity> cir) {
        if (mmceComplement$isStorage()) {
            cir.setReturnValue(new TileStorageController(
                (BlockController) (Object) this, state));
        }
    }

    @Inject(method = "createNewTileEntity", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$storageTileFromMeta(World world, int meta,
                                                     CallbackInfoReturnable<TileEntity> cir) {
        if (mmceComplement$isStorage()) {
            BlockController block = (BlockController) (Object) this;
            cir.setReturnValue(new TileStorageController(block,
                block.getStateFromMeta(meta)));
        }
    }

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$storageGui(World world, BlockPos pos,
                                            IBlockState state, EntityPlayer player,
                                            EnumHand hand, EnumFacing facing,
                                            float hitX, float hitY, float hitZ,
                                            CallbackInfoReturnable<Boolean> cir) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMultiblockMachineController
            && tile instanceof StorageController
            && ((StorageController) tile).mmceComplement$isStorageController()) {
            // This path is used by controllers that were already placed before
            // the dedicated storage block was registered.  Since the mixin
            // consumes the activation before vanilla can animate it, reproduce
            // the normal hand swing on both logical sides.
            player.swingArm(hand);
            if (!world.isRemote) {
                player.openGui(MMCEComplement.instance, MMCEComplement.GUI_STORAGE,
                    world, pos.getX(), pos.getY(), pos.getZ());
            }
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void mmceComplement$dropStorage(World world, BlockPos pos,
                                             IBlockState state, CallbackInfo ci) {
        if (!world.isRemote && mmceComplement$isStorage()) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof StorageController) {
                net.edwin.mmcecomplement.block.BlockStorageController.dropInventory(
                    world, pos,
                    ((StorageController) tile).mmceComplement$getStorageInventory());
            }
        }
    }
}
