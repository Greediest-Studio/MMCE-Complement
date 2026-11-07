package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.pathing.ChannelMode;
import ae2.me.GridNode;
import ae2.me.helpers.IGridConnectedTile;
import ae2.me.pathfinding.IPathItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import github.formlessdragon.appcompat.bridge.gtceu.GtceuGridAdapter;
import github.formlessdragon.appcompat.bridge.gtceu.GtceuGridNodeAdapter;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.mechannel.MEChannelPathSnapshot;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.util.item.AEItemStack;
import github.formlessdragon.appcompat.bridge.ae.LegacyAeStorageBridge;
import github.formlessdragon.appcompat.bridge.mmce.PatternProviderCacheExtensions;
import github.formlessdragon.appcompat.bridge.mmce.PatternProviderCacheInsertion;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Native AE2S topology and channel capacities, without emulated legacy grids. */
public final class AE2SNetwork {
    private AE2SNetwork() { }

    public static boolean isConfigSlot(net.minecraft.inventory.Slot slot) {
        return slot instanceof ae2.container.slot.FakeSlot;
    }

    public static boolean appendPatternInput(ItemStack stack, InfItemFluidHandler handler) {
        GenericStack wrapped = GenericStack.unwrapItemStack(stack);
        if (wrapped == null) return false;
        AEKey key = wrapped.what();
        if (key instanceof ae2.api.stacks.AEItemKey) {
            PatternProviderCacheInsertion.appendItems(handler, (ae2.api.stacks.AEItemKey) key, wrapped.amount());
            return true;
        }
        if (key instanceof AEFluidKey) {
            PatternProviderCacheInsertion.appendFluid(handler, (AEFluidKey) key, wrapped.amount());
            return true;
        }
        return PatternProviderCacheExtensions.push(handler, key, wrapped.amount());
    }

    public static ICraftingPatternDetails decodePattern(ItemStack stack, World world) {
        IPatternDetails nativePattern = PatternDetailsHelper.decodePattern(stack, world);
        if (nativePattern == null) return null;
        return new ComplementPatternDetails(nativePattern);
    }

    public static Object gridIdentity(IGrid grid) { return ((GtceuGridAdapter) grid).unwrap(); }

    public static Iterable<IGridNode> nodes(IGrid grid) {
        List<IGridNode> result = new ArrayList<>();
        for (ae2.api.networking.IGridNode node : ((GtceuGridAdapter) grid).unwrap().getNodes()) {
            result.add(new NodeView(node));
        }
        return result;
    }

    public static IGridNode getNode(MEMachineComponent component) {
        IManagedGridNode managed = ((IGridConnectedTile) component).getMainNode();
        return managed == null || managed.getNode() == null ? null : new NodeView(managed.getNode());
    }

    public static int normalCapacity() {
        return ae2.core.AEConfig.instance().getChannelMode().getAdHocNetworkChannels();
    }

    public static int denseCapacity() {
        ChannelMode mode = ae2.core.AEConfig.instance().getChannelMode();
        return mode == ChannelMode.INFINITE ? Integer.MAX_VALUE : 32 * mode.getCableCapacityFactor();
    }

    public static IGridNode createNode(IGridBlock block) { return new VirtualNode(block); }

    public static IGridConnection connect(IGridNode first, IGridNode second) {
        return new ConnectionView(GridHelper.createConnection(unwrap(first), unwrap(second)));
    }

