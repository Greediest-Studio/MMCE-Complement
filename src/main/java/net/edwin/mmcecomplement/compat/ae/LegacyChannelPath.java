package net.edwin.mmcecomplement.compat.ae;

import appeng.api.networking.*;
import appeng.api.networking.pathing.*;
import appeng.me.GridNode;
import appeng.me.GridConnection;
import appeng.me.cache.PathGridCache;
import appeng.me.pathfinding.IPathItem;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.mechannel.MEChannelPathSnapshot;
import java.util.*;
import static net.edwin.mmcecomplement.compat.ae.AeNetworkCompat.normalCapacity;
import static net.edwin.mmcecomplement.compat.ae.AeNetworkCompat.denseCapacity;

/** Old AE2UEL internals are resolved only on that branch. */
public final class LegacyChannelPath {
    private LegacyChannelPath() { }

    public static void channelChanged(IGridNode node) {
        if (node.getGrid() != null) {
            node.getGrid().postEvent(new appeng.api.networking.events.MENetworkChannelChanged(node));
        }
    }

    public static void repath(IGrid grid) {
        IPathingGrid path = grid.getCache(IPathingGrid.class);
        if (path != null) path.repath();
    }

    public static MEChannelPathSnapshot snapshot(MEMachineComponent tile) {
        IGridNode apiNode = tile.getProxy().getNode();
        if (!(apiNode instanceof GridNode) || apiNode.getGrid() == null
            || !tile.getProxy().isPowered()) {
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
            capacities.add(new MEChannelPathSnapshot.Capacity(tile,
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

}
