package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.AECapabilities;
import ae2.api.networking.*;
import ae2.api.parts.*;
import ae2.api.util.*;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.*;
import hellfirepvp.modularmachinery.common.tiles.base.*;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import net.edwin.mmcecomplement.variant.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.capabilities.Capability;
import java.util.*;

/** AE2S-only full-block host. It never links any appeng/MMCE ME base classes. */
public class TileStoragePort extends TileColorableMachineComponent
    implements ITickable, MachineComponentTile, StorageAccessPort, IPartHost, IInWorldGridNodeHost {
    private final Set<TileMultiblockMachineController> controllers = new HashSet<>();
    private StorageBusSettings bus;
    private boolean nodeCreated;
    private boolean pendingSave;
    private StorageController lastController;
    private long lastVersion = Long.MIN_VALUE;

    public StorageBusSettings bus() {
        if (bus == null) bus = new StorageBusSettings(this);
        return bus;
    }

    @Override public MachineComponent<?> provideComponent() {
        return new MachineComponent<Object>(IOType.INPUT) {
            @Override public ComponentType getComponentType() { return ModMEChannelTypes.STORAGE_ACCESS_COMPONENT; }
            @Override public Object getContainerProvider() { return TileStoragePort.this; }
            @Override public boolean isAsyncSupported() { return false; }
        };
    }

    @Override public synchronized void bindStorage(TileMultiblockMachineController controller) {
        if (controller instanceof StorageController && ((StorageController) controller).mmceComplement$isStorageController())
            controllers.add(controller);
    }
    @Override public synchronized void unbindStorage(TileMultiblockMachineController controller) { controllers.remove(controller); }

    protected synchronized StorageController storage() {
        if (world == null || isInvalid()) return null;
        TileMultiblockMachineController found = null;
        Iterator<TileMultiblockMachineController> it = controllers.iterator();
        while (it.hasNext()) {
            TileMultiblockMachineController tile = it.next();
            if (tile.isInvalid() || tile.getWorld() != world || !world.isBlockLoaded(tile.getPos())
                || world.getTileEntity(tile.getPos()) != tile) { it.remove(); continue; }
            if (!tile.isStructureFormed() || tile.getFoundPattern() == null
                || !tile.getFoundPattern().getTileBlocksArray().containsKey(pos.subtract(tile.getPos()))) continue;
            if (found != null && found != tile) return null;
            found = tile;
        }
        return (StorageController) found;
    }

    StorageController networkStorage() {
        if (world == null || world.isRemote || !bus().getMainNode().isOnline()) return null;
        StorageController target = storage();
        IGrid grid = bus().getMainNode().getGrid();
        if (target == null || grid == null) return null;
        for (StorageBusSettings other : grid.getMachines(StorageBusSettings.class)) {
            if (other == bus || !other.getMainNode().isOnline() || other.owner.isInvalid()) continue;
            if (other.owner.getPos().compareTo(pos) < 0 && other.owner.storage() == target) return null;
        }
        return target;
    }

    @Override public void update() {
        if (world == null || world.isRemote || isInvalid()) return;
        // Configuration restoration may call the host while its chunk is
        // still being deserialized. World access is safe at the first tick.
        if (pendingSave) {
            pendingSave = false;
            markDirty();
        }
        if (!nodeCreated) { bus().addToWorld(); nodeCreated = true; }
        StorageController current = networkStorage();
        long version = current == null ? Long.MIN_VALUE : current.mmceComplement$getStorageChangeVersion();
        // Native AE2S monitor invalidates its list on AE transfers itself.
        // This handles pipe/GUI changes and provider election/structure changes.
        bus().contentsChanged(current != lastController || version != lastVersion);
        lastController = current;
        lastVersion = version;
    }

    @Override public void invalidate() { destroyNode(); controllers.clear(); super.invalidate(); }
    @Override public void onChunkUnload() { destroyNode(); controllers.clear(); super.onChunkUnload(); }
    private void destroyNode() {
        if (nodeCreated) { bus.removeFromWorld(); nodeCreated = false; }
    }

    @Override public void readCustomNBT(NBTTagCompound data) {
        super.readCustomNBT(data);
        if (data.hasKey("Ae2sStorageBus", 10)) bus().readFromNBT(data.getCompoundTag("Ae2sStorageBus"));
    }
    @Override public void writeCustomNBT(NBTTagCompound data) {
        super.writeCustomNBT(data);
        NBTTagCompound settings = new NBTTagCompound();
        bus().writeToNBT(settings);
        data.setTag("Ae2sStorageBus", settings);
    }

    public boolean openGui(EntityPlayer player) {
        if (player.isSneaking()) return false;
        if (!world.isRemote) bus().onUseWithoutItem(player, Vec3d.ZERO);
        return true;
    }
    public void dropUpgrades() {
        if (world.isRemote || bus == null) return;
        ae2.api.inventories.InternalInventory upgrades = bus.getUpgrades();
        for (int slot = 0; slot < upgrades.size(); slot++)
            Block.spawnAsEntity(world, pos, upgrades.extractItem(slot, Integer.MAX_VALUE, false));
    }

    @Override public IGridNode getGridNode(EnumFacing side) { return bus().getMainNode().getNode(); }
    @Override public AECableType getCableConnectionType(EnumFacing side) { return AECableType.SMART; }
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
        return getCapability(capability, side) != null || super.hasCapability(capability, side);
    }
    @Override @SuppressWarnings("unchecked") public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability != null && capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) return (T) this;
        return super.getCapability(capability, side);
    }

    // UP is the native GUI locator's stable slot; all six faces expose the node.
    @Override public IPart getPart(EnumFacing side) { return side == EnumFacing.UP ? bus() : null; }
    @Override public IFacadeContainer getFacadeContainer() { return EmptyFacades.INSTANCE; }
    @Override public boolean canAddPart(ItemStack stack, EnumFacing side) { return false; }
    @Override public <T extends IPart> T addPart(IPartItem<T> item, EnumFacing side, EntityPlayer player) { return null; }
    @Override public <T extends IPart> T replacePart(IPartItem<T> item, EnumFacing side, EntityPlayer player, EnumHand hand) { return null; }
    @Override public void removePartFromSide(EnumFacing side) { }
    @Override public boolean removePart(IPart part) { return false; }
    @Override public DimensionalBlockPos getLocation() { return new DimensionalBlockPos(this); }
    @Override public TileEntity getTileEntity() { return this; }
    @Override public AEColor getColor() { return AEColor.TRANSPARENT; }
    @Override public void markForUpdate() { markForUpdateSync(); }
    @Override public void markForSave() { pendingSave = true; }
    @Override public void partChanged() { markForSave(); }
    @Override public void clearContainer() { }
    @Override public boolean isBlocked(EnumFacing side) { return true; }
    @Override public SelectedPart selectPartLocal(Vec3d point) { return new SelectedPart(bus(), EnumFacing.UP); }
    @Override public Iterable<AxisAlignedBB> getCollisionShape(Entity entity) { return Collections.singletonList(Block.FULL_BLOCK_AABB); }
    @Override public boolean hasRedstone() { return world != null && world.isBlockPowered(pos); }
    @Override public boolean isEmpty() { return false; }
    @Override public void cleanup() { }
    @Override public void notifyNeighbors() { if (world != null) world.notifyNeighborsOfStateChange(pos, getBlockType(), false); }
    @Override public void notifyNeighborNow(EnumFacing side) { notifyNeighbors(); }
    @Override public boolean isInWorld() { return world != null && !isInvalid(); }
    @Override public float getCableConnectionLength(AECableType type) { return 0; }

}