    public static MEChannelPathSnapshot snapshot(MEMachineComponent component) {
        ae2.api.networking.IGridNode node = ((IGridConnectedTile) component).getMainNode().getNode();
        if (!(node instanceof GridNode) || !node.isPowered()) {
            return MEChannelPathSnapshot.unavailable(MEChannelPathSnapshot.State.DISCONNECTED,
                node == null ? null : node.grid());
        }
        ae2.api.networking.IGrid grid = node.grid();
        ae2.api.networking.pathing.IPathingService path = grid.getPathingService();
        if (path.isNetworkBooting()) return MEChannelPathSnapshot.unavailable(MEChannelPathSnapshot.State.BOOTING, grid);
        if (path.getControllerState() == ae2.api.networking.pathing.ControllerState.CONTROLLER_CONFLICT) {
            return MEChannelPathSnapshot.unavailable(MEChannelPathSnapshot.State.CONTROLLER_CONFLICT, grid);
        }
        List<MEChannelPathSnapshot.Capacity> capacities = new ArrayList<>();
        if (path.getControllerState() == ae2.api.networking.pathing.ControllerState.NO_CONTROLLER) {
            capacities.add(new MEChannelPathSnapshot.Capacity(grid,
                path.channelMode().getAdHocNetworkChannels() - path.getUsedChannels()));
            capacities.add(new MEChannelPathSnapshot.Capacity(node,
                ((GridNode) node).getMaxChannels() - ((GridNode) node).getUsedChannels()));
        } else {
            IPathItem cursor = (GridNode) node;
            if (cursor.getControllerRoute() == null) {
                return MEChannelPathSnapshot.unavailable(MEChannelPathSnapshot.State.BOOTING, grid);
            }
            IdentityHashMap<IPathItem, Boolean> visited = new IdentityHashMap<>();
            while (cursor != null && visited.put(cursor, Boolean.TRUE) == null) {
                int used = cursor instanceof GridNode ? ((GridNode) cursor).getUsedChannels()
                    : ((ae2.me.GridConnection) cursor).getUsedChannels();
                if (cursor.getMaxChannels() > 0) capacities.add(new MEChannelPathSnapshot.Capacity(cursor,
                    cursor.getMaxChannels() - used));
                cursor = cursor.getControllerRoute();
            }
        }
        return MEChannelPathSnapshot.ready(grid, capacities);
    }

    public static IManagedGridNode mainNode(MEMachineComponent tile) {
        return ((IGridConnectedTile) tile).getMainNode();
    }

    public static appeng.api.networking.storage.IStorageGrid storage(MEMachineComponent tile) throws appeng.me.GridAccessException {
        ae2.api.networking.IGrid grid = mainNode(tile).getGrid();
        if (grid == null) throw new appeng.me.GridAccessException();
        return new github.formlessdragon.appcompat.bridge.gtceu.GtceuStorageGridAdapter(grid.getStorageService(), grid.getEnergyService());
    }
    public static <T extends appeng.api.storage.data.IAEStack<T>> appeng.api.storage.IMEMonitor<T> inventory(
        MEMachineComponent tile, appeng.api.storage.IStorageChannel<T> channel) throws appeng.me.GridAccessException {
        ae2.api.networking.IGrid grid = mainNode(tile).getGrid();
        if (grid == null) throw new appeng.me.GridAccessException();
        return new AE2SInventoryMonitor<>(channel, grid.getStorageService());
    }
    public static appeng.api.networking.energy.IEnergyGrid energy(MEMachineComponent tile) throws appeng.me.GridAccessException {
        ae2.api.networking.IGrid grid = mainNode(tile).getGrid();
        if (grid == null) throw new appeng.me.GridAccessException();
        return new github.formlessdragon.appcompat.bridge.gtceu.GtceuEnergyGridAdapter(grid.getEnergyService());
    }
    public static void alert(MEMachineComponent tile) {
        IManagedGridNode node = mainNode(tile);
        if (node.getGrid() != null) node.getGrid().getTickManager().alertDevice(node.getNode());
    }
    public static <T extends appeng.api.storage.data.IAEStack<T>> T transfer(MEMachineComponent tile, T stack,
        boolean extract, appeng.api.config.Actionable mode) {
        if (stack == null || stack.getStackSize() <= 0) return null;
        IManagedGridNode node = mainNode(tile);
        if (!node.isActive()) return extract ? null : stack.copy();
        AEKey key = AE2SKeyBridge.toKey(stack);
        ae2.api.storage.MEStorage storage = node.getGrid().getStorageService().getInventory();
        ae2.api.networking.security.IActionSource source = ae2.api.networking.security.IActionSource.ofMachine((IGridConnectedTile) tile);
        ae2.api.config.Actionable action = mode == appeng.api.config.Actionable.SIMULATE
            ? ae2.api.config.Actionable.SIMULATE : ae2.api.config.Actionable.MODULATE;
        ae2.me.helpers.ActionHostEnergySource energy = new ae2.me.helpers.ActionHostEnergySource((IGridConnectedTile) tile);
        long transferred = extract ? ae2.api.storage.StorageHelper.poweredExtraction(energy, storage, key, stack.getStackSize(), source, action)
            : ae2.api.storage.StorageHelper.poweredInsert(energy, storage, key, stack.getStackSize(), source, action);
        long amount = extract ? transferred : stack.getStackSize() - transferred;
        return amount <= 0 ? null : stack.copy().setStackSize(amount);
    }

