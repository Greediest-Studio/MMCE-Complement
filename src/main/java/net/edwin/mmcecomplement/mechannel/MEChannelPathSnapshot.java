package net.edwin.mmcecomplement.mechannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable view of one channel hatch's route to its AE network. */
public final class MEChannelPathSnapshot {

    public enum State {
        READY,
        DISCONNECTED,
        BOOTING,
        CONTROLLER_CONFLICT
    }

    private final State state;
    private final Object networkIdentity;
    private final List<Capacity> capacities;

    private MEChannelPathSnapshot(State state, Object networkIdentity,
                                  List<Capacity> capacities) {
        this.state = state;
        this.networkIdentity = networkIdentity;
        this.capacities = Collections.unmodifiableList(
            new ArrayList<>(capacities));
    }

    public static MEChannelPathSnapshot ready(Object networkIdentity,
                                               List<Capacity> capacities) {
        return new MEChannelPathSnapshot(State.READY, networkIdentity,
            capacities);
    }

    public static MEChannelPathSnapshot unavailable(State state,
                                                     Object networkIdentity) {
        if (state == State.READY) {
            throw new IllegalArgumentException("READY snapshots need capacities");
        }
        return new MEChannelPathSnapshot(state, networkIdentity,
            Collections.emptyList());
    }

    public State getState() {
        return state;
    }

    public Object getNetworkIdentity() {
        return networkIdentity;
    }

    public List<Capacity> getCapacities() {
        return capacities;
    }

    /**
     * A shared route resource. Identity equality of {@code key} is used so a
     * cable segment shared by several hatches is counted only once.
     */
    public static final class Capacity {
        private final Object key;
        private final int available;

        public Capacity(Object key, int available) {
            if (key == null) {
                throw new IllegalArgumentException("capacity key cannot be null");
            }
            this.key = key;
            this.available = Math.max(0, available);
        }

        public Object getKey() {
            return key;
        }

        public int getAvailable() {
            return available;
        }
    }
}
