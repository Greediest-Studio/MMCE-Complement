package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssemblyFallback;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Isolates the Mekanism-dependent full-exposure tile class. */
public final class AeFullExposureTileFactory {

    private AeFullExposureTileFactory() {}

    public static TileEntity create() {
        if (!CompatMods.isAeGasCompatLoaded()) {
            return new TileMEFullExposureAssemblyFallback();
        }
        try {
            return (TileEntity) Class.forName(
                "net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly",
                true, AeFullExposureTileFactory.class.getClassLoader())
                .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError failure) {
            return new TileMEFullExposureAssemblyFallback();
        }
    }

    public static Class<? extends TileEntity> tileClass() {
        if (CompatMods.isAeGasCompatLoaded()) {
            try {
                return Class.forName(
                    "net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly",
                    false, AeFullExposureTileFactory.class.getClassLoader())
                    .asSubclass(TileEntity.class);
            } catch (ClassNotFoundException | LinkageError ignored) { }
        }
        return TileMEFullExposureAssemblyFallback.class;
    }

    public static boolean isFullExposureTile(TileEntity tile) {
        return tile != null && (tile instanceof TileMEFullExposureAssemblyFallback
            || tile.getClass().getName().equals(
                "net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly"));
    }

    public static void writeDropNBT(TileEntity tile, NBTTagCompound tag) {
        invokeDropMethod(tile, "writeDropNBT", tag);
    }

    public static void readDropNBT(TileEntity tile, NBTTagCompound tag) {
        invokeDropMethod(tile, "readDropNBT", tag);
    }

    private static void invokeDropMethod(TileEntity tile, String method,
                                         NBTTagCompound tag) {
        try {
            tile.getClass().getMethod(method, NBTTagCompound.class)
                .invoke(tile, tag);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
