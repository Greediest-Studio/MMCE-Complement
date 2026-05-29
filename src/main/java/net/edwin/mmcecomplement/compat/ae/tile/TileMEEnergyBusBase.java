package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IItemDefinition;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import dev.beecube31.crazyae2.core.CrazyAE;
import dev.beecube31.crazyae2.core.api.storage.energy.IEnergyStorageChannel;
import github.kasuminova.mmce.common.tile.SettingsTransfer;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.edwin.mmcecomplement.compat.ae.AEEnergyBusTransferUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

public abstract class TileMEEnergyBusBase extends MEMachineComponent implements ITickable, IGridTickable, IEnergyHandlerAsync, SettingsTransfer {

    private static final String TAG_BUFFER = "buffer";
    private static final String TAG_BUFFER_CAPACITY = "bufferCapacity";
    private static final String TAG_GUI = "gui";
    private static final long DEFAULT_BUFFER_CAPACITY = 1_000_000L;

    protected final AtomicLong buffer = new AtomicLong();
    protected long bufferCapacity = DEFAULT_BUFFER_CAPACITY;

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.buffer.set(Math.max(0L, compound.getLong(TAG_BUFFER)));
        if (compound.hasKey(TAG_BUFFER_CAPACITY)) {
            this.bufferCapacity = clampBufferCapacity(compound.getLong(TAG_BUFFER_CAPACITY));
        } else {
            this.bufferCapacity = DEFAULT_BUFFER_CAPACITY;
        }
        if (this.buffer.get() > this.bufferCapacity) {
            this.buffer.set(this.bufferCapacity);
        }
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setLong(TAG_BUFFER, this.buffer.get());
        compound.setLong(TAG_BUFFER_CAPACITY, this.bufferCapacity);
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(1, 20, !shouldRequestWork(), true);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        if (!proxy.isActive()) {
            return TickRateModulation.IDLE;
        }

