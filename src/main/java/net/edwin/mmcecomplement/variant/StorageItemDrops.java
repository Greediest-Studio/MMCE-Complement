package net.edwin.mmcecomplement.variant;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Item ejection shared by storage controller harvesting and capacity changes. */
public final class StorageItemDrops {
    private StorageItemDrops() { }

    public static boolean drop(World world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (world == null || world.isRemote) return false;
        EntityItem entity = new EntityItem(world,
            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy());
        entity.motionX = (world.rand.nextDouble() - 0.5D) * 0.15D;
        entity.motionY = world.rand.nextDouble() * 0.2D + 0.1D;
        entity.motionZ = (world.rand.nextDouble() - 0.5D) * 0.15D;
        return world.spawnEntity(entity);
    }
}