    public static void configureChannelHatch(MEMachineComponent tile) {
        mainNode(tile).setFlags(ae2.api.networking.GridFlags.DENSE_CAPACITY);
    }

    public static void attachStorage(net.edwin.mmcecomplement.compat.ae.tile.TileAEStorageAccessPort tile) {
        mainNode(tile).addService(ae2.api.storage.IStorageProvider.class, new AE2SStorageProvider(tile));
    }

    public static void attachCrafting(github.kasuminova.mmce.common.tile.MEPatternProvider tile) {
        mainNode(tile).addService(ae2.api.networking.crafting.ICraftingProvider.class, new ComplementCraftingProvider(tile));
    }

    public static void requestPatternUpdate(MEMachineComponent tile) {
        IManagedGridNode node = mainNode(tile);
        if (node != null && node.isReady()) ae2.api.networking.crafting.ICraftingProvider.requestUpdate(node);
    }

    public static void returnItems(MEMachineComponent tile, InfItemFluidHandler handler) {
        IManagedGridNode node = mainNode(tile);
        if (node == null || !node.isActive()) return;
        ae2.api.networking.IGrid grid = node.getGrid();
        ae2.api.networking.security.IActionSource source = ae2.api.networking.security.IActionSource.ofMachine((IGridConnectedTile) tile);
        ae2.me.helpers.ActionHostEnergySource energy = new ae2.me.helpers.ActionHostEnergySource((IGridConnectedTile) tile);
        List<ItemStack> items = handler.getItemStackList();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            ae2.api.stacks.AEItemKey key = ae2.api.stacks.AEItemKey.of(stack);
            long inserted = ae2.api.storage.StorageHelper.poweredInsert(energy, grid.getStorageService().getInventory(), key, stack.getCount(), source);
            items.set(i, inserted >= stack.getCount() ? ItemStack.EMPTY : key.toStack(stack.getCount() - (int) inserted));
        }
        List<net.minecraftforge.fluids.FluidStack> fluids = handler.getFluidStackList();
        for (int i = 0; i < fluids.size(); i++) {
            net.minecraftforge.fluids.FluidStack stack = fluids.get(i);
            if (stack == null) continue;
            AEFluidKey key = AEFluidKey.of(stack);
            long inserted = ae2.api.storage.StorageHelper.poweredInsert(energy, grid.getStorageService().getInventory(), key, stack.amount, source);
            fluids.set(i, inserted >= stack.amount ? null : key.toStack(stack.amount - (int) inserted));
        }
        PatternProviderCacheExtensions.returnStacks(grid, (IGridConnectedTile) tile, handler);
    }

    public static Iterable<?> machines(MEMachineComponent tile, Class<?> type) {
        IManagedGridNode node = mainNode(tile);
        return node.getGrid() == null ? java.util.Collections.emptyList() : node.getGrid().getMachines(type);
    }

    private static ae2.api.networking.IGridNode unwrap(IGridNode node) {
        if (node instanceof NodeView) return ((NodeView) node).nativeNode();
        if (node instanceof GtceuGridNodeAdapter) return ((GtceuGridNodeAdapter) node).unwrap();
        throw new IllegalArgumentException("Not a Complement AE2S endpoint: " + node);
    }

    /** Local view used only by Complement's connection and channel managers. */
    private static class NodeView implements IGridNode {
        private final ae2.api.networking.IGridNode node;
        NodeView(ae2.api.networking.IGridNode node) { this.node = node; }
        ae2.api.networking.IGridNode nativeNode() { return node; }
        @Override public IGrid getGrid() { return new GtceuGridAdapter(nativeNode().grid()); }
        @Override public IGridHost getMachine() {
            Object owner = nativeNode().getOwner();
            return owner instanceof IGridHost ? (IGridHost) owner : null;
        }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof NodeView
                && nativeNode() != null && nativeNode() == ((NodeView) other).nativeNode();
        }
        @Override public int hashCode() { return System.identityHashCode(nativeNode()); }
        @Override public boolean isActive() { return nativeNode().isActive(); }
        public boolean isPowered() { return nativeNode().isPowered(); }
        @Override public int getPlayerID() { return nativeNode().getOwningPlayerId(); }
        @Override public boolean hasFlag(appeng.api.networking.GridFlags flag) {
            return nativeNode().hasFlag(ae2.api.networking.GridFlags.valueOf(flag.name()));
        }
        @Override public boolean meetsChannelRequirements() { return nativeNode().meetsChannelRequirements(); }
        @Override public World getWorld() { return nativeNode().getLevel(); }
        @Override public void updateState() { }
        @Override public void setPlayerID(int playerId) { throw new UnsupportedOperationException("Tile owns its node"); }
        @Override public void destroy() { throw new UnsupportedOperationException("Tile owns its node"); }
        @Override public void loadFromNBT(String name, net.minecraft.nbt.NBTTagCompound data) { throw new UnsupportedOperationException(); }
        @Override public void saveToNBT(String name, net.minecraft.nbt.NBTTagCompound data) { throw new UnsupportedOperationException(); }
        @Override public IGridBlock getGridBlock() { throw new UnsupportedOperationException(); }
        @Override public void beginVisit(appeng.api.networking.IGridVisitor visitor) { throw new UnsupportedOperationException(); }
        @Override public java.util.EnumSet<appeng.api.util.AEPartLocation> getConnectedSides() {
            java.util.EnumSet<appeng.api.util.AEPartLocation> sides = java.util.EnumSet.noneOf(appeng.api.util.AEPartLocation.class);
            for (net.minecraft.util.EnumFacing side : nativeNode().getConnectedSides()) sides.add(appeng.api.util.AEPartLocation.fromFacing(side));
            return sides;
        }
        @Override public appeng.api.util.IReadOnlyCollection<IGridConnection> getConnections() { throw new UnsupportedOperationException(); }
    }

    private static final class ConnectionView implements IGridConnection {
        private final ae2.api.networking.IGridConnection connection;
        ConnectionView(ae2.api.networking.IGridConnection connection) { this.connection = connection; }
        @Override public IGridNode getOtherSide(IGridNode node) { return new NodeView(connection.getOtherSide(unwrap(node))); }
        @Override public appeng.api.util.AEPartLocation getDirection(IGridNode node) {
            net.minecraft.util.EnumFacing side = connection.getDirection(unwrap(node));
            return side == null ? appeng.api.util.AEPartLocation.INTERNAL : appeng.api.util.AEPartLocation.fromFacing(side);
        }
        @Override public void destroy() { connection.destroy(); }
        @Override public IGridNode a() { return new NodeView(connection.a()); }
        @Override public IGridNode b() { return new NodeView(connection.b()); }
        @Override public boolean hasDirection() { return connection.isInWorld(); }
        @Override public int getUsedChannels() { return connection.getUsedChannels(); }
    }

    /** Private channel endpoint, created and destroyed by the owning hatch. */
    private static final class VirtualNode extends NodeView {
        private static final IGridNodeListener<IGridHost> LISTENER = (owner, node) -> { };
        private final IGridBlock block;
        private final IManagedGridNode managed;
        private VirtualNode(IGridBlock block) {
            super(null);
            this.block = block;
            managed = GridHelper.createManagedNode(block.getMachine(), LISTENER)
                .setInWorldNode(false).setIdlePowerUsage(0).setVisualRepresentation(block.getMachineRepresentation());
            List<ae2.api.networking.GridFlags> flags = new ArrayList<>();
            for (appeng.api.networking.GridFlags flag : block.getFlags()) flags.add(ae2.api.networking.GridFlags.valueOf(flag.name()));
            managed.setFlags(flags.toArray(new ae2.api.networking.GridFlags[0]));
        }
        @Override ae2.api.networking.IGridNode nativeNode() { return managed.getNode(); }
        @Override public void updateState() { if (!managed.isReady()) managed.create(block.getLocation().getWorld(), block.getLocation().getPos()); }
        @Override public void setPlayerID(int playerId) { managed.setOwningPlayerId(playerId); }
        @Override public void destroy() { managed.destroy(); }
        @Override public IGridBlock getGridBlock() { return block; }
    }
}
