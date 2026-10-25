package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses MMCE's private parent block for custom controller subclasses. */
@Mixin(value = TileMachineController.class, remap = false)
public interface AccessorTileMachineController {
    @Accessor("parentController")
    void mmceComplement$setParentController(BlockController controller);
}
