package net.edwin.mmcecomplement.event;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.attachment.AttachmentController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Invalidates attachment matching when a block in a loaded world changes.
 * MMCE's own area listener only drives its delayed main-structure check, so
 * attachment modules need this small, independent notification path.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class AttachmentBlockEvents {

    private AttachmentBlockEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        markControllers(event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        markControllers(event.getWorld(), event.getPos());
    }

    /** Also catches non-player block replacements and neighbour updates. */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        markControllers(event.getWorld(), event.getPos());
    }

    private static void markControllers(World world, BlockPos changedPosition) {
        if (world == null || world.isRemote || changedPosition == null) {
            return;
        }
        // Block changes are infrequent compared with controller ticks. The
        // controller performs the cheap bounding-box test before setting its
        // dirty bit, so this remains safe even with many loaded machines.
        for (TileEntity tile : world.loadedTileEntityList) {
            if (tile instanceof AttachmentController) {
                ((AttachmentController) tile)
                    .mmceComplement$markAttachmentDirty(changedPosition);
            }
        }
    }
}
