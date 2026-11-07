package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.networking.IGrid;
import appeng.api.networking.events.*;
import appeng.api.parts.*;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.*;
import appeng.core.sync.GuiBridge;
import appeng.me.GridAccessException;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.Platform;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.compat.ae.storage.StorageBusSettings;
import net.edwin.mmcecomplement.compat.ae.storage.StorageMEInventory;
import net.edwin.mmcecomplement.compat.ae.storage.StorageMEItemInventory;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;
import java.util.List;

/** Native bus configuration and change delivery for the ME storage access port. */
public abstract class TileConfigurableStoragePort extends MEMachineComponent
    implements ICellContainer, ITickable, IPartHost {
    private StorageBusSettings storageBus;
    private IGrid lastGrid;
    private boolean lastActive;
    private boolean refreshGrid;
    private boolean configurationDirty;
    private boolean pendingSave;
    private StorageController lastStorage;
    private long lastStorageVersion = Long.MIN_VALUE;

    protected abstract List<StorageMEInventory<?>> inventories();
    protected abstract StorageController networkStorage();

    public final StorageBusSettings getStorageBus() {
        if (storageBus == null) storageBus = new StorageBusSettings(this);
        return storageBus;
    }

    protected final void initializeNetworkViews(List<StorageMEInventory<?>> inventories) {
        for (StorageMEInventory<?> inventory : inventories) {
            getStorageBus().configure(inventory);
            inventory.resetSnapshot();
        }
    }

    @SuppressWarnings("unchecked")
    public final MEInventoryHandler<IAEItemStack> getItemNetworkView() {
        for (StorageMEInventory<?> inventory : inventories()) {
            if (inventory instanceof StorageMEItemInventory)
                return (MEInventoryHandler<IAEItemStack>) inventory.getNetworkView();
        }
        return null;
    }

    public final void storageConfigurationChanged() {
        configurationDirty = true;
        markForSave();
    }

    public final boolean openStorageBusGui(EntityPlayer player) {
        if (!player.isSneaking() && !getWorld().isRemote) {
            Platform.openGUI(player, this, AEPartLocation.INTERNAL, GuiBridge.GUI_STORAGEBUS);
        }
        return !player.isSneaking();
    }

    @Override public List<IMEInventoryHandler> getCellArray(IStorageChannel<?> channel) {
        for (StorageMEInventory<?> inventory : inventories()) {
            if (inventory.getChannel() == channel)
                return Collections.singletonList(inventory.getNetworkView());
        }
        return Collections.emptyList();
    }

    @Override public final void update() {
        if (getWorld() == null || getWorld().isRemote || isInvalid()) return;
        // Native settings callbacks also run during readFromNBT, before the
        // chunk has finished loading. markDirty then asks that same chunk for
        // its block state and recursively reloads its tiles. Persist only once
        // the tile is ticking, independently of network power/channel state.
        if (pendingSave) {
            pendingSave = false;
            markDirty();
        }
        try {
            IGrid grid = proxy.getGrid();
            boolean active = proxy.isActive();
            StorageController current = active ? networkStorage() : null;
            long version = current == null ? Long.MIN_VALUE : current.mmceComplement$getStorageChangeVersion();
            boolean topologyChanged = lastGrid != grid || lastActive != active;
            if (topologyChanged) {
                // Grid activity changes have their own AE add/remove accounting.
                // Configuration changes still need their visible-content delta.
                grid.postEvent(new MENetworkCellArrayUpdate());
                for (StorageMEInventory<?> inventory : inventories()) inventory.resetSnapshot();
            }
            if (configurationDirty || active && (current != lastStorage || version != lastStorageVersion)) {
                if (configurationDirty) {
                    for (StorageMEInventory<?> inventory : inventories()) getStorageBus().configure(inventory);
                }
                // Include a transition to null: a broken/unbound structure must
                // remove the previously exposed stock, even without a mutation.
                for (StorageMEInventory<?> inventory : inventories()) {
                    List<? extends IAEStack<?>> changes = inventory.pollChanges();
                    if (!changes.isEmpty() && active)
                        proxy.getStorage().postAlterationOfStoredItems(inventory.getChannel(), changes, source);
                }
                // Only changes in routing/settings rebuild the cell array.
                // Ordinary contents changes use the incremental monitor path.
                if (configurationDirty || refreshGrid) grid.postEvent(new MENetworkCellArrayUpdate());
                configurationDirty = false;
            } else if (refreshGrid && !topologyChanged) {
                grid.postEvent(new MENetworkCellArrayUpdate());
            }
            refreshGrid = false;
            lastGrid = grid;
            lastActive = active;
            lastStorage = current;
            lastStorageVersion = version;
        } catch (GridAccessException ignored) {
            lastGrid = null;
            lastActive = false;
        }
    }

    @Override public void gridChanged() { refreshGrid = true; }
    @MENetworkEventSubscribe public void channelsChanged(MENetworkChannelsChanged event) { refreshGrid = true; }
    @MENetworkEventSubscribe public void powerChanged(MENetworkPowerStatusChange event) { refreshGrid = true; }
    @Override public int getPriority() { return getStorageBus().getPriority(); }
    @Override public void blinkCell(int slot) { }
    @Override public void saveChanges(ICellInventory<?> cell) { markForSave(); }

    @Override public void readCustomNBT(NBTTagCompound data) {
        super.readCustomNBT(data);
        if (data.hasKey("StorageBusSettings", 10)) getStorageBus().readFromNBT(data.getCompoundTag("StorageBusSettings"));
        configurationDirty = true;
    }

    @Override public void writeCustomNBT(NBTTagCompound data) {
        super.writeCustomNBT(data);
        NBTTagCompound settings = new NBTTagCompound();
        getStorageBus().writeToNBT(settings);
        data.setTag("StorageBusSettings", settings);
    }

    public final void dropUpgrades() {
        if (getWorld() == null || getWorld().isRemote || storageBus == null) return;
        IItemHandler upgrades = storageBus.getInventoryByName("upgrades");
        for (int slot = 0; slot < upgrades.getSlots(); slot++)
            Block.spawnAsEntity(getWorld(), getPos(), upgrades.extractItem(slot, Integer.MAX_VALUE, false));
    }

    // Expose the embedded settings object to AE's native GUI resolver. It has
    // no separate world presence, grid node, geometry, or installable parts.
    @Override public IPart getPart(AEPartLocation side) { return side == AEPartLocation.INTERNAL ? getStorageBus() : null; }
    @Override public IPart getPart(EnumFacing side) { return null; }
    @Override public TileEntity getTile() { return this; }
    @Override public AEColor getColor() { return AEColor.TRANSPARENT; }
    @Override public void markForUpdate() { markForUpdateSync(); }
    @Override public void markForSave() { pendingSave = true; }
    @Override public void partChanged() { storageConfigurationChanged(); }
    @Override public boolean isInWorld() { return getWorld() != null && !isInvalid(); }
    @Override public boolean isEmpty() { return false; }
    @Override public boolean isBlocked(EnumFacing side) { return true; }
    @Override public boolean hasRedstone(AEPartLocation side) { return getWorld() != null && getWorld().isBlockPowered(getPos()); }
    @Override public float getCableConnectionLength(AECableType type) { return 0; }
    @Override public SelectedPart selectPart(Vec3d position) { return new SelectedPart(getStorageBus(), AEPartLocation.INTERNAL); }
    @Override public IFacadeContainer getFacadeContainer() { return net.edwin.mmcecomplement.compat.ae.storage.EmptyFacades.INSTANCE; }
    @Override public boolean canAddPart(ItemStack stack, AEPartLocation side) { return false; }
    @Override public AEPartLocation addPart(ItemStack stack, AEPartLocation side, EntityPlayer player, EnumHand hand) { return null; }
    @Override public void removePart(AEPartLocation side, boolean suppressUpdate) { }
    @Override public void clearContainer() { }
    @Override public void cleanup() { }
    @Override public void notifyNeighbors() { super.notifyNeighbors(); }
}
