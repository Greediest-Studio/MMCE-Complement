package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.block.BlockFactoryController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
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

/** Same fallback for MMCE integrated/factory controllers. */
@Mixin(value = BlockFactoryController.class, remap = false)
public abstract class MixinBlockFactoryControllerStorageFallback {
    private boolean mmceComplement$isStorage() {
        DynamicMachine machine = ((BlockFactoryController) (Object) this).getParentMachine();
        return machine instanceof StorageMachine
            && ((StorageMachine) (Object) machine).mmceComplement$isStorageVariant();
    }

    @Inject(method = "createTileEntity", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$storageTile(World world, IBlockState state,
                                             CallbackInfoReturnable<TileEntity> cir) {
        if (mmceComplement$isStorage()) {
            cir.setReturnValue(new TileStorageController(
                ((BlockFactoryController) (Object) this), state));
        }
    }

    @Inject(method = "createNewTileEntity", at = @At("HEAD"), cancellable = true)
    private void mmceComplement$storageTileFromMeta(World world, int meta,
                                                     CallbackInfoReturnable<TileEntity> cir) {
        if (mmceComplement$isStorage()) {
            BlockFactoryController block = (BlockFactoryController) (Object) this;
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
        if (tile instanceof StorageController
            && ((StorageController) tile).mmceComplement$isStorageController()) {
            // The fallback consumes the block activation, so vanilla will not
            // perform its usual hand animation for this interaction.
            player.swingArm(hand);
            if (!world.isRemote) {
                player.openGui(MMCEComplement.instance, MMCEComplement.GUI_STORAGE,
                    world, pos.getX(), pos.getY(), pos.getZ());
            }
            cir.setReturnValue(true);
        }
    }
}
