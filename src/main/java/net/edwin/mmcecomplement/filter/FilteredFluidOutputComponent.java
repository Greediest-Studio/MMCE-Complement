package net.edwin.mmcecomplement.filter;

import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Marker-bearing MMCE fluid output component. */
public final class FilteredFluidOutputComponent
    extends MachineComponent.FluidHatch implements FilteredOutputComponent {

    private final IFluidHandler handler;

    public FilteredFluidOutputComponent(IFluidHandler handler) {
        super(IOType.OUTPUT);
        this.handler = handler;
    }

    @Override
    public IFluidHandler getContainerProvider() {
        return handler;
    }
}
