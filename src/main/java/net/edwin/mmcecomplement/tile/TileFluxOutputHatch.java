package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.ItemStack;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.api.tiles.IFluxPlug;

/**
 * Wireless Flux Output Hatch tile entity.
 *
 * <p>Acts as a Flux Networks Plug (buffer → network) and as a Modular
 * Machinery {@link IOType#OUTPUT} energy hatch — MMCE machines push their
 * generated energy into the internal buffer and the Flux Network drains it
 * each transfer cycle to distribute among the network's points.
 */
public class TileFluxOutputHatch extends TileFluxHatchBase implements IFluxPlug {

    public static final String DEFAULT_NAME = "Wireless Flux Output Hatch";

    @Override
    public String getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.PLUG;
    }

    @Override
    public ItemStack getDisplayStack() {
        return new ItemStack(ModBlocks.FLUX_OUTPUT_HATCH);
    }

    @Override
    protected long computeRequest() {
        // Plugs do not pull from an external supplier — MM machines push
        // directly into the buffer via receiveEnergy(). The network drains
        // the buffer via removeFromBuffer() each cycle.
        return 0L;
    }

    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.OUTPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileFluxOutputHatch.this;
            }
        };
    }

    @Override
    public boolean receiveEnergy(long amount) {
        long max = getBufferCapacityRaw();
        long cur = buffer.get();
        if (cur + amount > max) {
            return false;
        }
        buffer.addAndGet(amount);
        return true;
    }

    @Override
    public boolean extractEnergy(long amount) {
        // Output hatch — MM machines only fill it; the network drains it.
        return false;
    }
}
