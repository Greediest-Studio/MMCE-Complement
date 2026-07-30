package net.edwin.mmcecomplement.filter;

import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Marker-bearing MMCE item output component. */
public final class FilteredItemOutputComponent extends MachineComponent.ItemBus
    implements FilteredOutputComponent {

    private final IItemHandlerModifiable handler;

    public FilteredItemOutputComponent(IItemHandlerModifiable handler) {
        super(IOType.OUTPUT);
        this.handler = handler;
    }

    @Override
    public IItemHandlerModifiable getContainerProvider() {
        return handler;
    }
}
