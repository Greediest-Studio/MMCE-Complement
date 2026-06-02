package net.edwin.mmcecomplement.mixin.client;

import hellfirepvp.modularmachinery.client.gui.GuiFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import net.edwin.mmcecomplement.util.MachineControlInterfaceHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiFactoryController.class)
public abstract class MixinGuiFactoryController {

    @Shadow(remap = false)
    private TileFactoryController factory;

    @Redirect(
            method = "drawFactoryStatus",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getStrongPower(Lnet/minecraft/util/math/BlockPos;)I"),
            remap = false
    )
    private int mmceComplement$useInterfacePower(World world, BlockPos pos) {
        return MachineControlInterfaceHelper.getEffectiveRedstonePower(factory);
    }
}