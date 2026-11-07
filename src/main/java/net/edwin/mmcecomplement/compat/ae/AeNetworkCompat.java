package net.edwin.mmcecomplement.compat.ae;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnectionException;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.core.AEConfig;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae2s.AE2SNetwork;
import net.edwin.mmcecomplement.compat.ae2s.AE2SStorageProvider;
import net.edwin.mmcecomplement.compat.ae.storage.StorageMEInventory;
import appeng.api.storage.data.IAEStack;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;

/** Branch boundary: native AE2S types stay in the optional compatibility package. */
public final class AeNetworkCompat {
    private AeNetworkCompat() { }

    public static void attachCrafting(github.kasuminova.mmce.common.tile.MEPatternProvider tile) {
        if (CompatMods.isAe2sLoaded()) AE2SNetwork.attachCrafting(tile);
    }
    public static void requestPatternUpdate(MEMachineComponent tile) { AE2SNetwork.requestPatternUpdate(tile); }
    public static void returnItems(MEMachineComponent tile, InfItemFluidHandler handler) { AE2SNetwork.returnItems(tile, handler); }


    public static appeng.api.networking.storage.IStorageGrid storage(MEMachineComponent tile) throws appeng.me.GridAccessException {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.storage(tile) : tile.getProxy().getStorage();
    }
    public static <T extends IAEStack<T>> appeng.api.storage.IMEMonitor<T> inventory(
        MEMachineComponent tile, appeng.api.storage.IStorageChannel<T> channel) throws appeng.me.GridAccessException {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.inventory(tile, channel) : storage(tile).getInventory(channel);
    }
    public static appeng.api.networking.energy.IEnergyGrid energy(MEMachineComponent tile) throws appeng.me.GridAccessException {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.energy(tile) : tile.getProxy().getEnergy();
    }
    public static void alert(MEMachineComponent tile) throws appeng.me.GridAccessException {
        if (CompatMods.isAe2sLoaded()) AE2SNetwork.alert(tile);
        else if (tile.getProxy().getNode() != null) tile.getProxy().getTick().alertDevice(tile.getProxy().getNode());
    }
    public static <T extends IAEStack<T>> T poweredInsert(MEMachineComponent tile,
        appeng.api.networking.energy.IEnergySource energy, appeng.api.storage.IMEInventory<T> inventory,
        T input, appeng.api.networking.security.IActionSource source) {
        if (CompatMods.isAe2sLoaded()) return AE2SNetwork.transfer(tile, input, false, appeng.api.config.Actionable.MODULATE);
        return appeng.util.Platform.poweredInsert(energy, inventory, input, source);
    }
    public static <T extends IAEStack<T>> T poweredExtraction(MEMachineComponent tile,
        appeng.api.networking.energy.IEnergySource energy, appeng.api.storage.IMEInventory<T> inventory,
        T input, appeng.api.networking.security.IActionSource source) {
        if (CompatMods.isAe2sLoaded()) return AE2SNetwork.transfer(tile, input, true, appeng.api.config.Actionable.MODULATE);
        return appeng.util.Platform.poweredExtraction(energy, inventory, input, source);
    }

    public static boolean appendPatternInput(ItemStack stack, InfItemFluidHandler handler) {
        return CompatMods.isAe2sLoaded() && AE2SNetwork.appendPatternInput(stack, handler);
    }

    public static ICraftingPatternDetails decodePattern(ItemStack stack, World world) {
        if (stack.isEmpty()) return null;
        if (CompatMods.isAe2sLoaded()) return AE2SNetwork.decodePattern(stack, world);
        return stack.getItem() instanceof ICraftingPatternItem
            ? ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, world) : null;
    }

    public static Object gridIdentity(IGrid grid) {
        return grid == null || !CompatMods.isAe2sLoaded() ? grid : AE2SNetwork.gridIdentity(grid);
    }

    public static boolean sameGrid(IGrid first, IGrid second) {
        return gridIdentity(first) == gridIdentity(second);
    }

    public static Iterable<IGridNode> nodes(IGrid grid) {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.nodes(grid) : grid.getNodes();
    }

    public static IGridNode getNode(MEMachineComponent component) {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.getNode(component) : component.getProxy().getNode();
    }

    public static IGridNode createNode(IGridBlock block) {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.createNode(block) : AEApi.instance().grid().createGridNode(block);
    }

    public static IGridConnection connect(IGridNode first, IGridNode second) throws FailedConnectionException {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.connect(first, second)
            : AEApi.instance().grid().createGridConnection(first, second);
    }

    public static int normalCapacity() {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.normalCapacity() : AEConfig.instance().getNormalChannelCapacity();
    }

    public static int denseCapacity() {
        return CompatMods.isAe2sLoaded() ? AE2SNetwork.denseCapacity() : AEConfig.instance().getDenseChannelCapacity();
    }

    public static void postStorageChanges(MEMachineComponent tile, StorageMEInventory<?> inventory,
                                          Iterable<? extends IAEStack<?>> changes) {
        for (appeng.api.storage.data.IAEStack<?> stack : changes)
            AE2SStorageProvider.changed(tile, inventory, stack, stack.getStackSize());
    }
}
