package net.edwin.mmcecomplement.machineconnection;

import crafttweaker.CraftTweakerAPI;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import github.kasuminova.mmce.common.helper.IMachineController;
import net.edwin.mmcecomplement.block.BlockMachineSignalCable;
import net.edwin.mmcecomplement.tile.TileMachineSignalInputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalOutputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalCable;
import net.edwin.mmcecomplement.tile.TileMachineSignalHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-side registry and resolver for one-to-one machine signal cables. */
public final class MachineConnectionRegistry {
    private static final Map<ResourceLocation, Set<ResourceLocation>> COMPATIBILITY =
        new LinkedHashMap<>();
    private MachineConnectionRegistry() {}

    public static synchronized void addMachineConnection(String machineId,
                                                         String[] targetIds) {
        ResourceLocation source = normalize(machineId);
        if (source == null) {
            CraftTweakerAPI.logError("[MMCE Complement] Invalid machine id: " + machineId);
            return;
        }
        Set<ResourceLocation> targets = COMPATIBILITY.computeIfAbsent(source,
            ignored -> new LinkedHashSet<>());
        if (targetIds == null) {
            return;
        }
        for (String target : targetIds) {
            ResourceLocation id = normalize(target);
            if (id == null) {
                CraftTweakerAPI.logError("[MMCE Complement] Invalid target machine id: " + target);
            } else {
                targets.add(id);
            }
        }
    }

    private static ResourceLocation normalize(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        try {
            String value = id.trim();
            return value.indexOf(':') < 0
                ? new ResourceLocation("modularmachinery", value)
                : new ResourceLocation(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static synchronized boolean isCompatible(@Nullable ResourceLocation source,
                                                    @Nullable ResourceLocation target) {
        return source != null && target != null
            && COMPATIBILITY.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    public static synchronized IMachineController[] getConnectors(@Nullable IMachineController controller) {
        if (controller == null || controller.getController() == null
            || controller.getController().getWorld() == null) {
            return new IMachineController[0];
        }
        TileMultiblockMachineController host = controller.getController();
        Set<TileMultiblockMachineController> result =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        // The query is the only place where the controller reads endpoint
        // state. Endpoint ticks maintain their own connection snapshot.
        for (TileEntity tile : host.getWorld().loadedTileEntityList) {
            if (!(tile instanceof TileMachineSignalInputHatch)) continue;
            TileMachineSignalInputHatch input = (TileMachineSignalInputHatch) tile;
            if (input.getBoundController() == host) {
                TileMultiblockMachineController child = input.getConnectedController();
                if (child != null) result.add(child);
            }
        }
        return asControllers(result);
    }

    public static synchronized IMachineController[] getConnected(@Nullable IMachineController controller) {
        if (controller == null || controller.getController() == null
            || controller.getController().getWorld() == null) {
            return new IMachineController[0];
        }
        TileMultiblockMachineController child = controller.getController();
        Set<TileMultiblockMachineController> result =
            Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (TileEntity tile : child.getWorld().loadedTileEntityList) {
            if (!(tile instanceof TileMachineSignalOutputHatch)) continue;
            TileMachineSignalOutputHatch output = (TileMachineSignalOutputHatch) tile;
            if (output.getBoundController() == child) {
                TileMultiblockMachineController host = output.getConnectedController();
                if (host != null) result.add(host);
            }
        }
        return asControllers(result);
    }

    private static IMachineController[] asControllers(Set<TileMultiblockMachineController> set) {
        if (set == null || set.isEmpty()) return new IMachineController[0];
        return set.toArray(new IMachineController[0]);
    }

    /** Resolves one endpoint from its own tile tick. */
    public static synchronized void refreshPort(TileMachineSignalHatch port) {
        if (port == null || port.getWorld() == null || port.getWorld().isRemote) return;
        World world = port.getWorld();
        port.clearConnectedController();
        NetworkEndpoints endpoints = findEndpoints(world, port.getPos());
        if (endpoints.inputs.size() != 1 || endpoints.outputs.size() != 1) return;
        TileMachineSignalInputHatch input = endpoints.inputs.get(0);
        TileMachineSignalOutputHatch output = endpoints.outputs.get(0);
        input.clearConnectedController();
        output.clearConnectedController();
        TileMultiblockMachineController host = input.getBoundController();
        TileMultiblockMachineController child = output.getBoundController();
        if (host == null || child == null || host == child
            || !isCompatible(host.getFoundMachine().getRegistryName(),
                child.getFoundMachine().getRegistryName())) return;
        input.setConnectedController(child);
        output.setConnectedController(host);
    }

    private static NetworkEndpoints findEndpoints(World world, BlockPos start) {
        NetworkEndpoints result = new NetworkEndpoints();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new LinkedHashSet<>();
        for (net.minecraft.util.EnumFacing facing
            : net.minecraft.util.EnumFacing.values()) {
            BlockPos next = start.offset(facing);
            if (isCable(world, next)) queue.add(next);
            else collectEndpoint(world, next, result);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            TileMachineSignalCable cable = cableAt(world, pos);
            if (cable == null) continue;
            for (net.minecraft.util.EnumFacing facing : net.minecraft.util.EnumFacing.values()) {
                BlockPos next = pos.offset(facing);
                if (isCable(world, next)) {
                    TileMachineSignalCable other = cableAt(world, next);
                    if (other != null && colorsConnect(cableColorAt(world, pos),
                        cableColorAt(world, next))) queue.add(next);
                } else {
                    collectEndpoint(world, next, result);
                }
            }
        }
        return result;
    }

    private static boolean colorsConnect(int a, int b) {
        return a < 0 || b < 0 || a == b;
    }

    private static void collectEndpoint(World world, BlockPos pos, NetworkEndpoints result) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMachineSignalInputHatch && !result.inputs.contains(tile)) result.inputs.add((TileMachineSignalInputHatch) tile);
        if (tile instanceof TileMachineSignalOutputHatch && !result.outputs.contains(tile)) result.outputs.add((TileMachineSignalOutputHatch) tile);
    }

    private static boolean isCable(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof BlockMachineSignalCable;
    }

    @Nullable
    private static TileMachineSignalCable cableAt(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileMachineSignalCable ? (TileMachineSignalCable) tile : null;
    }

    /** Resolve a cable's colour from its tile first, then its coloured block
     * registration. This keeps server-side networks strict during placement
     * and chunk/tile synchronization. */
    private static int cableColorAt(World world, BlockPos pos) {
        TileMachineSignalCable tile = cableAt(world, pos);
        if (tile != null && tile.getCableColor() >= 0) return tile.getCableColor();
        net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockMachineSignalCable) {
            EnumDyeColor color = ((BlockMachineSignalCable) block).getDefaultDyeColor();
            return color == null ? -1 : color.getColorValue();
        }
        return -1;
    }

    public static synchronized void clear(TileMultiblockMachineController controller) {
        // Endpoint state is owned by the endpoint tile. This method remains
        // as a compatibility no-op for older lifecycle hooks.
    }

    private static final class NetworkEndpoints {
        final List<TileMachineSignalInputHatch> inputs = new ArrayList<>();
        final List<TileMachineSignalOutputHatch> outputs = new ArrayList<>();
    }
}
