package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.AEApi;
import appeng.api.config.*;
import appeng.api.networking.IGridNode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AEPartLocation;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.misc.PartStorageBus;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.PrecisePriorityList;
import net.edwin.mmcecomplement.compat.ae.tile.TileConfigurableStoragePort;

/** Native storage-bus settings/GUI, using the hatch's existing node and storage. */
public final class StorageBusSettings extends PartStorageBus {
    private final TileConfigurableStoragePort owner;

    public StorageBusSettings(TileConfigurableStoragePort owner) {
        // Use the real storage-bus item so AE's upgrade limits stay identical.
        super(AEApi.instance().definitions().parts().storageBus().maybeStack(1)
            .orElseThrow(() -> new IllegalStateException("Missing AE storage bus")));
        this.owner = owner;
        setPartHostInfo(AEPartLocation.INTERNAL, owner, owner);
    }

    @Override public AENetworkProxy getProxy() {
        // PartBasicState calls this during super construction.
        return owner == null ? super.getProxy() : owner.getProxy();
    }
    @Override public IGridNode getActionableNode() { return owner.getActionableNode(); }
    @Override public IGridNode getGridNode(AEPartLocation side) { return owner.getGridNode(side); }
    @Override public IGridNode getGridNode() { return owner.getActionableNode(); }
    @Override public boolean isActive() { return owner.isActive(); }
    @Override public boolean isPowered() { return owner.isPowered(); }

    @Override protected void resetCache(boolean fullReset) {
        if (owner != null) owner.storageConfigurationChanged();
    }

    @Override public MEInventoryHandler<IAEItemStack> getInternalHandler() {
        return owner.getItemNetworkView();
    }

    /** Apply the installed branch's own access/filter/priority implementation. */
    public <T extends IAEStack<T>> void configure(StorageMEInventory<T> inventory) {
        MEInventoryHandler<T> view;
        if (inventory.getNetworkView() == inventory) {
            view = new MEInventoryHandler<>(inventory, inventory.getChannel());
            inventory.setNetworkView(view);
        } else {
            view = (MEInventoryHandler<T>) inventory.getNetworkView();
        }
        view.setBaseAccess((AccessRestriction) getConfigManager().getSetting(Settings.ACCESS));
        view.setPriority(getPriority());
        view.setStorageFilter((StorageFilter) getConfigManager().getSetting(Settings.STORAGE_FILTER));
        view.setSticky(getInstalledUpgrades(Upgrades.STICKY) > 0);

        if (inventory instanceof StorageMEItemInventory) {
            MEInventoryHandler<IAEItemStack> itemView = (MEInventoryHandler<IAEItemStack>) view;
            itemView.setWhitelist(getInstalledUpgrades(Upgrades.INVERTER) > 0
                ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
            IItemList<IAEItemStack> list = ((StorageMEItemInventory) inventory).getChannel().createList();
            int usableSlots = Math.min(Config.getSlots(), 18 + 9 * getInstalledUpgrades(Upgrades.CAPACITY));
            for (int slot = 0; slot < usableSlots; slot++) {
                IAEItemStack stack = Config.getAEStackInSlot(slot);
                if (stack != null) list.add(stack.copy());
            }
            itemView.setPartitionList(getInstalledUpgrades(Upgrades.FUZZY) > 0
                ? new FuzzyPriorityList<>(list, (FuzzyMode) getConfigManager().getSetting(Settings.FUZZY_MODE))
                : new PrecisePriorityList<>(list));
        }
    }
}
