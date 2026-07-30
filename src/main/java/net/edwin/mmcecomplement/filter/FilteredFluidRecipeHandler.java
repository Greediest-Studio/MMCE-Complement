package net.edwin.mmcecomplement.filter;

import net.minecraftforge.fluids.capability.IFluidHandler;

/** Fluid handler capable of retaining its filter in recipe-space snapshots. */
public interface FilteredFluidRecipeHandler extends IFluidHandler {
    FilteredFluidRecipeHandler copyForSimulation();
}
