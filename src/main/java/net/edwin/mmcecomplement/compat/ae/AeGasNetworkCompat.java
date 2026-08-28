package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.MixedMEInputMarker;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Mekanism Energistics packet bridge. */
public final class AeGasNetworkCompat {

    private AeGasNetworkCompat() {}

    public static boolean applyMarker(EntityPlayerMP player, TileEntity tile,
                                      int fieldId, NBTTagCompound nbt) {
        if (!(tile instanceof TileMEInputAssembly)
                || fieldId != NetworkHandlerMMCE.FIELD_ME_INPUT_ASSEMBLY_MARKER) {
            return false;
        }
        if (!(player.openContainer instanceof ContainerMEInputAssembly)
                || ((ContainerMEInputAssembly) player.openContainer)
                    .getOwner() != tile) {
            return true;
        }
        int slot = nbt.getInteger("slot");
        if (slot < 0 || slot >= TileMEInputAssembly.SLOT_COUNT) return true;
        ItemStack marker = nbt.hasKey("marker", 10)
            ? new ItemStack(nbt.getCompoundTag("marker")) : ItemStack.EMPTY;
        if (!marker.isEmpty() && nbt.hasKey("amount", 99)) {
            marker = MixedMEInputMarker.withAmount(marker, nbt.getLong("amount"));
        }
        ((TileMEInputAssembly) tile).setMarker(slot, marker);
        player.openContainer.detectAndSendChanges();
        return true;
    }
}
