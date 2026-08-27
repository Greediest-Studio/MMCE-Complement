package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Item-style 4x4 configuration and 4x4 display container. */
public class ContainerMEInputAssembly extends AEBaseContainer {

    private final TileMEInputAssembly owner;

    public ContainerMEInputAssembly(TileMEInputAssembly owner,
                                    EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;
        bindPlayerInventory(getInventoryPlayer(), 0, 123);
        IItemHandlerModifiable config =
            owner.getConfigInventory().asGUIAccess();
        IItemHandlerModifiable display =
            owner.getDisplayInventory().asGUIAccess();
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int slot = row * 4 + column;
                // The full-exposure assembly deliberately has no filter
                // configuration.  Use disabled slots here as well as the
                // server-side no-op marker handler, so the GUI cannot imply
                // that filters are supported.
                if (owner instanceof TileMEFullExposureAssembly) {
                    addSlotToContainer(new SlotDisabled(config, slot,
                        8 + column * 18, 35 + row * 18));
                } else {
                    addSlotToContainer(new SlotFake(config, slot,
                        8 + column * 18, 35 + row * 18));
                }
            }
        }
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int slot = row * 4 + column;
                addSlotToContainer(new SlotDisabled(display, slot,
                    98 + column * 18, 35 + row * 18));
            }
        }
    }

    public TileMEInputAssembly getOwner() {
        return owner;
    }
}
