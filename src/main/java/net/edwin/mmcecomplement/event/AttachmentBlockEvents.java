package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/** Invalidates attachment matching only for controllers watching a changed block. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class AttachmentBlockEvents {

    private static final Map<World, AttachmentControllerPositionIndex<AttachmentController>> INDEXES =
        new IdentityHashMap<>();

    private AttachmentBlockEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        BlockPos position = event.getPos();
        markControllers(world, position);
        TileEntity tile = world.getTileEntity(position);
        if (tile instanceof AttachmentController) {
            unregisterController(world, (AttachmentController) tile);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        BlockPos position = event.getPos();
        markControllers(world, position);
        TileEntity tile = world.getTileEntity(position);
        if (tile instanceof AttachmentController) {
            reindexController((AttachmentController) tile);
        }
    }

    /** Also catches non-player block replacements and neighbour updates. */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        markControllers(event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        for (TileEntity tile : event.getChunk().getTileEntityMap().values()) {
            if (tile instanceof AttachmentController) {
                reindexController((AttachmentController) tile);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        World world = event.getWorld();
        for (TileEntity tile : event.getChunk().getTileEntityMap().values()) {
            if (tile instanceof AttachmentController) {
                unregisterController(world, (AttachmentController) tile);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        synchronized (INDEXES) {
            INDEXES.remove(event.getWorld());
        }
    }

    /** Replaces the previous candidate positions after machine context changes. */
    public static void reindexController(AttachmentController controller) {
        if (!(controller instanceof TileEntity)) {
            return;
        }
        TileEntity tile = (TileEntity) controller;
        World world = tile.getWorld();
        if (world == null || world.isRemote || tile.isInvalid()) {
            unregisterControllerWithoutWorld(controller);
            return;
        }
        Collection<StructureBoundingBox> bounds =
            controller.mmceComplement$getAttachmentBounds();
        if (bounds == null || bounds.isEmpty()) {
            unregisterController(world, controller);
            return;
        }
        getIndex(world, true).replace(controller, bounds);
    }

    public static void unregisterController(AttachmentController controller) {
        if (!(controller instanceof TileEntity)) {
            return;
        }
        World world = ((TileEntity) controller).getWorld();
        if (world == null) {
            unregisterControllerWithoutWorld(controller);
        } else {
            unregisterController(world, controller);
        }
    }

    private static void markControllers(World world, BlockPos changedPosition) {
        if (world == null || world.isRemote || changedPosition == null) {
            return;
        }
        AttachmentControllerPositionIndex<AttachmentController> index = getIndex(world, false);
        if (index != null) {
            index.forEachAt(changedPosition, controller ->
                controller.mmceComplement$markAttachmentDirty(changedPosition));
        }
    }

    private static void unregisterController(World world,
                                             AttachmentController controller) {
        if (world == null || controller == null) {
            return;
        }
        synchronized (INDEXES) {
            AttachmentControllerPositionIndex<AttachmentController> index = INDEXES.get(world);
            if (index != null) {
                index.remove(controller);
                if (index.isEmpty()) {
                    INDEXES.remove(world);
                }
            }
        }
    }

    private static void unregisterControllerWithoutWorld(
        AttachmentController controller) {
        synchronized (INDEXES) {
            java.util.Iterator<Map.Entry<World, AttachmentControllerPositionIndex<AttachmentController>>> it =
                INDEXES.entrySet().iterator();
            while (it.hasNext()) {
                AttachmentControllerPositionIndex<AttachmentController> index = it.next().getValue();
                index.remove(controller);
                if (index.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private static AttachmentControllerPositionIndex<AttachmentController> getIndex(
        World world, boolean create) {
        synchronized (INDEXES) {
            AttachmentControllerPositionIndex<AttachmentController> index = INDEXES.get(world);
            if (index == null && create) {
                index = new AttachmentControllerPositionIndex<>();
                INDEXES.put(world, index);
            }
            return index;
        }
    }
}
