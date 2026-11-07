package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.*;
import appeng.api.storage.*;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.storage.*;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import net.edwin.mmcecomplement.variant.StorageAccessPort;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ITickable;
import java.util.*;

/** One AE channel mounts all declared storage channels of a formed storage variant. */
public class TileAEStorageAccessPort extends TileConfigurableStoragePort implements StorageAccessPort {
    private final Set<TileMultiblockMachineController> controllers = new HashSet<>();
    private List<StorageMEInventory<?>> inventories;

    @Override public ItemStack getVisualItemStack() {
        return ModBlocks.AE_STORAGE_ACCESS_PORT == null ? ItemStack.EMPTY
            : new ItemStack(ModBlocks.AE_STORAGE_ACCESS_PORT);
    }

    @Override public MachineComponent<Object> provideComponent() {
        return new MachineComponent<Object>(IOType.INPUT) {
            @Override public ComponentType getComponentType() { return ModMEChannelTypes.STORAGE_ACCESS_COMPONENT; }
            @Override public Object getContainerProvider() { return TileAEStorageAccessPort.this; }
            @Override public boolean isAsyncSupported() { return false; }
        };
    }

    @Override public void bindStorage(TileMultiblockMachineController controller) {
        if (controller instanceof StorageController
            && ((StorageController) controller).mmceComplement$isStorageController()) controllers.add(controller);
    }
    @Override public void unbindStorage(TileMultiblockMachineController controller) { controllers.remove(controller); }

    TileMultiblockMachineController controller() {
        TileMultiblockMachineController found = null;
        Iterator<TileMultiblockMachineController> iterator = controllers.iterator();
        while (iterator.hasNext()) {
            TileMultiblockMachineController candidate = iterator.next();
            if (candidate.isInvalid() || candidate.getWorld() != getWorld()
                || !getWorld().isBlockLoaded(candidate.getPos())
                || getWorld().getTileEntity(candidate.getPos()) != candidate) {
                iterator.remove();
                continue;
            }
            if (!candidate.isStructureFormed() || candidate.getFoundPattern() == null
                || !candidate.getFoundPattern().getTileBlocksArray().containsKey(getPos().subtract(candidate.getPos()))) continue;
            // A shared hatch cannot unambiguously expose two different containers.
            if (found != null && found != candidate) return null;
            found = candidate;
        }
        return found;
    }

    StorageController storage() {
        if (isInvalid() || getWorld() == null || getWorld().isRemote || !proxy.isActive()) return null;
        TileMultiblockMachineController target = controller();
        if (target == null) return null;
        try {
            // Mount each backing container only once per grid, even with several ports.
            for (IGridNode node : proxy.getGrid().getMachines(TileAEStorageAccessPort.class)) {
                Object machine = node.getMachine();
                if (!(machine instanceof TileAEStorageAccessPort) || machine == this) continue;
                TileAEStorageAccessPort other = (TileAEStorageAccessPort) machine;
                if (other.isInvalid() || !other.proxy.isActive() || other.controller() != target) continue;
                if (other.getPos().compareTo(getPos()) < 0) return null;
            }
        } catch (GridAccessException e) { return null; }
        return (StorageController) target;
    }

    protected List<StorageMEInventory<?>> inventories() {
        if (inventories != null) return inventories;
        inventories = new ArrayList<>();
        inventories.add(new StorageMEItemInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class), () -> {
            StorageController storage = storage();
            return storage != null && storage.mmceComplement$getStorageSlots() > 0
                ? storage.mmceComplement$getStorageInventory() : null;
        }));
        inventories.add(new StorageMEFluidInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class), () -> {
            StorageController storage = storage();
            return storage != null && storage.mmceComplement$hasFluidStorage()
                ? storage.mmceComplement$getFluidStorage() : null;
        }));
        if (CompatMods.isAeGasCompatLoaded()) inventories.add(StorageMEGasBridge.create(this::storage));
        initializeNetworkViews(inventories);
        return inventories;
    }

    @Override protected StorageController networkStorage() { return storage(); }
}
