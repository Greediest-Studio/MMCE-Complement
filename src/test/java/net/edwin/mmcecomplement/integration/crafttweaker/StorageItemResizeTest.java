package net.edwin.mmcecomplement.integration.crafttweaker;

import net.edwin.mmcecomplement.block.BlockStorageController;
import net.edwin.mmcecomplement.variant.StorageItemHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StorageItemResizeTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void tailStacksUseRetainedEmptySlotsInOrderWithMetadataAndNbt() throws Exception {
        Fixture f = fixture(6);
        f.items.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
        ItemStack named = new ItemStack(Items.DYE, 12, 4);
        named.setTagCompound(new NBTTagCompound());
        named.getTagCompound().setString("name", "retained");
        f.items.setStackInSlot(4, named);
        f.items.setStackInSlot(5, new ItemStack(Items.EMERALD, 3));
        int changes = f.controller.changes;
        assertTrue(f.items.resize(3));
        assertEquals(1, f.items.getStackInSlot(0).getCount());
        assertTrue(ItemStack.areItemStacksEqual(named, f.items.getStackInSlot(1)));
        assertEquals(Items.EMERALD, f.items.getStackInSlot(2).getItem());
        assertEquals(3, f.items.getStackInSlot(2).getCount());
        assertEquals(changes + 1, f.controller.changes);
        assertTrue(f.world.drops.isEmpty());
        assertTrue(f.items.resize(3));
        assertEquals(changes + 1, f.controller.changes);
    }

    @Test void overflowIsEjectedOnceAndNeverMergedIntoOccupiedSlots() throws Exception {
        Fixture f = fixture(6);
        f.items.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
        f.items.setStackInSlot(4, new ItemStack(Items.DIAMOND, 130));
        f.items.setStackInSlot(5, new ItemStack(Items.EMERALD, 3));
        f.world.onSpawn = () -> {
            assertEquals(3, f.items.getSlots());
            assertFalse(f.items.resize(2), "Entity callbacks cannot start another resize");
        };
        assertTrue(f.items.resize(3));
        assertEquals(1, f.items.getStackInSlot(0).getCount());
        assertEquals(64, f.items.getStackInSlot(1).getCount());
        assertEquals(64, f.items.getStackInSlot(2).getCount());
        assertEquals(2, f.world.drops.size());
        assertEquals(2, f.world.drops.get(0).getItem().getCount());
        assertEquals(Items.EMERALD, f.world.drops.get(1).getItem().getItem());
        assertEquals(3, f.world.drops.get(1).getItem().getCount());
        f.world.onSpawn = null;
        assertTrue(f.items.resize(3));
        assertTrue(f.items.resize(6));
        assertTrue(f.items.getStackInSlot(4).isEmpty());
        assertTrue(f.items.getStackInSlot(5).isEmpty());
        assertEquals(2, f.world.drops.size());
        // Harvesting uses exactly the same ejection position and motion.
        f.world.rand.setSeed(73);
        BlockStorageController.dropInventory(f.world, f.controller.getPos(), f.items);
        EntityItem harvested = f.world.drops.get(2);
        EntityItem overflow = f.world.drops.get(0);
        assertEquals(overflow.posX, harvested.posX);
        assertEquals(overflow.posY, harvested.posY);
        assertEquals(overflow.posZ, harvested.posZ);
        assertEquals(overflow.motionX, harvested.motionX);
        assertEquals(overflow.motionY, harvested.motionY);
        assertEquals(overflow.motionZ, harvested.motionZ);
    }

    @Test void refusedDropKeepsUndroppedRemainderAndCanBeRetried() throws Exception {
        Fixture f = fixture(5);
        f.items.setStackInSlot(0, new ItemStack(Items.EMERALD, 64));
        f.items.setStackInSlot(3, new ItemStack(Items.DIAMOND, 70));
        f.items.setStackInSlot(4, new ItemStack(Items.GOLD_INGOT, 8));
        f.world.accept = false;
        assertFalse(f.items.resize(2));
        assertEquals(5, f.items.getSlots());
        assertEquals(64, f.items.getStackInSlot(1).getCount());
        assertEquals(6, f.items.getStackInSlot(3).getCount());
        assertEquals(8, f.items.getStackInSlot(4).getCount());
        assertTrue(f.world.drops.isEmpty());
        f.world.accept = true;
        assertTrue(f.items.resize(2));
        assertEquals(2, f.world.drops.size());
        assertEquals(6, f.world.drops.get(0).getItem().getCount());
        assertEquals(8, f.world.drops.get(1).getItem().getCount());
    }

    @Test void laterDropFailureDoesNotRestoreAlreadyEjectedItems() throws Exception {
        Fixture f = fixture(3);
        f.items.setStackInSlot(0, new ItemStack(Items.EMERALD, 64));
        f.items.setStackInSlot(1, new ItemStack(Items.DIAMOND, 2));
        f.items.setStackInSlot(2, new ItemStack(Items.GOLD_INGOT, 3));
        f.world.onSpawn = () -> {
            f.world.accept = f.world.drops.isEmpty();
            if (!f.world.accept) f.items.extractItem(0, 1, false);
        };
        assertFalse(f.items.resize(1));
        assertEquals(3, f.items.getSlots());
        assertEquals(63, f.items.getStackInSlot(0).getCount());
        assertTrue(f.items.getStackInSlot(1).isEmpty());
        assertEquals(3, f.items.getStackInSlot(2).getCount());
        assertEquals(1, f.world.drops.size());
        f.world.onSpawn = null;
        f.world.accept = true;
        assertTrue(f.items.resize(1));
        assertEquals(2, f.world.drops.size());
        assertEquals(Items.DIAMOND, f.world.drops.get(0).getItem().getItem());
        assertEquals(Items.GOLD_INGOT, f.world.drops.get(1).getItem().getItem());
    }

    private static Fixture fixture(int slots) throws Exception {
        CaptureWorld world = allocate(CaptureWorld.class);
        world.drops = new ArrayList<>();
        world.accept = true;
        Field random = World.class.getDeclaredField("rand");
        random.setAccessible(true);
        random.set(world, new Random(73));
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, new WorldProviderSurface());
        MachineStorageControllerTest.FakeController controller = allocate(MachineStorageControllerTest.FakeController.class);
        controller.setWorld(world);
        controller.setPos(new BlockPos(7, 21, -9));
        StorageItemHandler items = new StorageItemHandler(controller, slots);
        return new Fixture(world, controller, items);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class Fixture {
        final CaptureWorld world;
        final MachineStorageControllerTest.FakeController controller;
        final StorageItemHandler items;
        Fixture(CaptureWorld world, MachineStorageControllerTest.FakeController controller, StorageItemHandler items) {
            this.world = world; this.controller = controller; this.items = items;
        }
    }

    private static final class CaptureWorld extends WorldServer {
        List<EntityItem> drops;
        boolean accept;
        Runnable onSpawn;
        private CaptureWorld() { super(null, null, null, 0, null); }
        @Override public boolean spawnEntity(Entity entity) {
            if (onSpawn != null) onSpawn.run();
            if (accept) drops.add((EntityItem) entity);
            return accept;
        }
    }
}
