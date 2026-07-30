package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/** Registers the data half after MMCE registers the hatch's item component. */
@Mixin(value = TileMultiblockMachineController.class, remap = false)
public abstract class MixinDataItemInputController {

    @Inject(method = "checkAndAddComponents", at = @At("RETURN"))
    private void mmceComplement$registerDataItemInterface(
        BlockPos relativePos, BlockPos controllerPos,
        Map<Long, Map<TileEntity, ProcessingComponent<?>>> found,
        CallbackInfo ci) {
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) (Object) this;
        BlockPos realPos = controllerPos.add(relativePos);
        if (!controller.getWorld().isBlockLoaded(realPos)) {
            return;
        }
        TileEntity tile = controller.getWorld().getTileEntity(realPos);
        if (tile instanceof TileDataItemInputHatch
            && !(tile instanceof TileItemInputAssemblyHatch)) {
            controller.checkAndAddSmartInterface(
                ((TileDataItemInputHatch) tile).getDataProvider(), realPos);
        }
    }
}
