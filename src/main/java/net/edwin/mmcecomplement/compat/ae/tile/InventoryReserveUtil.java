package net.edwin.mmcecomplement.compat.ae.tile;

/** Arithmetic shared by ME inventory buses which retain stock in the grid. */
public final class InventoryReserveUtil {

    private InventoryReserveUtil() { }

    public static long clamp(long reserve) {
        return Math.max(0L, reserve);
    }

    /**
     * Returns how much may be extracted without taking the ME inventory below
     * {@code reserve}, additionally capped by the receiving buffer's room.
     */
    public static long extractable(long networkAmount, long reserve,
                                   long bufferRoom) {
        if (networkAmount <= 0L || bufferRoom <= 0L) return 0L;
        long retained = clamp(reserve);
        if (networkAmount <= retained) return 0L;
        return Math.min(networkAmount - retained, bufferRoom);
    }
}
