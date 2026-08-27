package net.edwin.mmcecomplement.compat.ae.tile;

/** Shared active-pull state exposed by all inventory ME input buses. */
public interface MEInventoryInputBus {

    String TAG_PERMANENT_RESERVE = "inventoryPermanentReserve";

    boolean isActivePull();

    void setActivePull(boolean activePull);

    /** Applies an optimistic GUI state without mutating server inventory. */
    void setClientActivePull(boolean activePull);

    long getPermanentReserve();

    void setPermanentReserve(long permanentReserve);

    /** Applies an optimistic GUI value while the server packet is in flight. */
    void setClientPermanentReserve(long permanentReserve);
}
