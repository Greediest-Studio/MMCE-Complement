package net.edwin.mmcecomplement.variant;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/** Exchanges one cursor container, stowing extra results from stacked containers. */
public final class StorageFluidInteraction {
    private StorageFluidInteraction() { }

    public static FluidActionResult interact(ItemStack cursor, IFluidHandler tank,
                                             IItemHandler inventory, @Nullable EntityPlayer player) {
        if (cursor.isEmpty() || tank == null) return FluidActionResult.FAILURE;
        FluidActionResult result = FluidUtil.tryFillContainerAndStow(
            cursor, tank, inventory, Integer.MAX_VALUE, player, true);
        return result.isSuccess() ? result : FluidUtil.tryEmptyContainerAndStow(
            cursor, tank, inventory, Integer.MAX_VALUE, player, true);
    }
}