        try {
            IMEMonitor<IAEItemStack> inventory = proxy.getStorage().getInventory(getChannel());
            long moved = transferEnergy(inventory);
            return moved > 0L ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        }
    }

    @Nullable
    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(getIOType()) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileMEEnergyBusBase.this;
            }
        };
    }

    public long getBufferCapacity() {
        return bufferCapacity;
    }

    public long getRemainingCapacity() {
        return Math.max(0L, bufferCapacity - buffer.get());
    }

    public void setBufferCapacityRaw(long value) {
        long clamped = clampBufferCapacity(value);
        this.bufferCapacity = clamped;
        if (buffer.get() > clamped) {
            buffer.set(clamped);
        }
        markNoUpdateSync();
        alertGridIfNeeded();
    }

    @Override
    public long getCurrentEnergy() {
        return buffer.get();
    }

    @Override
    public void setCurrentEnergy(long energy) {
        this.buffer.set(clampBuffer(energy));
        markNoUpdateSync();
        alertGridIfNeeded();
    }

    @Override
    public boolean extractEnergy(long extract) {
        if (!canExtractToMachine() || extract < 0L) {
            return false;
        }

        synchronized (this) {
            if (buffer.get() < extract) {
                return false;
            }
            buffer.addAndGet(-extract);
        }

        markNoUpdateSync();
        alertGridIfNeeded();
        return true;
    }

    @Override
    public boolean receiveEnergy(long receive) {
        if (!canReceiveFromMachine() || receive < 0L) {
            return false;
        }

        synchronized (this) {
            if (getRemainingCapacity() < receive) {
                return false;
            }
            buffer.addAndGet(receive);
        }

        markNoUpdateSync();
        alertGridIfNeeded();
        return true;
    }

    @Override
    public long getMaxEnergy() {
        return bufferCapacity;
    }

    @Override
    public NBTTagCompound downloadSettings() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong(TAG_BUFFER_CAPACITY, bufferCapacity);
        return tag;
    }

    @Override
    public void uploadSettings(NBTTagCompound settings) {
        if (settings != null && settings.hasKey(TAG_BUFFER_CAPACITY)) {
            setBufferCapacityRaw(settings.getLong(TAG_BUFFER_CAPACITY));
        }
    }

    public NBTTagCompound writeGuiData() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong(TAG_BUFFER, getCurrentEnergy());
        tag.setLong(TAG_BUFFER_CAPACITY, getBufferCapacity());
        tag.setBoolean("powered", isPowered());
        tag.setBoolean("active", isActive());
        tag.setString("mode", getIOType().name());
        return tag;
    }

    public void readGuiData(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(TAG_GUI)) {
            return;
        }
        NBTTagCompound gui = tag.getCompoundTag(TAG_GUI);
        this.buffer.set(Math.max(0L, gui.getLong(TAG_BUFFER)));
        this.bufferCapacity = clampBufferCapacity(gui.getLong(TAG_BUFFER_CAPACITY));
    }

    public void writeGuiSyncTag(NBTTagCompound tag) {
        tag.setTag(TAG_GUI, writeGuiData());
    }

    protected long clampBuffer(long value) {
        return Math.max(0L, Math.min(value, bufferCapacity));
    }

    protected long clampBufferCapacity(long value) {
        return Math.max(0L, value);
    }

    protected void alertGridIfNeeded() {
        if (!shouldRequestWork()) {
            return;
        }

        World currentWorld = this.world;
        if (currentWorld == null || currentWorld.isRemote) {
            return;
        }

        Runnable alertTask = () -> {
            if (isInvalid()) {
                return;
            }
            try {
                proxy.getTick().alertDevice(proxy.getNode());
            } catch (GridAccessException ignored) {
            }
        };

        if (currentWorld instanceof WorldServer) {
            WorldServer serverWorld = (WorldServer) currentWorld;
            if (serverWorld.isCallingFromMinecraftThread()) {
                alertTask.run();
            } else {
                serverWorld.addScheduledTask(alertTask);
            }
            return;
        }

        alertTask.run();
    }

    protected long transferEnergy(IMEMonitor<IAEItemStack> inventory) {
        if (inventory == null) {
            return 0L;
        }

        long moved = doTransfer(inventory);
        if (moved > 0L) {
            markNoUpdateSync();
        }
        return moved;
    }

    protected IAEItemStack createEnergyStack(long amount) {
        if (amount <= 0L) {
            return null;
        }
        IItemDefinition definition = CrazyAE.definitions().items().FEEnergyAsAeStack();
        return AEEnergyBusTransferUtil.createAEStack(definition, amount);
    }

    protected IEnergyStorageChannel getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEnergyStorageChannel.class);
    }

    protected long importFromME(IMEMonitor<IAEItemStack> inventory, long amount) {
        IAEItemStack request = createEnergyStack(amount);
        IAEItemStack extracted = AEEnergyBusTransferUtil.extractFromME(inventory, request, source, Actionable.MODULATE);
        long moved = extracted == null ? 0L : extracted.getStackSize();
        if (moved > 0L) {
            buffer.addAndGet(moved);
        }
        return moved;
    }

    protected long exportToME(IMEMonitor<IAEItemStack> inventory, long amount) {
        IAEItemStack stack = createEnergyStack(amount);
        IAEItemStack left = AEEnergyBusTransferUtil.injectToME(inventory, stack, source, Actionable.MODULATE);
        long moved = amount - (left == null ? 0L : left.getStackSize());
        if (moved > 0L) {
            buffer.addAndGet(-moved);
        }
        return moved;
    }

    protected abstract IOType getIOType();

    protected abstract boolean canExtractToMachine();

    protected abstract boolean canReceiveFromMachine();

    protected abstract boolean shouldRequestWork();

    protected abstract long doTransfer(IMEMonitor<IAEItemStack> inventory);
}