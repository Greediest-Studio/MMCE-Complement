package net.edwin.mmcecomplement.variant;

import github.kasuminova.mmce.common.machine.component.MachineComponentProxyRegistry;
import github.kasuminova.mmce.common.tile.base.MachineCombinationComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Connects only matched structure hatches, including combined/proxied components. */
public final class StorageAutomation {
    private StorageAutomation() { }

    public static boolean tick(TileMultiblockMachineController controller, StorageController storage) {
        if (!controller.isStructureFormed() || controller.getFoundPattern() == null) return false;
        List<Port> ports = ports(controller);
        IItemHandler items = storage.mmceComplement$getStorageSlots() > 0
            ? storage.mmceComplement$getStorageInventory() : null;
        IFluidHandler fluids = storage.mmceComplement$hasFluidStorage()
            ? storage.mmceComplement$getFluidStorage() : null;
        Object gases = storage.mmceComplement$hasGasStorage()
            ? storage.mmceComplement$getGasStorage() : null;

        // Free room before importing. Outputs are always tried in the same
        // position order, and each handler is visited in ascending slot order.
        for (Port port : ports) {
            if (port.io != IOType.OUTPUT) continue;
            if (items != null && port.handler instanceof IItemHandler) {
                StorageTransfer.items(items, internalItems(port.handler), port::changed);
            }
            if (fluids != null && port.handler instanceof IFluidHandler) {
                StorageTransfer.fluids(fluids, (IFluidHandler) port.handler, port::changed);
            }
            if (gases != null) StorageGasBridge.transfer(gases, port.handler, port::changed);
        }
        boolean full = false;
        for (Port port : ports) {
            if (port.io != IOType.INPUT) continue;
            if (items != null && port.handler instanceof IItemHandler) {
                full |= StorageTransfer.items(internalItems(port.handler), items, port::changed);
            }
            if (fluids != null && port.handler instanceof IFluidHandler) {
                full |= StorageTransfer.fluids((IFluidHandler) port.handler, fluids, port::changed);
            }
            if (gases != null) full |= StorageGasBridge.transfer(port.handler, gases, port::changed);
        }
        return full;
    }

    private static IItemHandler internalItems(Object handler) {
        // The external capability of an input bus forbids extraction, and an
        // output bus forbids insertion. MMCE recipes/GUI use internal access.
        return handler instanceof IItemHandlerImpl
            ? ((IItemHandlerImpl) handler).asGUIAccess() : (IItemHandler) handler;
    }

    private static List<Port> ports(TileMultiblockMachineController controller) {
        List<BlockPos> positions = new ArrayList<>(controller.getFoundPattern().getTileBlocksArray().keySet());
        positions.sort(Comparator.comparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
        List<Port> result = new ArrayList<>();
        for (BlockPos relative : positions) {
            BlockPos pos = controller.getPos().add(relative);
            if (!controller.getWorld().isBlockLoaded(pos)) continue;
            TileEntity tile = controller.getWorld().getTileEntity(pos);
            if (tile == null || tile == controller || tile.isInvalid()) continue;
            Collection<MachineComponent<?>> components = new ArrayList<>();
            MachineComponent<?> primary = tile instanceof MachineComponentTile
                ? ((MachineComponentTile) tile).provideComponent()
                : MachineComponentProxyRegistry.INSTANCE.proxy(tile);
            if (primary != null) components.add(primary);
            if (tile instanceof MachineCombinationComponent) {
                components.addAll(((MachineCombinationComponent) tile).provideComponents());
            }
            for (MachineComponent<?> component : components) {
                if (component == null) continue;
                Object handler = component.getContainerProvider();
                if (handler == null) continue;
                IOType io = component.getIOType();
                boolean duplicate = false;
                for (Port port : result) {
                    if (port.handler == handler && port.io == io) { duplicate = true; break; }
                }
                if (!duplicate) result.add(new Port(tile, handler, io));
            }
        }
        return result;
    }

    private static final class Port {
        private final TileEntity tile;
        private final Object handler;
        private final IOType io;
        private Port(TileEntity tile, Object handler, IOType io) {
            this.tile = tile; this.handler = handler; this.io = io;
        }
        private void changed() {
            tile.markDirty();
            if (tile instanceof TileEntitySynchronized) ((TileEntitySynchronized) tile).markForUpdateSync();
        }
    }
}
