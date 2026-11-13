package net.edwin.mmcecomplement.event;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Reverse index from candidate attachment blocks to their loaded controllers. */
final class AttachmentControllerPositionIndex<T> {

    private final Map<Long, Set<T>> controllersByPosition = new HashMap<>();
    private final IdentityHashMap<T, Set<Long>> positionsByController =
        new IdentityHashMap<>();

    synchronized void replace(T controller,
                              Collection<StructureBoundingBox> bounds) {
        Set<Long> next = new LinkedHashSet<>();
        if (bounds != null) {
            for (StructureBoundingBox bound : bounds) {
                if (bound == null) {
                    continue;
                }
                for (int chunkX = bound.minX >> 4; chunkX <= bound.maxX >> 4; chunkX++) {
                    for (int chunkZ = bound.minZ >> 4; chunkZ <= bound.maxZ >> 4; chunkZ++) {
                        next.add(chunkKey(chunkX, chunkZ));
                    }
                }
            }
        }
        Set<Long> previous = positionsByController.get(controller);
        if (next.equals(previous)) {
            return;
        }
        remove(controller);
        if (next.isEmpty()) {
            return;
        }
        positionsByController.put(controller, next);
        for (Long position : next) {
            controllersByPosition.computeIfAbsent(position, ignored ->
                Collections.newSetFromMap(new IdentityHashMap<T, Boolean>())).add(controller);
        }
    }

    synchronized void remove(T controller) {
        Set<Long> positions = positionsByController.remove(controller);
        if (positions == null) {
            return;
        }
        for (Long position : positions) {
            Set<T> controllers = controllersByPosition.get(position);
            if (controllers != null) {
                controllers.remove(controller);
                if (controllers.isEmpty()) {
                    controllersByPosition.remove(position);
                }
            }
        }
    }

    synchronized void forEachAt(BlockPos position, Consumer<T> action) {
        Set<T> controllers = controllersByPosition.get(chunkKey(
            position.getX() >> 4, position.getZ() >> 4));
        if (controllers != null) {
            for (T controller : controllers) {
                action.accept(controller);
            }
        }
    }

    synchronized boolean isEmpty() {
        return positionsByController.isEmpty();
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
