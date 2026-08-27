package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.redstoneinterface.RedstoneInterfaceRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Common persistent binding and label state for both redstone data hatches. */
public abstract class TileRedstoneInterfaceHatch
    extends TileColorableMachineComponent {

    private static final String TAG_NAME = "RedstoneValueName";
    private static final String TAG_MACHINE = "RedstoneMachine";
    private static final String TAG_CONTROLLER = "RedstoneController";
    private static final String TAG_HAS_CONTROLLER = "HasRedstoneController";

    private String selectedName = "";
    private ResourceLocation boundMachineId;
    private BlockPos controllerPos;

    public String getSelectedName() {
        return selectedName;
    }

    @Nullable
    public ResourceLocation getBoundMachineId() {
        return boundMachineId;
    }

    public List<String> getAvailableNames() {
        return boundMachineId == null
            ? Collections.emptyList()
            : RedstoneInterfaceRegistry.getNames(boundMachineId);
    }

    public boolean setSelectedName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (!normalized.isEmpty()
            && (boundMachineId == null
                || RedstoneInterfaceRegistry.get(boundMachineId, normalized) == null)) {
            return false;
        }
        if (selectedName.equals(normalized)) {
            return true;
        }
        selectedName = normalized;
        syncChangedState();
        return true;
    }

    public void bindToController(TileMultiblockMachineController controller) {
        DynamicMachine machine = controller == null ? null : controller.getFoundMachine();
        if (controller == null || machine == null) {
            return;
        }
        ResourceLocation newMachineId = machine.getRegistryName();
        BlockPos newControllerPos = controller.getPos();
        boolean changed = !newMachineId.equals(boundMachineId)
            || !newControllerPos.equals(controllerPos);
        boundMachineId = newMachineId;
        controllerPos = newControllerPos;

        List<String> names = RedstoneInterfaceRegistry.getNames(newMachineId);
        if (!names.contains(selectedName)) {
            selectedName = names.isEmpty() ? "" : names.get(0);
            changed = true;
        }
        if (changed) {
            syncChangedState();
        }
    }

    public void unbindFromController(BlockPos expectedControllerPos) {
        if (controllerPos == null || !controllerPos.equals(expectedControllerPos)) {
            return;
        }
        controllerPos = null;
        boundMachineId = null;
        onUnbound();
        syncChangedState();
    }

    protected void onUnbound() {
    }

    private void syncChangedState() {
        if (world == null) {
            return;
        }
        if (!world.isRemote) {
            markForUpdateSync();
        } else {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Nullable
    public TileMultiblockMachineController getBoundController() {
        if (world == null || controllerPos == null
            || !world.isBlockLoaded(controllerPos)) {
            return null;
        }
        TileEntity tile = world.getTileEntity(controllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) {
            return null;
        }
        TileMultiblockMachineController controller =
            (TileMultiblockMachineController) tile;
        DynamicMachine machine = controller.getFoundMachine();
        if (!controller.isStructureFormed() || machine == null
            || boundMachineId == null
            || !boundMachineId.equals(machine.getRegistryName())) {
            return null;
        }
        return controller;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        selectedName = compound.getString(TAG_NAME).trim();
        boundMachineId = null;
        if (compound.hasKey(TAG_MACHINE, 8)) {
            try {
                boundMachineId = new ResourceLocation(compound.getString(TAG_MACHINE));
            } catch (RuntimeException ignored) {
                // A malformed old value simply leaves this hatch unbound.
            }
        }
        controllerPos = compound.getBoolean(TAG_HAS_CONTROLLER)
            ? BlockPos.fromLong(compound.getLong(TAG_CONTROLLER))
            : null;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setString(TAG_NAME, selectedName);
        if (boundMachineId != null) {
            compound.setString(TAG_MACHINE, boundMachineId.toString());
        }
        compound.setBoolean(TAG_HAS_CONTROLLER, controllerPos != null);
        if (controllerPos != null) {
            compound.setLong(TAG_CONTROLLER, controllerPos.toLong());
        }
    }
}
