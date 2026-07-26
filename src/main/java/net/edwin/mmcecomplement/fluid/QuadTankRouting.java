package net.edwin.mmcecomplement.fluid;

/** Pure routing logic shared by the four-slot fluid and gas handlers. */
public final class QuadTankRouting {

    private QuadTankRouting() {}

    /**
     * Existing matching material wins, including when its tank is full.
     * Otherwise the first unoccupied tank is selected.
     */
    public static int findFillTarget(boolean[] occupied, boolean[] matching) {
        if (occupied == null || matching == null || occupied.length != matching.length) {
            throw new IllegalArgumentException("Tank state arrays must have equal lengths");
        }
        for (int i = 0; i < matching.length; i++) {
            if (matching[i]) {
                return i;
            }
        }
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Output routing: a matching slot with room wins, then an empty slot.
     * Unlike input routing, a full matching slot does not block allocation of
     * another slot for the same material.
     */
    public static int findOutputFillTarget(boolean[] occupied, boolean[] matching,
                                           boolean[] hasRoom) {
        if (occupied == null || matching == null || hasRoom == null
                || occupied.length != matching.length
                || occupied.length != hasRoom.length) {
            throw new IllegalArgumentException("Tank state arrays must have equal lengths");
        }
        for (int i = 0; i < matching.length; i++) {
            if (matching[i] && hasRoom[i]) {
                return i;
            }
        }
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) {
                return i;
            }
        }
        return -1;
    }
}
