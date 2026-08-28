package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyBusBase;
import net.minecraft.tileentity.TileEntity;

/** CrazyAE energy packet bridge. */
public final class AeEnergyNetworkCompat {

    private AeEnergyNetworkCompat() {}

    public static boolean setBufferCapacity(TileEntity tile, long capacity) {
        if (!(tile instanceof TileMEEnergyBusBase)) return false;
        ((TileMEEnergyBusBase) tile).setBufferCapacityRaw(capacity);
        return true;
    }
}
