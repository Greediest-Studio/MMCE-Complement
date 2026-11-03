package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.machineconnection.MachineConnectionRegistry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/** Common controller binding for machine signal endpoints. */
public abstract class TileMachineSignalHatch extends TileEntityRestrictedTick {
    private BlockPos controllerPos;
    private BlockPos connectedControllerPos;

    public void bindToController(TileMultiblockMachineController controller) {
        if (controller == null) return;
        controllerPos = controller.getPos();
        markDirty();
    }

    /** Called by the tile's own restricted tick, never by the machine controller. */
    @Override
    public void doRestrictedTick() {
        if (world == null || world.isRemote) {
            return;
        }
        // Endpoint discovery is deliberately independent from the machine
        // controller tick. Five ticks keeps placement/replacement responsive
        // while avoiding a full cable walk every server tick.
        if ((world.getTotalWorldTime() + pos.toLong()) % 5L != 0L) {
            return;
        }
        refreshBoundController();
        MachineConnectionRegistry.refreshPort(this);
    }

    private void refreshBoundController() {
        TileMultiblockMachineController current = getBoundController();
        if (current != null && containsPosition(current)) {
            return;
        }
        controllerPos = null;
        for (TileEntity tile : world.loadedTileEntityList) {
            if (!(tile instanceof TileMultiblockMachineController)) {
                continue;
            }
            TileMultiblockMachineController candidate =
                (TileMultiblockMachineController) tile;
            if (candidate.isInvalid() || !candidate.isStructureFormed()
                || candidate.getFoundMachine() == null
                || !containsPosition(candidate)) {
                continue;
            }
            controllerPos = candidate.getPos();
            break;
        }
        markDirty();
    }

    private boolean containsPosition(TileMultiblockMachineController controller) {
        if (controller == null || controller.getFoundPattern() == null) {
            return false;
        }
        return controller.getFoundPattern().getPattern().containsKey(
            pos.subtract(controller.getPos()));
    }

    public void unbindFromController(BlockPos expected) {
        if (controllerPos != null && controllerPos.equals(expected)) {
            controllerPos = null;
            markDirty();
        }
    }

    public void clearConnectedController() {
        connectedControllerPos = null;
    }

    public void setConnectedController(TileMultiblockMachineController controller) {
        connectedControllerPos = controller == null ? null : controller.getPos();
    }

    @Nullable
    public TileMultiblockMachineController getConnectedController() {
        if (world == null || connectedControllerPos == null
            || !world.isBlockLoaded(connectedControllerPos)) {
            return null;
        }
        TileEntity tile = world.getTileEntity(connectedControllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) return null;
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) tile;
        return controller.isStructureFormed() && controller.getFoundMachine() != null
            ? controller : null;
    }

    @Nullable
    public TileMultiblockMachineController getBoundController() {
        if (world == null || controllerPos == null || !world.isBlockLoaded(controllerPos)) return null;
        TileEntity tile = world.getTileEntity(controllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) return null;
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;
        DynamicMachine machine = controller.getFoundMachine();
        return controller.isStructureFormed() && machine != null ? controller : null;
    }

    @Override
    public void readCustomNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readCustomNBT(tag);
        controllerPos = tag.getBoolean("MachineSignalController")
            ? BlockPos.fromLong(tag.getLong("MachineSignalControllerPos")) : null;
        connectedControllerPos = tag.getBoolean("MachineSignalConnectedController")
            ? BlockPos.fromLong(tag.getLong("MachineSignalConnectedControllerPos")) : null;
    }

    @Override
    public void writeCustomNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setBoolean("MachineSignalController", controllerPos != null);
        if (controllerPos != null) tag.setLong("MachineSignalControllerPos", controllerPos.toLong());
        tag.setBoolean("MachineSignalConnectedController", connectedControllerPos != null);
        if (connectedControllerPos != null) {
            tag.setLong("MachineSignalConnectedControllerPos", connectedControllerPos.toLong());
        }
    }
}
