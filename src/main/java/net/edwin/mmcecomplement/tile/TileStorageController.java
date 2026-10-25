package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import net.minecraft.block.state.IBlockState;
import net.edwin.mmcecomplement.variant.StorageAutomation;
import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.variant.StorageAccessPort;
import net.edwin.mmcecomplement.variant.StorageAccessPorts;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller used by storage variants.
 *
 * <p>A storage structure has the same pattern matching and component update
 * behaviour as a normal MMCE machine, but it deliberately never schedules a
 * recipe search or recipe tick. Keeping it as a separate tile prevents the
 * normal integrated controller logic from running a container as a machine.
 */
public class TileStorageController extends TileMachineController {
    public static final String FULL_MESSAGE = "status.mmce_complement.storage.full";
    private static final CraftingStatus FULL_STATUS = CraftingStatus.failure(FULL_MESSAGE);
    private CraftingStatus status = CraftingStatus.MISSING_STRUCTURE;
    private Set<StorageAccessPort> storagePorts = new HashSet<>();

    public TileStorageController() {
        super();
    }

    public TileStorageController(IBlockState state) {
        super(state);
        updateControllerState(state);
    }

    /** Preserve MMCE's parent-controller reference for a freshly placed block. */
    public TileStorageController(BlockController controller, IBlockState state) {
        super(state);
        ((net.edwin.mmcecomplement.mixin.AccessorTileMachineController)
            (Object) this).mmceComplement$setParentController(controller);
        updateControllerState(state);
    }

    private void updateControllerState(IBlockState state) {
        if (state == null || !(state.getBlock() instanceof BlockController)) {
            return;
        }
        BlockController block = (BlockController) state.getBlock();
        ((net.edwin.mmcecomplement.mixin.AccessorTileMachineController)
            (Object) this).mmceComplement$setParentController(block);
        parentMachine = block.getParentMachine();
        if (state.getPropertyKeys().contains(BlockController.FACING)) {
            controllerRotation = state.getValue(BlockController.FACING);
        }
    }

    @Override
    public void doControllerTick() {
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        // Keep the controller's machine and facing current after a block
        // rotation or after a chunk has restored the tile from NBT.
        updateControllerState(getWorld().getBlockState(getPos()));
        boolean formed = doStructureCheck() && isStructureFormed();
        storagePorts = StorageAccessPorts.refresh(this, storagePorts);
        // No stock is exposed to hatches until the whole structure is formed.
        setControllerStatus(!formed ? CraftingStatus.MISSING_STRUCTURE
            : StorageAutomation.tick(this, (StorageController) (Object) this)
                ? FULL_STATUS : CraftingStatus.SUCCESS);
    }

    @Override
    public CraftingStatus getControllerStatus() {
        return status;
    }

    @Override
    public void setControllerStatus(CraftingStatus status) {
        CraftingStatus next = status == null ? CraftingStatus.MISSING_STRUCTURE : status;
        if (next.equals(this.status)) return;
        this.status = next;
        markDirty();
        if (getWorld() != null && !getWorld().isRemote) markForUpdateSync();
    }

    @Override
    public void flushContextModifier() {
        // Storage variants do not create recipe contexts or modifiers.
    }

    @Override
    public void overrideStatusInfo(String message) {
        if (message != null && !message.isEmpty()) {
            setControllerStatus(CraftingStatus.failure(message));
        }
    }

    @Override
    public void readCustomNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.readCustomNBT(compound);
        status = compound.hasKey("mmceComplementStorageStatus", 10)
            ? CraftingStatus.deserialize(compound.getCompoundTag("mmceComplementStorageStatus"))
            : CraftingStatus.MISSING_STRUCTURE;
        if (getWorld() != null) {
            updateControllerState(getWorld().getBlockState(getPos()));
        }
    }

    @Override
    public void writeCustomNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setTag("mmceComplementStorageStatus", status.serialize());
    }
}
