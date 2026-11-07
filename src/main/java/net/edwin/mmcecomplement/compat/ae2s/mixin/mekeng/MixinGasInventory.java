package net.edwin.mmcecomplement.compat.ae2s.mixin.mekeng;

import mekanism.api.gas.GasTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Restores the getter used by MMCE against AppliedCompatibility's record accessor. */
@Mixin(targets = "com.mekeng.github.common.me.inventory.impl.GasInventory", remap = false)
public abstract class MixinGasInventory {
    @Shadow public abstract GasTank[] tanks();

    // Return the existing tanks: copying or replacing them would disconnect
    // MMCE's capacity checks from the inventories, callbacks and saved contents.
    // Preserve a future upstream implementation if it supplies this getter.
    @Unique(silent = true)
    public GasTank[] getTanks() {
        return tanks();
    }
}
