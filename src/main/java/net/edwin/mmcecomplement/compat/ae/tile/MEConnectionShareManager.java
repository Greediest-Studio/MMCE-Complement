package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridNotification;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.core.AEConfig;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Maintains the extra AE links made by ME connection sharing hatches.  Links
 * are deliberately owned by this manager so that a structure rebuild can
 * remove only links created by the sharing feature.
 */
public final class MEConnectionShareManager {
    private static final Map<TileMultiblockMachineController, State> STATES =
        Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<TileMultiblockMachineController> PENDING_REFRESH =
        Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<TileMEConnectionShareHatch,
        Set<TileMultiblockMachineController>> OWNERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private MEConnectionShareManager() { }

    /** Queues AE graph mutations for the server thread and coalesces repeats. */
    public static synchronized void scheduleRefresh(
        final TileMultiblockMachineController controller) {
        if (!PENDING_REFRESH.add(controller)) return;
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(() -> {
            synchronized (MEConnectionShareManager.class) {
                PENDING_REFRESH.remove(controller);
            }
            if (controller.isInvalid() || controller.getWorld() == null
                || controller.getWorld().isRemote) return;
            refresh(controller);
        });
    }

    public static synchronized void scheduleClear(
        final TileMultiblockMachineController controller) {
        PENDING_REFRESH.remove(controller);
        ModularMachinery.EXECUTE_MANAGER.addSyncTask(
            () -> clear(controller));
    }

    public static synchronized void refresh(TileMultiblockMachineController controller) {
        List<MEMachineComponent> components = collectComponents(controller);
        List<TileMEConnectionShareHatch> shares = new ArrayList<>();
        List<MEMachineComponent> targets = new ArrayList<>();
        for (MEMachineComponent component : components) {
            if (component instanceof TileMEConnectionShareHatch) {
                shares.add((TileMEConnectionShareHatch) component);
            } else {
                targets.add(component);
            }
        }

        IGrid network = null;
        boolean conflict = false;
        for (TileMEConnectionShareHatch share : shares) {
            IGridNode node = share.getProxy().getNode();
            IGrid grid = node == null ? null : node.getGrid();
            if (grid == null || !share.getProxy().isPowered()) continue;
            if (network == null) network = grid;
            else if (network != grid) conflict = true;
        }

        State old = STATES.get(controller);
        if (old != null && old.sameContext(components, network, conflict)
            && old.linksValid()) {
            if (network != null && !conflict) {
                IGridNode sharedNode = findSharedNode(shares, network);
                if (sharedNode != null) {
                    connectTargets(old, components, targets, sharedNode,
                        network);
                    ensureDuplicateChannels(old);
                }
            }
            return;
        }
        STATES.remove(controller);
        if (old != null) {
            unregister(controller, old.shares);
            old.destroy();
        }
        if (shares.isEmpty()) return;

        State state = new State();
        STATES.put(controller, state);
        state.components.addAll(components);
        state.shares.addAll(shares);
        state.conflict = conflict;
        state.network = network;
        register(controller, shares);
        if (network == null || state.conflict) return;

        // Every target is connected at most once.  A target already attached
        // to another AE network is intentionally left untouched.
        IGridNode sharedNode = findSharedNode(shares, network);
        if (sharedNode == null) return;

        connectTargets(state, components, targets, sharedNode, network);
        ensureDuplicateChannels(state);
    }

    private static IGridNode findSharedNode(
        List<TileMEConnectionShareHatch> shares, IGrid network) {
        for (TileMEConnectionShareHatch share : shares) {
            IGridNode node = share.getProxy().getNode();
            if (node != null && node.getGrid() == network) return node;
        }
        return null;
    }

