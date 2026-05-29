package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.EnergyHatchData;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.api.network.ITransferHandler;
import sonar.fluxnetworks.api.tiles.IFluxConnector;
import sonar.fluxnetworks.api.utils.Coord4D;
import sonar.fluxnetworks.api.utils.NBTType;
import sonar.fluxnetworks.common.connection.FluxNetworkCache;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared base for the Wireless Flux Input / Output Hatches.
 *
 * <p>Holds all the bookkeeping (network ID, custom name, priority, transfer
 * limit, surge / disable-limit / chunk-loading flags, NBT serialisation, the
 * Flux Networks connector contract and the internal energy buffer) that both
 * variants share. Subclasses only have to declare which direction they move
 * energy in by overriding a handful of hooks.
 */
public abstract class TileFluxHatchBase extends TileColorableMachineComponent
        implements IFluxConnector, MachineComponentTile, IEnergyHandlerAsync, ITickable {

    protected final AtomicLong buffer = new AtomicLong(0L);
    protected long transferChange = 0L;
    protected long lastCycleChange = 0L;

    protected int networkID = -1;
    protected IFluxNetwork network = null;

    protected UUID playerUUID = null;
    protected String customName = "";
    protected int priority = 0;
    protected long transferLimit = Long.MAX_VALUE;
    protected boolean disableLimit = true;
    protected boolean surgeMode = false;
    protected int folderID = -1;

    protected EnergyHatchData tier = EnergyHatchData.LUDICROUS;
    protected long bufferCapacity = tier.maxEnergy;

    protected boolean chunkLoaded = true;
    protected boolean chunkLoadingRequested = false;

    // ---- Subclass hooks ------------------------------------------------

    /** Default English name displayed when the player has not set a custom one. */
    public abstract String getDefaultName();

    /** {@link ConnectionType#POINT} for the input hatch, {@link ConnectionType#PLUG} for the output hatch. */
    @Override
    public abstract ConnectionType getConnectionType();

    /** Item form of this hatch — used by FN's connection list display. */
    @Override
    public abstract ItemStack getDisplayStack();

    /** The {@link MachineComponent.EnergyHatch} this tile exposes to Modular Machinery. */
    @Override
    public abstract MachineComponent<?> provideComponent();

    /** Cycle request — for points: room left to receive from the network; for plugs: 0. */
    protected abstract long computeRequest();

    // ---- Tile lifecycle ------------------------------------------------

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        if (networkID > 0 && (network == null || network.isInvalid())) {
            IFluxNetwork net = FluxNetworkCache.instance.getNetwork(networkID);
            if (net != null && !net.isInvalid()) {
                net.queueConnectionAddition(this);
                network = net;
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (network != null) {
            network.queueConnectionRemoval(this, false);
            network = null;
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (network != null) {
            network.queueConnectionRemoval(this, true);
        }
    }

    // ---- NBT (TileColorableMachineComponent overrides) -----------------

    @Override
    public void readCustomNBT(NBTTagCompound nbt) {
        super.readCustomNBT(nbt);
        readFluxNBT(nbt);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound nbt) {
        super.writeCustomNBT(nbt);
        writeFluxNBT(nbt);
    }

    private void readFluxNBT(NBTTagCompound nbt) {
        networkID = nbt.getInteger("networkID");
        priority = nbt.getInteger("priority");
        transferLimit = nbt.hasKey("limit") ? nbt.getLong("limit") : Long.MAX_VALUE;
        bufferCapacity = nbt.hasKey("bufferCapacity") ? nbt.getLong("bufferCapacity") : tier.maxEnergy;
        lastCycleChange = nbt.hasKey("fnChange") ? nbt.getLong("fnChange") : 0L;
        disableLimit = !nbt.hasKey("disableLimit") || nbt.getBoolean("disableLimit");
        surgeMode = nbt.getBoolean("surgeMode");
        chunkLoadingRequested = nbt.getBoolean("chunkLoading");
        folderID = nbt.hasKey("folderID") ? nbt.getInteger("folderID") : -1;
        customName = nbt.getString("customName");
        buffer.set(nbt.getLong("buffer"));
        if (nbt.hasKey("mmTier")) {
            int idx = nbt.getInteger("mmTier");
            EnergyHatchData[] values = EnergyHatchData.values();
            if (idx >= 0 && idx < values.length) {
                tier = values[idx];
            }
        }
        if (nbt.hasUniqueId("owner")) {
            playerUUID = nbt.getUniqueId("owner");
        }
        clampBufferCapacityAndStoredEnergy();
    }

    private void writeFluxNBT(NBTTagCompound nbt) {
        nbt.setInteger("networkID", networkID);
        nbt.setInteger("priority", priority);
        nbt.setLong("limit", transferLimit);
        nbt.setLong("bufferCapacity", bufferCapacity);
        nbt.setLong("fnChange", lastCycleChange);
        nbt.setBoolean("disableLimit", disableLimit);
        nbt.setBoolean("chunkLoading", chunkLoadingRequested);
        nbt.setBoolean("surgeMode", surgeMode);
        nbt.setInteger("folderID", folderID);
        nbt.setString("customName", customName);
        nbt.setLong("buffer", buffer.get());
        nbt.setInteger("mmTier", tier.ordinal());
        if (playerUUID != null) {
            nbt.setUniqueId("owner", playerUUID);
        }
    }

    // ---- IFluxConnector ------------------------------------------------

    @Override
    public NBTTagCompound writeCustomNBT(NBTTagCompound nbt, NBTType type) {
        writeFluxNBT(nbt);
        return nbt;
    }

    @Override
    public void readCustomNBT(NBTTagCompound nbt, NBTType type) {
        readFluxNBT(nbt);
    }

    @Override
    public int getLogicPriority() {
        return priority;
    }

    @Override
    public int getRawPriority() {
        return priority;
    }

    @Override
    public UUID getConnectionOwner() {
        return playerUUID;
    }

    @Override
    public boolean canAccess(EntityPlayer player) {
        return true;
    }

    @Override
    public long getLogicLimit() {
        return disableLimit ? Long.MAX_VALUE : transferLimit;
    }

    @Override
    public long getRawLimit() {
        return transferLimit;
    }

    @Override
    public long getMaxTransferLimit() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isActive() {
        return network != null && !network.isInvalid();
    }

    @Override
    public boolean isChunkLoaded() {
        return chunkLoaded;
    }

    @Override
    public void setChunkLoaded(boolean loaded) {
        this.chunkLoaded = loaded;
    }

    @Override
    public boolean isForcedLoading() {
        return false;
    }

    @Override
    public void connect(IFluxNetwork network) {
        this.network = network;
        this.networkID = network.getNetworkID();
        markDirty();
    }

    @Override
    public void disconnect(IFluxNetwork network) {
        if (this.network == network) {
            this.network = null;
            this.networkID = -1;
            markDirty();
        }
    }

    @Override
    public ITransferHandler getTransferHandler() {
        return transferHandler;
    }

    @Override
    public net.minecraft.world.World getFluxWorld() {
        return world;
    }

    @Override
    public Coord4D getCoords() {
        return new Coord4D(this);
    }

    @Override
    public int getFolderID() {
        return folderID;
    }

    @Override
    public String getCustomName() {
        return (customName == null || customName.isEmpty()) ? getDefaultName() : customName;
    }

    /** Raw stored custom name (may be empty when the player hasn't set one). */
    public String getRawCustomName() {
        return customName == null ? "" : customName;
    }

    // ---- Server-side mutators driven by NetworkHandlerMMCE -------------

    public void setCustomNameRaw(String name) {
        this.customName = name == null ? "" : name;
        markDirty();
    }

    public void setPriorityRaw(int p) {
        this.priority = p;
        markDirty();
    }

    public void setTransferLimitRaw(long l) {
        this.transferLimit = Math.max(0L, Math.min(l, getMaxTransferLimit()));
        markDirty();
    }

    public void setBufferCapacityRaw(long value) {
        this.bufferCapacity = Math.max(0L, Math.min(value, getBufferCapacityCeiling()));
        clampBufferCapacityAndStoredEnergy();
        markDirty();
    }

    public long getBufferCapacityRaw() {
        return bufferCapacity;
    }

    public long getBufferCapacityCeiling() {
        return tier.maxEnergy;
    }

    public void setSurgeModeRaw(boolean v) {
        this.surgeMode = v;
        markDirty();
    }

    public void setDisableLimitRaw(boolean v) {
        this.disableLimit = v;
        markDirty();
    }

    public void setChunkLoadingRequested(boolean v) {
        this.chunkLoadingRequested = v;
        markDirty();
    }

    public boolean isChunkLoadingRequested() {
        return chunkLoadingRequested;
    }

    @Override
    public boolean getDisableLimit() {
        return disableLimit;
    }

    @Override
    public boolean getSurgeMode() {
        return surgeMode;
    }

    @Override
    public long getTransferBuffer() {
        return buffer.get();
    }

    @Override
    public long getTransferChange() {
        return lastCycleChange;
    }

    // ---- INetworkConnector --------------------------------------------

    @Override
    public int getNetworkID() {
        return networkID;
    }

    @Override
    public IFluxNetwork getNetwork() {
        // On the client we don't run the server-side connection bookkeeping
        // in update(), so resolve lazily from the client-side network cache
        // whenever the GUI asks for our network.
        if (world != null && world.isRemote
                && networkID > 0
                && (network == null || network.isInvalid())) {
            IFluxNetwork client = FluxNetworkCache.instance.getClientNetwork(networkID);
            if (client != null && !client.isInvalid()) {
                network = client;
            }
        }
        return network;
    }

    @Override
    public void open(EntityPlayer player) {
        // No-op: dedicated FN-style container not used.
    }

    @Override
    public void close(EntityPlayer player) {
        // No-op.
    }

    // ---- ITransferHandler delegate -------------------------------------

    private final ITransferHandler transferHandler = new ITransferHandler() {
        @Override
        public void onCycleStart() {
            // reset accumulated at cycle end; nothing needed here
        }

        @Override
        public void onCycleEnd() {
            lastCycleChange = transferChange;
            transferChange = 0L;
            markForUpdateSync();
        }

        @Override
        public long getBuffer() {
            return buffer.get();
        }

        @Override
        public long getRequest() {
            return computeRequest();
        }

        @Override
        public long getChange() {
            return lastCycleChange;
        }

        @Override
        public void addToBuffer(long amount) {
            long max = getBufferCapacityRaw();
            long cur = buffer.get();
            long add = Math.min(amount, max - cur);
            if (add > 0L) {
                buffer.addAndGet(add);
                // FN expects points/controllers to report outgoing transfer as negative.
                transferChange -= add;
            }
        }

        @Override
        public long removeFromBuffer(long amount) {
            long cur = buffer.get();
            long take = Math.min(amount, cur);
            if (take > 0L) {
                buffer.addAndGet(-take);
                // FN expects plugs to report injected transfer as positive.
                transferChange += take;
            }
            return take;
        }

        @Override
        public long receiveFromSupplier(long amount, EnumFacing side, boolean simulate) {
            return 0L;
        }

        @Override
        public void writeCustomNBT(NBTTagCompound nbt, NBTType type) {
            nbt.setLong("buffer", buffer.get());
            nbt.setLong("fnChange", lastCycleChange);
        }

        @Override
        public void readCustomNBT(NBTTagCompound nbt, NBTType type) {
            buffer.set(nbt.getLong("buffer"));
            lastCycleChange = nbt.getLong("fnChange");
        }

        @Override
        public void updateTransfers(EnumFacing... faces) {
            // No-op.
        }

        @Override
        public void reset() {
            transferChange = 0L;
            lastCycleChange = 0L;
        }
    };

    // ---- IEnergyHandlerAsync (MM) shared parts -------------------------

    @Override
    public long getCurrentEnergy() {
        return buffer.get();
    }

    @Override
    public void setCurrentEnergy(long value) {
        long clamped = Math.max(0L, Math.min(value, getBufferCapacityRaw()));
        buffer.set(clamped);
    }

    @Override
    public long getMaxEnergy() {
        return getBufferCapacityRaw();
    }

    // ---- Public helpers ------------------------------------------------

    public EnergyHatchData getTier() {
        return tier;
    }

    public void setTier(EnergyHatchData tier) {
        this.tier = tier;
        clampBufferCapacityAndStoredEnergy();
    }

    private void clampBufferCapacityAndStoredEnergy() {
        long maxAllowed = getBufferCapacityCeiling();
        bufferCapacity = Math.max(0L, Math.min(bufferCapacity, maxAllowed));
        long cur = buffer.get();
        if (cur > bufferCapacity) {
            buffer.set(bufferCapacity);
        }
    }
}
