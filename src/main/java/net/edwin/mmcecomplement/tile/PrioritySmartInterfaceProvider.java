package net.edwin.mmcecomplement.tile;

/**
 * Marker for smart-interface components which should take precedence over
 * ordinary standalone smart interfaces in the same recipe group.
 */
public interface PrioritySmartInterfaceProvider {

    /** Whether this provider should suppress ordinary interfaces right now. */
    default boolean isPriorityActive() {
        return true;
    }
}
