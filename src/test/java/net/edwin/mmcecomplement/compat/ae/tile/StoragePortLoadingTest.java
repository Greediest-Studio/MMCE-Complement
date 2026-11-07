package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Settings;
import appeng.util.ConfigManager;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class StoragePortLoadingTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void nativeSettingsRestoreDoesNotReenterChunkLoadingAndSavesOnFirstTick() throws Exception {
        ProbePort port = new ProbePort();
        // A world is already assigned during TileEntity.create/readFromNBT.
        // A null-world guard alone therefore cannot fix the reported recursion.
        port.testWorld = worldWithoutChunks();
        ConfigManager manager = new ConfigManager((config, setting, value) -> {
            // PartStorageBus.updateSetting calls resetCache and host.markForSave.
            port.storageConfigurationChanged();
            port.markForSave();
        });
        manager.registerSetting(Settings.ACCESS, AccessRestriction.READ_WRITE);
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("ACCESS", AccessRestriction.READ.name());
        manager.readFromNBT(saved);
        assertEquals(AccessRestriction.READ, manager.getSetting(Settings.ACCESS));
        assertEquals(0, port.saves, "Deserialization must not touch a loading chunk");

        // Additional callbacks are coalesced; persistence must work even when
        // the AE proxy has no active grid, power, or channels yet.
        port.partChanged();
        port.saveChanges(null);
        port.update();
        assertEquals(1, port.saves);
        port.update();
        assertEquals(1, port.saves);
        port.storageConfigurationChanged();
        assertEquals(1, port.saves);
        port.update();
        assertEquals(2, port.saves, "Live GUI settings must still be saved");
    }

    @Test void callbacksBeforeWorldAssignmentAreSafe() {
        ProbePort port = new ProbePort();
        port.storageConfigurationChanged();
        port.markForSave();
        port.update();
        assertEquals(0, port.saves);
    }

    private static World worldWithoutChunks() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field unsafe = unsafeType.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        return (World) unsafeType.getMethod("allocateInstance", Class.class)
            .invoke(unsafe.get(null), WorldServer.class);
    }

    private static final class ProbePort extends TileAEStorageAccessPort {
        World testWorld;
        int saves;
        @Override public World getWorld() { return testWorld; }
        @Override public void markDirty() { saves++; }
    }
}
