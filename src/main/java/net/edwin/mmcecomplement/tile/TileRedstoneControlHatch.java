package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.edwin.mmcecomplement.block.BlockRedstoneControlHatch;
import net.edwin.mmcecomplement.redstone.RedstoneControlLogic;
import net.minecraft.nbt.NBTTagCompound;

/** Stores and evaluates the shutdown signal threshold for a machine. */
public class TileRedstoneControlHatch extends TileColorableMachineComponent {

    private static final String TAG_SHUTDOWN_THRESHOLD = "ShutdownThreshold";

    private int shutdownThreshold = RedstoneControlLogic.MIN_THRESHOLD;

    public int getShutdownThreshold() {
        return shutdownThreshold;
    }

    public void setShutdownThreshold(int shutdownThreshold) {
        int clamped = RedstoneControlLogic.clampThreshold(shutdownThreshold);
        if (this.shutdownThreshold == clamped) {
            return;
        }
        this.shutdownThreshold = clamped;
        markForUpdateSync();
        syncDisabledState();
    }

    public int getReceivedSignalStrength() {
        if (world == null) {
            return 0;
        }
        // Sample the power delivered to this hatch's own block position. The
        // controller position and all other structure positions are irrelevant
        // to this hatch's configured threshold.
        return world.getRedstonePowerFromNeighbors(pos);
    }

    public boolean isShutdownSignalActive() {
        return RedstoneControlLogic.shouldShutdown(
            getReceivedSignalStrength(), shutdownThreshold);
    }

    /** Returns zero unless this hatch is currently requesting a shutdown. */
    public int getShutdownPower() {
        int signal = getReceivedSignalStrength();
        return RedstoneControlLogic.shouldShutdown(signal, shutdownThreshold)
            ? signal
            : 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncDisabledState();
    }

    private void syncDisabledState() {
        if (world != null && !world.isRemote) {
            BlockRedstoneControlHatch.syncDisabledState(world, pos);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        shutdownThreshold = compound.hasKey(TAG_SHUTDOWN_THRESHOLD)
            ? RedstoneControlLogic.clampThreshold(
                compound.getInteger(TAG_SHUTDOWN_THRESHOLD))
            : RedstoneControlLogic.MIN_THRESHOLD;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger(TAG_SHUTDOWN_THRESHOLD, shutdownThreshold);
    }
}
