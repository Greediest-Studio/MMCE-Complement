package net.edwin.mmcecomplement.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/** Non-interactive server container for the brief unformed-structure notice. */
public final class ContainerStorageUnformed extends Container {
    @Override public boolean canInteractWith(EntityPlayer player) { return true; }
}