    /**
     * Connects every machine-local AE sub-grid, not just the first isolated
     * node.  Hatches touching each other can already have internal AE
     * connections; those links do not mean that they are wired to an
     * external network.
     */
    private static void connectTargets(State state,
                                       List<MEMachineComponent> components,
                                       List<MEMachineComponent> targets,
                                       IGridNode sharedNode,
                                       IGrid network) {
        Set<IGridNode> machineNodes =
            Collections.newSetFromMap(new IdentityHashMap<>());
        Set<IGridHost> machineHosts =
            Collections.newSetFromMap(new IdentityHashMap<>());
        for (MEMachineComponent component : components) {
            IGridNode node = component.getProxy().getNode();
            if (node != null) machineNodes.add(node);
            machineHosts.add(component);
        }

        // Determine all candidates before mutating AE's graph. Connecting one
        // member can merge its whole local sub-grid into the shared network.
        List<MEMachineComponent> candidates = new ArrayList<>();
        for (MEMachineComponent target : targets) {
            IGridNode node = target.getProxy().getNode();
            if (node == null || node.getGrid() == network) continue;
            if (isMachineLocalGrid(node.getGrid(), machineNodes, machineHosts)) {
                candidates.add(target);
            }
        }

        for (MEMachineComponent target : candidates) {
            IGridNode node = target.getProxy().getNode();
            IGrid currentSharedGrid = sharedNode.getGrid();
            if (node == null || node.getGrid() == currentSharedGrid) continue;
            try {
                IGridConnection connection =
                    AEApi.instance().grid().createGridConnection(sharedNode, node);
                state.links.add(new Link(connection, node, false));
            } catch (FailedConnectionException | RuntimeException ignored) {
                // A node can disappear while AE is rebuilding its pathing
                // grid. A later candidate in the same local sub-grid may
                // still connect it; otherwise the next refresh retries it.
            }
        }

        // AE2 keeps the higher-priority/larger grid object when two grids are
        // merged. A large machine-local hatch group can therefore replace the
        // original shared-grid identity even though the resulting network is
        // correct. Track the identity currently owned by the sharing node.
        state.network = sharedNode.getGrid();

        // One bridge can bring several mutually connected hatches online.
        // Count every candidate that actually reached the shared network.
        for (MEMachineComponent target : candidates) {
            IGridNode node = target.getProxy().getNode();
            if (node != null && node.getGrid() == state.network
                && !containsIdentity(state.replacedComponents, target)) {
                state.replacedComponents.add(target);
            }
        }
    }

    private static boolean isMachineLocalGrid(
        IGrid grid, Set<IGridNode> machineNodes, Set<IGridHost> machineHosts) {
        if (grid == null) return true;
        for (IGridNode node : grid.getNodes()) {
            if (machineNodes.contains(node)) continue;
            IGridHost host = node.getMachine();
            // Virtual channel nodes are hosted by their owning machine hatch
            // and are therefore part of the local machine sub-grid.
            if (host == null || !machineHosts.contains(host)) return false;
        }
        return true;
    }

    private static boolean containsIdentity(
        List<MEMachineComponent> components, MEMachineComponent target) {
        for (MEMachineComponent component : components) {
            if (component == target) return true;
        }
        return false;
    }

    public static synchronized void clear(TileMultiblockMachineController controller) {
        PENDING_REFRESH.remove(controller);
        State state = STATES.remove(controller);
        if (state != null) {
            unregister(controller, state.shares);
            state.destroy();
        }
    }

    /** Called by AE when the visible sharing node changes its network. */
    public static synchronized void markDirty(TileMEConnectionShareHatch hatch) {
        Set<TileMultiblockMachineController> owners = OWNERS.get(hatch);
        if (owners == null || owners.isEmpty()) return;
        for (final TileMultiblockMachineController controller : owners) {
            scheduleRefresh(controller);
        }
    }

    private static void register(TileMultiblockMachineController controller,
                                 List<TileMEConnectionShareHatch> shares) {
        for (TileMEConnectionShareHatch share : shares) {
            OWNERS.computeIfAbsent(share,
                ignored -> Collections.newSetFromMap(new WeakHashMap<>()))
                .add(controller);
        }
    }

