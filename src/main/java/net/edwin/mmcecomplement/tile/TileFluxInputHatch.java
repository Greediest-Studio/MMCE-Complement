package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.api.tiles.IFluxPoint;

/**
 * Wireless Flux Input Hatch tile entity.
 *
 * <p>Acts as a Flux Networks Point (network → buffer) and as a Modular
 * Machinery {@link IOType#INPUT} energy hatch — MMCE machines drain the
 * internal buffer that the network keeps topped up.
 */
public class TileFluxInputHatch extends TileFluxHatchBase implements IFluxPoint {

    public static final String DEFAULT_NAME = "Wireless Flux Input Hatch";

    @Override
    public String getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.POINT;
    }

    @Override
    public ItemStack getDisplayStack() {
        return new ItemStack(ModBlocks.FLUX_INPUT_HATCH);
    }

    @Override
    protected long computeRequest() {
        long max = tier.maxEnergy;
        long cur = buffer.get();
        long room = max - cur;
        if (room <= 0L) {
            return 0L;
        }
        return Math.min(room, getLogicLimit());
    }

    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.INPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileFluxInputHatch.this;
            }
        };
    }

    @Override
    public boolean receiveEnergy(long amount) {
        // Input hatch — MM machines only drain it; the network fills the buffer.
        return false;
    }

    @Override
    public boolean extractEnergy(long amount) {
        long cur = buffer.get();
        if (cur < amount) {
            return false;
        }
        buffer.addAndGet(-amount);
        return true;
    }
}
