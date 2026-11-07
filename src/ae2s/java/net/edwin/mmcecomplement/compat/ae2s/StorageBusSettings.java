package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.config.*;
import ae2.api.networking.IGridNode;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.storage.*;
import ae2.core.definitions.AEItems;
import ae2.core.definitions.AEParts;
import ae2.me.storage.NullInventory;
import ae2.parts.storagebus.StorageBusPart;
import ae2.util.prioritylist.IPartitionList;

/** The installed AE2S version owns the GUI, packets, upgrades and monitor cache. */
public final class StorageBusSettings extends StorageBusPart {
    final TileStoragePort owner;
    private boolean configurationDirty = true;

    StorageBusSettings(TileStoragePort owner) {
        super(AEParts.STORAGE_BUS.item());
        this.owner = owner;
        setPartHostInfo(net.minecraft.util.EnumFacing.UP, owner, owner);
        getMainNode().setInWorldNode(true).setExposedOnSides(java.util.EnumSet.allOf(net.minecraft.util.EnumFacing.class));
        ((ControllerInventory) handler).bind(new ControllerStorage(owner::networkStorage));
    }

    @Override protected StorageBusInventory createHandler() { return new ControllerInventory(); }

    @Override protected void onConfigurationChanged() {
        configurationDirty = true;
        if (owner != null) owner.markForSave();
    }

    @Override protected void updateTarget(boolean force) { configure(); }

    private void configure() {
        if (!configurationDirty) return;
        configurationDirty = false;
        handler.setAccessRestriction(getConfigManager().getSetting(Settings.ACCESS));
        handler.setWhitelist(isUpgradedWith(AEItems.INVERTER_CARD) ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
        IPartitionList partition = createFilter();
        handler.setPartitionList(partition);
        handler.setReadPartitionFiltering(!partition.isEmpty());
        handler.setVoidOverflow(isUpgradedWith(AEItems.VOID_CARD));
        handler.setSticky(isUpgradedWith(AEItems.STICKY_CARD));
        boolean filter = getConfigManager().getSetting(Settings.FILTER_ON_EXTRACT) == YesNo.YES;
        handler.setExtractFiltering(filter, isExtractableOnly() && filter);
        handler.configurationChanged();
    }

    void contentsChanged(boolean changed) {
        boolean configured = configurationDirty;
        configure();
        if (changed || configured) handler.onTick();
    }

    @Override public void mountInventories(IStorageMounts mounts) {
        configure();
        if (getMainNode().isOnline()) mounts.mount(handler, getPriority());
    }

    // Tile ticking uses the controller's version marker instead of rescanning
    // every slot at the native bus's adaptive polling interval.
    @Override public TickRateModulation tickingRequest(IGridNode node, int ticks) { return TickRateModulation.SLEEP; }

    private static final class ControllerInventory extends StorageBusInventory {
        ControllerInventory() { super(NullInventory.of()); }
        void bind(MEStorage storage) { setDelegate(storage); }
    }
}