    private static void unregister(TileMultiblockMachineController controller,
                                   List<TileMEConnectionShareHatch> shares) {
        for (TileMEConnectionShareHatch share : shares) {
            Set<TileMultiblockMachineController> owners = OWNERS.get(share);
            if (owners == null) continue;
            owners.remove(controller);
            if (owners.isEmpty()) OWNERS.remove(share);
        }
    }

    /** Refreshes duplicate channel charges after ME channel reservations change. */
    public static synchronized void syncDuplicateChannels(
        TileMultiblockMachineController controller) {
        State state = STATES.get(controller);
        if (state != null && !state.conflict) ensureDuplicateChannels(state);
    }

    private static void ensureDuplicateChannels(State state) {
        int duplicateCount = Math.max(0, state.shares.size() - 1);
        int contribution = 0;
        for (MEMachineComponent component : state.replacedComponents) {
            contribution += channelContribution(component);
        }
        long desiredLong = (long) contribution * duplicateCount;
        int desired = (int) Math.min(Math.max(0L, desiredLong),
            Math.max(0, AEConfig.instance().getDenseChannelCapacity()));
        while (state.duplicateLinks.size() > desired) {
            Link link = state.duplicateLinks.remove(state.duplicateLinks.size() - 1);
            state.links.remove(link);
            link.destroy();
        }
        while (state.duplicateLinks.size() < desired) {
            int index = state.duplicateLinks.size();
            if (duplicateCount == 0) break;
            TileMEConnectionShareHatch owner =
                state.shares.get(1 + (index % duplicateCount));
            IGridNode ownerNode = owner.getProxy().getNode();
            if (ownerNode == null || ownerNode.getGrid() != state.network) break;
            VirtualBlock block = new VirtualBlock(owner, index);
            IGridNode virtualNode = AEApi.instance().grid().createGridNode(block);
            try {
                virtualNode.updateState();
                IGridConnection connection =
                    AEApi.instance().grid().createGridConnection(ownerNode,
                        virtualNode);
                Link link = new Link(connection, virtualNode, true);
                state.duplicateLinks.add(link);
                state.links.add(link);
            } catch (FailedConnectionException | RuntimeException ignored) {
                virtualNode.destroy();
                break;
            }
        }
    }

    /** Returns a localized failure key, or {@code null} when sharing is valid. */
    public static synchronized String failure(TileMultiblockMachineController controller) {
        State state = STATES.get(controller);
        if (state == null) return null;
        if (state.conflict) {
            return "craftcheck.failure.mmce_complement.me_connection_share.network_mismatch";
        }
        // A network can split after the last structure update.  This is
        // checked from recipe events so the running recipe pauses safely.
        IGrid current = null;
        for (MEMachineComponent component : collectComponents(controller)) {
            if (!(component instanceof TileMEConnectionShareHatch)) continue;
            IGridNode node = component.getProxy().getNode();
            IGrid grid = node == null ? null : node.getGrid();
            if (grid == null || !component.getProxy().isPowered()) continue;
            if (current == null) current = grid;
            else if (current != grid) {
                return "craftcheck.failure.mmce_complement.me_connection_share.network_mismatch";
            }
        }
        return null;
    }

    private static int channelContribution(MEMachineComponent component) {
        if (component instanceof TileMEChannelInputHatch) {
            return ((TileMEChannelInputHatch) component).getRequestedChannels();
        }
        IGridNode node = component.getProxy().getNode();
        return node != null && node.hasFlag(GridFlags.REQUIRE_CHANNEL) ? 1 : 0;
    }

