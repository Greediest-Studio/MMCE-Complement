package net.edwin.mmcecomplement.tile;

/** Samples the redstone power delivered to its own block position. */
public class TileRedstoneSignalInputHatch extends TileRedstoneInterfaceHatch {

    public int getReceivedSignalStrength() {
        return world == null ? 0 : world.getRedstonePowerFromNeighbors(pos);
    }
}
