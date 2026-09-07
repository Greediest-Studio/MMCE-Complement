package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssemblyFallback;
import net.minecraft.entity.player.EntityPlayer;

/** Minimal Mekanism-free UI container for the full-exposure assembly. */
public class ContainerMEFullExposureFallback extends AEBaseContainer {

    private final TileMEFullExposureAssemblyFallback owner;

    public ContainerMEFullExposureFallback(
        TileMEFullExposureAssemblyFallback owner, EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;
        bindPlayerInventory(getInventoryPlayer(), 0, 123);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int slot = row * 4 + column;
                addSlotToContainer(new SlotDisabled(
                    owner.getItemInventory(), slot,
                    8 + column * 18, 35 + row * 18));
            }
        }
    }

    public TileMEFullExposureAssemblyFallback getOwner() {
        return owner;
    }
}
