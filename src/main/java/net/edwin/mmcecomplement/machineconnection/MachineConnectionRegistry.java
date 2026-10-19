package net.edwin.mmcecomplement.machineconnection;

import crafttweaker.CraftTweakerAPI;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import github.kasuminova.mmce.common.helper.IMachineController;
import net.edwin.mmcecomplement.block.BlockMachineSignalCable;
import net.edwin.mmcecomplement.tile.TileMachineSignalInputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalOutputHatch;
import net.edwin.mmcecomplement.tile.TileMachineSignalCable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-side registry and resolver for one-to-one machine signal cables. */
public final class MachineConnectionRegistry {
    private static final Map<ResourceLocation, Set<ResourceLocation>> COMPATIBILITY =
        new LinkedHashMap<>();
    private static final Map<TileMultiblockMachineController, Set<TileMultiblockMachineController>> CONNECTORS =
        Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<TileMultiblockMachineController, Set<TileMultiblockMachineController>> CONNECTED =
        Collections.synchronizedMap(new IdentityHashMap<>());

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
        if (controller == null || controller.getController() == null) return new IMachineController[0];
        return asControllers(CONNECTORS.get(controller.getController()));
    }

    public static synchronized IMachineController[] getConnected(@Nullable IMachineController controller) {
        if (controller == null || controller.getController() == null) return new IMachineController[0];
        return asControllers(CONNECTED.get(controller.getController()));
    }

    private static IMachineController[] asControllers(Set<TileMultiblockMachineController> set) {
        if (set == null || set.isEmpty()) return new IMachineController[0];
        return set.toArray(new IMachineController[0]);
    }

    /** Re-resolves all cable networks touching the supplied world. Must run on the server thread. */
    public static synchronized void refresh(World world) {
        if (world == null || world.isRemote) return;
        clearWorld(world);
        List<TileMachineSignalInputHatch> inputs = new ArrayList<>();
        List<TileMachineSignalOutputHatch> outputs = new ArrayList<>();
        for (TileEntity tile : world.loadedTileEntityList) {
            if (tile instanceof TileMachineSignalInputHatch) inputs.add((TileMachineSignalInputHatch) tile);
            if (tile instanceof TileMachineSignalOutputHatch) outputs.add((TileMachineSignalOutputHatch) tile);
        }
        for (TileMachineSignalInputHatch input : inputs) {
            TileMultiblockMachineController host = input.getBoundController();
            if (host == null || host.getFoundMachine() == null) continue;
            NetworkEndpoints endpoints = findEndpoints(world, input.getPos());
            if (endpoints.inputs.size() != 1 || endpoints.outputs.size() != 1
                || endpoints.inputs.get(0) != input) continue;
            TileMachineSignalOutputHatch output = endpoints.outputs.get(0);
            TileMultiblockMachineController child = output.getBoundController();
            if (child == null || child == host || child.getFoundMachine() == null) continue;
            if (!isCompatible(host.getFoundMachine().getRegistryName(),
                              child.getFoundMachine().getRegistryName())) continue;
            CONNECTORS.computeIfAbsent(host, ignored -> new LinkedHashSet<>()).add(child);
            CONNECTED.computeIfAbsent(child, ignored -> new LinkedHashSet<>()).add(host);
        }
    }

    private static NetworkEndpoints findEndpoints(World world, BlockPos start) {
        NetworkEndpoints result = new NetworkEndpoints();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new LinkedHashSet<>();
        for (net.minecraft.util.EnumFacing facing : net.minecraft.util.EnumFacing.values()) {
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
        CONNECTORS.remove(controller);
        CONNECTED.remove(controller);
        for (Set<TileMultiblockMachineController> set : CONNECTORS.values()) set.remove(controller);
        for (Set<TileMultiblockMachineController> set : CONNECTED.values()) set.remove(controller);
    }

    private static void clearWorld(World world) {
        Set<TileMultiblockMachineController> loaded = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TileEntity tile : world.loadedTileEntityList) {
            if (tile instanceof TileMultiblockMachineController) loaded.add((TileMultiblockMachineController) tile);
        }
        for (TileMultiblockMachineController controller : loaded) clear(controller);
    }

    private static final class NetworkEndpoints {
        final List<TileMachineSignalInputHatch> inputs = new ArrayList<>();
        final List<TileMachineSignalOutputHatch> outputs = new ArrayList<>();
    }
}
