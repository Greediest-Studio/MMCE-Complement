package net.edwin.mmcecomplement.mixin;

import hellfirepvp.modularmachinery.common.selection.PlayerStructureSelectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(value = PlayerStructureSelectionHelper.class, remap = false)
public interface AccessorPlayerStructureSelectionHelper {

    @Accessor("activeSelectionMap")
    static Map<UUID, PlayerStructureSelectionHelper.StructureSelection>
    mmceComplement$getActiveSelectionMap() {
        throw new AssertionError();
    }
}
