package net.edwin.mmcecomplement.util;

import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.TaggedPositionBlockArray;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.tile.TileMachineControlInterface;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public final class MachineControlInterfaceHelper {

    private static final String CONTROL_INTERFACE_PATH = "machine_control_interface";

    private MachineControlInterfaceHelper() {}

    public static boolean hasPoweredInterface(TileMultiblockMachineController controller) {
        if (hasPoweredInterfaceInPattern(controller)) {
            return true;
        }

        // Fallback for edge-cases where pattern data is temporarily unavailable.
        for (Map<TileEntity, ProcessingComponent<?>> componentMap : controller.getFoundComponents().values()) {
            for (TileEntity tileEntity : componentMap.keySet()) {
                if (tileEntity instanceof TileMachineControlInterface) {
                    TileMachineControlInterface controlInterface = (TileMachineControlInterface) tileEntity;
                    if (controlInterface.isRedstonePowered()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasPoweredInterfaceInPattern(TileMultiblockMachineController controller) {
        if (controller.getWorld() == null) {
            return false;
        }

        TaggedPositionBlockArray foundPattern = controller.getFoundPattern();
        if (foundPattern == null || foundPattern.getPattern().isEmpty()) {
            return false;
        }

        BlockPos controllerPos = controller.getPos();
        for (BlockPos relPos : foundPattern.getPattern().keySet()) {
            BlockPos worldPos = controllerPos.add(relPos);
            Block block = controller.getWorld().getBlockState(worldPos).getBlock();
            ResourceLocation registryName = block.getRegistryName();
            if (registryName != null
                    && Tags.MOD_ID.equals(registryName.getNamespace())
                    && CONTROL_INTERFACE_PATH.equals(registryName.getPath())) {
                if (controller.getWorld().isBlockPowered(worldPos)
                        || controller.getWorld().getRedstonePowerFromNeighbors(worldPos) > 0
                        || controller.getWorld().getStrongPower(worldPos) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getEffectiveRedstonePower(TileMultiblockMachineController controller) {
        int directPower = controller.getWorld().getStrongPower(controller.getPos());
        if (hasPoweredInterface(controller)) {
            return Math.max(directPower, 15);
        }
        return directPower;
    }
}