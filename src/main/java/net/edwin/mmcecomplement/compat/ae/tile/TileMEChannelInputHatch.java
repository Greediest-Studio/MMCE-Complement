package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridNotification;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkChannelChanged;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.core.AEConfig;
import appeng.me.GridConnection;
import appeng.me.GridNode;
import appeng.me.cache.PathGridCache;
import appeng.me.pathfinding.IPathItem;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.mechannel.MEChannelPathSnapshot;
import net.edwin.mmcecomplement.mechannel.MEChannelProvider;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * An idle, zero-channel AE endpoint which creates internal channel nodes only
 * while its owning recipe is running.
 */
public class TileMEChannelInputHatch extends MEMachineComponent
    implements MEChannelProvider {

    private final List<ChannelNode> channelNodes = new ArrayList<>();

    private volatile Object reservationOwner;
    private volatile int requestedChannels;
    private volatile boolean applyQueued;

    /** Number of virtual channels currently requested by a running recipe. */
    public int getRequestedChannels() {
        return requestedChannels;
    }

    public TileMEChannelInputHatch() {
        // The visible/world node carries channels but does not consume one.
        proxy.setFlags(GridFlags.DENSE_CAPACITY);
        proxy.setIdlePowerUsage(1.0D);
    }

    @Override
    public ItemStack getVisualItemStack() {
        if (ModBlocks.ME_CHANNEL_INPUT_HATCH == null) {
            return ItemStack.EMPTY;
        }
        Item item = Item.getItemFromBlock(ModBlocks.ME_CHANNEL_INPUT_HATCH);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public MachineComponent<MEChannelProvider> provideComponent() {
        return new MachineComponent<MEChannelProvider>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ModMEChannelTypes.COMPONENT;
            }

            @Override
            public MEChannelProvider getContainerProvider() {
                return TileMEChannelInputHatch.this;
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }
        };
    }

    @Override
    public Object getMEChannelNetworkIdentity() {
        IGridNode node = proxy.getNode();
        if (node == null || node.getGrid() == null || !proxy.isPowered()) {
            return null;
        }
        return node.getGrid();
    }

    @Override
    public MEChannelPathSnapshot snapshotMEChannelPath() {
        IGridNode apiNode = proxy.getNode();
        if (!(apiNode instanceof GridNode) || apiNode.getGrid() == null
            || !proxy.isPowered()) {
            return MEChannelPathSnapshot.unavailable(
                MEChannelPathSnapshot.State.DISCONNECTED,
                apiNode == null ? null : apiNode.getGrid());
        }

        IGrid grid = apiNode.getGrid();
        IPathingGrid apiPath = grid.getCache(IPathingGrid.class);
        if (apiPath == null || apiPath.isNetworkBooting()) {
            return MEChannelPathSnapshot.unavailable(
                MEChannelPathSnapshot.State.BOOTING, grid);
        }
        ControllerState controllerState = apiPath.getControllerState();
        if (controllerState == ControllerState.CONTROLLER_CONFLICT) {
            return MEChannelPathSnapshot.unavailable(
                MEChannelPathSnapshot.State.CONTROLLER_CONFLICT, grid);
        }

        List<MEChannelPathSnapshot.Capacity> capacities = new ArrayList<>();
        if (controllerState == ControllerState.NO_CONTROLLER) {
            int used = apiPath instanceof PathGridCache
                ? ((PathGridCache) apiPath).getChannelsInUse()
                : ((GridNode) apiNode).usedChannels();
            capacities.add(new MEChannelPathSnapshot.Capacity(grid,
                normalCapacity() - used));
            capacities.add(new MEChannelPathSnapshot.Capacity(this,
                denseCapacity() - ((GridNode) apiNode).usedChannels()));
            return MEChannelPathSnapshot.ready(grid, capacities);
        }

        IPathItem cursor = (IPathItem) apiNode;
        if (cursor.getControllerRoute() == null) {
            return MEChannelPathSnapshot.unavailable(
                MEChannelPathSnapshot.State.BOOTING, grid);
        }
        IdentityHashMap<IPathItem, Boolean> visited = new IdentityHashMap<>();
        while (cursor != null && visited.put(cursor, Boolean.TRUE) == null) {
            addCapacity(cursor, capacities);
            cursor = cursor.getControllerRoute();
        }
        if (capacities.isEmpty()) {
            return MEChannelPathSnapshot.unavailable(
                MEChannelPathSnapshot.State.DISCONNECTED, grid);
        }
        return MEChannelPathSnapshot.ready(grid, capacities);
    }

    private static void addCapacity(
        IPathItem pathItem,
        List<MEChannelPathSnapshot.Capacity> capacities) {
        if (pathItem instanceof GridNode) {
            GridNode node = (GridNode) pathItem;
            EnumSet<GridFlags> flags = node.getFlags();
            if (flags.contains(GridFlags.CANNOT_CARRY)) {
                return;
            }
            int maximum = flags.contains(GridFlags.DENSE_CAPACITY)
                ? denseCapacity() : normalCapacity();
            capacities.add(new MEChannelPathSnapshot.Capacity(node,
                maximum - node.usedChannels()));
        } else if (pathItem instanceof GridConnection) {
            GridConnection connection = (GridConnection) pathItem;
            capacities.add(new MEChannelPathSnapshot.Capacity(connection,
                denseCapacity() - connection.getUsedChannels()));
        }
    }

    @Override
    public synchronized boolean requestMEChannels(Object owner, int amount) {
        if (owner == null || amount <= 0 || amount > denseCapacity()) {
            return false;
        }
        if (reservationOwner != null && reservationOwner != owner
            && requestedChannels > 0) {
            return false;
        }
        reservationOwner = owner;
        requestedChannels = amount;
        queueApply();
        return true;
    }

    @Override
    public synchronized void releaseMEChannels(Object owner) {
        if (owner == null || reservationOwner != owner) {
            return;
        }
        requestedChannels = 0;
        reservationOwner = null;
        queueApply();
    }

    @Override
    public synchronized boolean isMEChannelReservationSatisfied(
        Object owner, int amount) {
        if (owner == null || reservationOwner != owner
            || amount <= 0 || requestedChannels != amount
            || channelNodes.size() < amount) {
            queueApply();
            return false;
        }
        IGridNode main = proxy.getNode();
        IGrid grid = main == null ? null : main.getGrid();
        if (grid == null || !proxy.isPowered()) {
            return false;
        }
        for (int i = 0; i < amount; i++) {
            IGridNode node = channelNodes.get(i).node;
            if (node == null || node.getGrid() != grid || !node.isActive()
                || !node.meetsChannelRequirements()) {
                return false;
            }
        }
        return true;
    }

    private synchronized void queueApply() {
        if (world == null || world.isRemote || isInvalid() || applyQueued) {
            return;
        }
        applyQueued = true;
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::applyReservation);
    }

    private void applyReservation() {
        int target;
        synchronized (this) {
            applyQueued = false;
        }
        if (world == null || world.isRemote || isInvalid()) {
            return;
        }

        // Releasing must also work while AE is rebuilding or the visible
        // node is temporarily detached.  Destroying the internal endpoints
        // immediately guarantees that an ended/cancelled recipe consumes no
        // channels on either side of a network split.
        synchronized (this) {
            if (requestedChannels == 0) {
                destroyChannelNodes();
                return;
            }
        }

        IGridNode main = proxy.getNode();
        if (main == null || main.getGrid() == null) {
            // Do not enqueue ourselves from inside MMCE's synchronous-task
            // drain loop: that queue is consumed until empty, so doing so
            // would spin forever while AE is not ready.  doIOTick calls
            // isMEChannelReservationSatisfied every tick and queues the next
            // safe attempt.
            return;
        }

        synchronized (this) {
            // The request can be changed by an asynchronous recipe thread
            // while this main-thread action is waiting for AE.  Always apply
            // the latest value, not the earlier snapshot.
            target = requestedChannels;
            if (target == 0) {
                destroyChannelNodes();
                return;
            }
            while (channelNodes.size() < target) {
                if (!createChannelNode(main, channelNodes.size())) {
                    break;
                }
            }

            for (int i = 0; i < channelNodes.size(); i++) {
                ChannelNode channelNode = channelNodes.get(i);
                boolean changed = channelNode.block.setEnabled(i < target);
                if (changed && channelNode.node.getGrid() != null) {
                    channelNode.node.getGrid().postEvent(
                        new MENetworkChannelChanged(channelNode.node));
                }
            }
        }

        IGrid currentGrid = main.getGrid();
        if (currentGrid != null) {
            IPathingGrid path = currentGrid.getCache(IPathingGrid.class);
            if (path != null) {
                path.repath();
            }
        }
    }

    private boolean createChannelNode(IGridNode main, int index) {
        VirtualChannelGridBlock block = new VirtualChannelGridBlock(index,
            index < requestedChannels);
        IGridNode node = AEApi.instance().grid().createGridNode(block);
        try {
            node.setPlayerID(main.getPlayerID());
            node.updateState();
            AEApi.instance().grid().createGridConnection(main, node);
            channelNodes.add(new ChannelNode(block, node));
            return true;
        } catch (FailedConnectionException | RuntimeException ex) {
            node.destroy();
            return false;
        }
    }

    private synchronized void destroyChannelNodes() {
        requestedChannels = 0;
        reservationOwner = null;
        applyQueued = false;
        for (ChannelNode channelNode : channelNodes) {
            if (channelNode.node != null) {
                channelNode.node.destroy();
            }
        }
        channelNodes.clear();
    }

    @Override
    public void onChunkUnload() {
        destroyChannelNodes();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        destroyChannelNodes();
        super.invalidate();
    }

    private static int normalCapacity() {
        return Math.max(0, AEConfig.instance().getNormalChannelCapacity());
    }

    private static int denseCapacity() {
        return Math.max(0, AEConfig.instance().getDenseChannelCapacity());
    }

    private final class VirtualChannelGridBlock implements IGridBlock {
        private final int index;
        private volatile boolean enabled;

        private VirtualChannelGridBlock(int index, boolean enabled) {
            this.index = index;
            this.enabled = enabled;
        }

        private boolean setEnabled(boolean enabled) {
            if (this.enabled == enabled) {
                return false;
            }
            this.enabled = enabled;
            return true;
        }

        @Override public double getIdlePowerUsage() { return 0D; }

        @Override
        public EnumSet<GridFlags> getFlags() {
            return enabled ? EnumSet.of(GridFlags.REQUIRE_CHANNEL)
                : EnumSet.noneOf(GridFlags.class);
        }

        @Override public boolean isWorldAccessible() { return false; }
        @Override public DimensionalCoord getLocation() {
            return new DimensionalCoord(TileMEChannelInputHatch.this);
        }
        @Override public AEColor getGridColor() { return AEColor.TRANSPARENT; }
        @Override public void onGridNotification(GridNotification notification) { }
        @Override public void setNetworkStatus(IGrid grid, int channelsInUse) { }
        @Override public EnumSet<EnumFacing> getConnectableSides() {
            return EnumSet.noneOf(EnumFacing.class);
        }
        @Override public IGridHost getMachine() {
            return TileMEChannelInputHatch.this;
        }
        @Override public void gridChanged() { }
        @Override public ItemStack getMachineRepresentation() {
            return getVisualItemStack();
        }

        @Override
        public String toString() {
            return "MEChannelNode{" + pos + '#' + index + '}';
        }
    }

    private final class ChannelNode {
        private final VirtualChannelGridBlock block;
        private final IGridNode node;

        private ChannelNode(VirtualChannelGridBlock block, IGridNode node) {
            this.block = block;
            this.node = node;
        }
    }
}
