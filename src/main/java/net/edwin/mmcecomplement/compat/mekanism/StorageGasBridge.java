package net.edwin.mmcecomplement.compat.mekanism;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/** Reflection boundary keeping Mekanism classes out of common GUI/container code. */
public final class StorageGasBridge {
    private static final String TYPE = "net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler";
    private StorageGasBridge() {}
    public static void sort(Object handler) {
        if (handler == null) return;
        try {
            handler.getClass().getMethod("sort").invoke(handler);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Storage gas sorting failed", e);
        }
    }
    public static boolean transfer(Object source, Object destination, Runnable changed) {
        try {
            return (Boolean) Class.forName("net.edwin.mmcecomplement.compat.mekanism.StorageGasTransfer")
                .getMethod("transfer", Object.class, Object.class, Runnable.class)
                .invoke(null, source, destination, changed);
        } catch (ReflectiveOperationException e) {
            // Never hide a failed transfer: its source may already have changed.
            throw new IllegalStateException("Storage gas transfer failed", e);
        }
    }
    public static Object create(Object owner, int slots, int capacity) {
        try { return Class.forName(TYPE).getConstructor(net.minecraft.tileentity.TileEntity.class, int.class, int.class).newInstance(owner, slots, capacity); }
        catch (Throwable ignored) { return null; }
    }
    public static Object stack(Object handler, int index) { try { return handler.getClass().getMethod("getStack", int.class).invoke(handler,index); } catch(Throwable e){return null;} }
    public static net.minecraft.util.ResourceLocation texture(Object handler, int index) { try { return (net.minecraft.util.ResourceLocation)handler.getClass().getMethod("getTexture", int.class).invoke(handler,index); } catch(ReflectiveOperationException e){return null;} }
    public static int tint(Object handler, int index) { try { return ((Number)handler.getClass().getMethod("getTint", int.class).invoke(handler,index)).intValue(); } catch(ReflectiveOperationException e){return 0xFFFFFF;} }
    public static String name(Object handler, int index) { try { return (String)handler.getClass().getMethod("getDisplayName", int.class).invoke(handler,index); } catch(ReflectiveOperationException e){return null;} }
    public static int amount(Object handler, int index) { try { return ((Number)handler.getClass().getMethod("getAmount", int.class).invoke(handler,index)).intValue(); } catch(Throwable e){return 0;} }
    public static int capacity(Object handler) { try { return ((Number)handler.getClass().getMethod("getCapacity").invoke(handler)).intValue(); } catch(Throwable e){return 0;} }
    public static boolean interact(Object handler, EntityPlayer player, int index) { try { return (Boolean)handler.getClass().getMethod("interact", EntityPlayer.class,int.class).invoke(handler,player,index); } catch(Throwable e){return false;} }
    public static NBTTagCompound serialize(Object handler) { try { return (NBTTagCompound)handler.getClass().getMethod("serializeNBT").invoke(handler); } catch(Throwable e){return new NBTTagCompound();} }
    public static void deserialize(Object handler, NBTTagCompound nbt) { try { handler.getClass().getMethod("deserializeNBT", NBTTagCompound.class).invoke(handler,nbt); } catch(Throwable ignored) {} }
}
