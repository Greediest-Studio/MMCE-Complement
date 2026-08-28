package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaBusBase;
import net.minecraft.tileentity.TileEntity;

/** CrazyAE/Modular Magic mana packet bridge. */
public final class AeManaNetworkCompat {

    private AeManaNetworkCompat() {}

    public static boolean setBufferCapacity(TileEntity tile, long capacity) {
        if (!(tile instanceof TileMEManaBusBase)) return false;
        ((TileMEManaBusBase) tile).setBufferCapacityRaw(capacity);
        return true;
    }
}