    private static List<MEMachineComponent> collectComponents(
        TileMultiblockMachineController controller) {
        Set<MEMachineComponent> unique =
            Collections.newSetFromMap(new IdentityHashMap<>());
        List<MEMachineComponent> result = new ArrayList<>();
        for (Map<TileEntity, ?> group : controller.getFoundComponents().values()) {
            for (TileEntity tile : group.keySet()) {
                if (tile instanceof MEMachineComponent
                    && unique.add((MEMachineComponent) tile)) {
                    result.add((MEMachineComponent) tile);
                }
            }
        }
        // The sharing hatch has a marker component rather than an item/fluid
        // handler, but scanning the formed pattern also keeps this manager
        // robust if a future MMCE version filters marker-only components.
        TaggedPositionBlockArray pattern = controller.getFoundPattern();
        if (pattern != null) {
            for (net.minecraft.util.math.BlockPos offset
                : pattern.getTileBlocksArray().keySet()) {
                TileEntity tile = controller.getWorld().getTileEntity(
                    controller.getPos().add(offset));
                if (tile instanceof MEMachineComponent
                    && unique.add((MEMachineComponent) tile)) {
                    result.add((MEMachineComponent) tile);
                }
            }
        }
        return result;
    }

    private static final class State {
        private final List<Link> links = new ArrayList<>();
        private final List<Link> duplicateLinks = new ArrayList<>();
        private final List<MEMachineComponent> components = new ArrayList<>();
        private final List<MEMachineComponent> replacedComponents = new ArrayList<>();
        private final List<TileMEConnectionShareHatch> shares = new ArrayList<>();
        private IGrid network;
        private boolean conflict;
        private boolean sameContext(List<MEMachineComponent> current,
                                    IGrid currentNetwork,
                                    boolean currentConflict) {
            if (network != currentNetwork || conflict != currentConflict
                || components.size() != current.size()) return false;
            Set<MEMachineComponent> identities =
                Collections.newSetFromMap(new IdentityHashMap<>());
            identities.addAll(components);
            for (MEMachineComponent component : current) {
                if (!identities.contains(component)) return false;
            }
            return true;
        }

        private boolean linksValid() {
            for (Link link : links) {
                if (link.node == null || link.node.getGrid() != network) return false;
            }
            return true;
        }

        private void destroy() {
            for (Link link : links) {
                link.destroy();
            }
            links.clear();
            duplicateLinks.clear();
        }
    }

    private static final class Link {
        private final IGridConnection connection;
        private final IGridNode node;
        private final boolean ownsNode;
        private Link(IGridConnection connection, IGridNode node,
                     boolean ownsNode) {
            this.connection = connection;
            this.node = node;
            this.ownsNode = ownsNode;
        }
        private void destroy() {
            if (connection != null) connection.destroy();
            // Target nodes belong to their TileEntity/AENetworkProxy and must
            // survive a structure refresh.  Only the virtual channel nodes
            // created by this manager are ours to destroy.
            if (ownsNode && node != null) node.destroy();
        }
    }

    private static final class VirtualBlock implements IGridBlock {
        private final TileMEConnectionShareHatch owner;
        private final int index;
        private VirtualBlock(TileMEConnectionShareHatch owner, int index) {
            this.owner = owner;
            this.index = index;
        }
        @Override public double getIdlePowerUsage() { return 0D; }
        @Override public java.util.EnumSet<GridFlags> getFlags() {
            return java.util.EnumSet.of(GridFlags.REQUIRE_CHANNEL);
        }
        @Override public boolean isWorldAccessible() { return false; }
        @Override public DimensionalCoord getLocation() { return new DimensionalCoord(owner); }
        @Override public AEColor getGridColor() { return AEColor.TRANSPARENT; }
        @Override public void onGridNotification(GridNotification notification) { }
        @Override public void setNetworkStatus(IGrid grid, int channelsInUse) { }
        @Override public java.util.EnumSet<EnumFacing> getConnectableSides() {
            return java.util.EnumSet.noneOf(EnumFacing.class);
        }
        @Override public IGridHost getMachine() { return owner; }
        @Override public void gridChanged() { }
        @Override public ItemStack getMachineRepresentation() {
            return owner.getVisualItemStack();
        }
        @Override public String toString() {
            return "MEConnectionShareChannel{" + owner.getPos() + '#' + index + '}';
        }
    }
}
