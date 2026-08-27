package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Display-only 4x4 container for the mixed ME output assembly. */
public class ContainerMEOutputAssembly extends AEBaseContainer {

    private final TileMEOutputAssembly owner;

    public ContainerMEOutputAssembly(TileMEOutputAssembly owner,
                                     EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;
        bindPlayerInventory(getInventoryPlayer(), 0, 105);
        IItemHandlerModifiable display =
            owner.getDisplayInventory().asGUIAccess();
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int slot = row * 4 + column;
                addSlotToContainer(new SlotDisabled(display, slot,
                    52 + column * 18, 35 + row * 18));
            }
        }
    }

    public TileMEOutputAssembly getOwner() {
        return owner;
    }
}
