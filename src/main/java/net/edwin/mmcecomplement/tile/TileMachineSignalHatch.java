package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/** Common controller binding for machine signal endpoints. */
public abstract class TileMachineSignalHatch extends TileColorableMachineComponent {
    private BlockPos controllerPos;

    public void bindToController(TileMultiblockMachineController controller) {
        if (controller == null) return;
        controllerPos = controller.getPos();
        markDirty();
    }

    public void unbindFromController(BlockPos expected) {
        if (controllerPos != null && controllerPos.equals(expected)) {
            controllerPos = null;
            markDirty();
        }
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
    }

    @Override
    public void writeCustomNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setBoolean("MachineSignalController", controllerPos != null);
        if (controllerPos != null) tag.setLong("MachineSignalControllerPos", controllerPos.toLong());
    }
}
