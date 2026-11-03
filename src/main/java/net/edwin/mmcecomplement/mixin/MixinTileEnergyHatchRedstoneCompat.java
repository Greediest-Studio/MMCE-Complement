package net.edwin.mmcecomplement.mixin;

import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps MMCE energy hatches usable when a launcher transformer cannot load
 * MMCE's optional RedstoneHelper class. Only the helper call is redirected;
 * TileEnergyHatch's comparator/update bookkeeping remains native.
 */
@Mixin(targets = "hellfirepvp.modularmachinery.common.tiles.base.TileEnergyHatch",
       remap = false)
public abstract class MixinTileEnergyHatchRedstoneCompat {

    @Redirect(
        method = "markNoUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lhellfirepvp/modularmachinery/common/util/RedstoneHelper;"
                + "getRedstoneLevel(Lnet/minecraft/tileentity/TileEntity;)I"),
        remap = false)
    private int mmceComplement$readRedstoneLevelWithoutHelper(TileEntity tile) {
        if (tile == null || tile.getWorld() == null) {
            return 0;
        }
        return tile.getWorld().getRedstonePowerFromNeighbors(tile.getPos());
    }
}
