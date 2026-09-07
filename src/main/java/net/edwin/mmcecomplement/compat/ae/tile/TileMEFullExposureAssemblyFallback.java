package net.edwin.mmcecomplement.compat.ae.tile;

import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;

/**
 * Mekanism-free fallback so the full-exposure block remains registered when
 * the optional gas channel is unavailable. Item-only inventory behavior is
 * provided by the common ME inventory input bus.
 */
public class TileMEFullExposureAssemblyFallback
    extends TileMEItemInventoryInputBus {

    public net.minecraftforge.items.IItemHandlerModifiable getItemInventory() {
        return inventory;
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY == null
            ? super.getVisualItemStack()
            : new ItemStack(ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
    }
}
