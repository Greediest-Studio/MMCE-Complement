package net.edwin.mmcecomplement.mechannel;

/**
 * Machine-facing abstraction for an AE channel reservation endpoint.
 *
 * <p>The recipe implementation deliberately depends on this interface rather
 * than AE2 classes so recipe parsing and MMCE's registries remain loadable
 * when AE2 is absent.</p>
 */
public interface MEChannelProvider {

    /** Returns the identity of the currently connected AE grid, or null. */
    Object getMEChannelNetworkIdentity();

    /** Captures the currently available capacity along this hatch's AE path. */
    MEChannelPathSnapshot snapshotMEChannelPath();

    /** Requests a real AE channel reservation for the supplied owner. */
    boolean requestMEChannels(Object owner, int amount);

    /** Releases the reservation if it belongs to the supplied owner. */
    void releaseMEChannels(Object owner);

    /** True only while every requested channel is active in AE2's grid. */
    boolean isMEChannelReservationSatisfied(Object owner, int amount);
}
