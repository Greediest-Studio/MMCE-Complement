package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneSignalLogic;
import net.minecraft.util.ITickable;

import java.util.concurrent.atomic.AtomicBoolean;

/** Emits the latest value committed by its bound controller event tick. */
public class TileRedstoneSignalOutputHatch extends TileRedstoneInterfaceHatch
    implements ITickable {

    private final AtomicBoolean updateQueued = new AtomicBoolean();
    private volatile int requestedSignal;
    private volatile int outputSignal;
    private volatile long lastRequestWorldTick = Long.MIN_VALUE;

    public int getOutputSignal() {
        return outputSignal;
    }

    public void setOutputSignal(int signal) {
        requestedSignal = RedstoneSignalLogic.clampOutput(signal);
        if (world != null) {
            lastRequestWorldTick = world.getTotalWorldTime();
        }
        if (world == null || world.isRemote) {
            outputSignal = requestedSignal;
            return;
        }
        if (world.getMinecraftServer() != null
            && world.getMinecraftServer().isCallingFromMinecraftThread()) {
            applyRequestedSignal();
            return;
        }
        if (updateQueued.compareAndSet(false, true)) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
                updateQueued.set(false);
                applyRequestedSignal();
            });
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || outputSignal == 0) {
            return;
        }
        // A setRedstone call is scoped to its controller event tick. Permit
        // one scheduling tick of slack for MMCE's async work modes, then drop
        // an output that is no longer being refreshed.
        if (getBoundController() == null
            || world.getTotalWorldTime() - lastRequestWorldTick > 1L) {
            setOutputSignal(0);
        }
    }

    @Override
    protected void onUnbound() {
        setOutputSignal(0);
    }

    private void applyRequestedSignal() {
        if (world == null || world.isRemote || isInvalid()) {
            return;
        }
        int next = requestedSignal;
        if (outputSignal == next) {
            return;
        }
        outputSignal = next;
        markNoUpdate();
        world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
        world.notifyNeighborsOfStateChange(pos.down(), getBlockType(), false);
    }
}
