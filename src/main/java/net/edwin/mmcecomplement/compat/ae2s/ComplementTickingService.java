package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.networking.IGridNode;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;

/** Runs Complement's inventory/reserve logic from the real AE2S scheduler. */
public final class ComplementTickingService implements IGridTickable {
    private final MEMachineComponent tile;
    private final appeng.api.networking.ticking.IGridTickable ticking;
    public ComplementTickingService(MEMachineComponent tile) {
        this.tile = tile;
        ticking = (appeng.api.networking.ticking.IGridTickable) tile;
    }
    @Override public TickingRequest getTickingRequest(IGridNode node) { return new TickingRequest(10, 120, false); }
    @Override public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return TickRateModulation.valueOf(ticking.tickingRequest(AE2SNetwork.getNode(tile), ticksSinceLastCall).name());
    }
}
