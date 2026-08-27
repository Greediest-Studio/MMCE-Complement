package net.edwin.mmcecomplement.mechannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Combines hatch paths without double-counting shared cables. */
public final class MEChannelAllocationPlanner {

    private MEChannelAllocationPlanner() { }

    public static Result plan(Collection<MEChannelProvider> candidates,
                              int requested) {
        if (requested <= 0) {
            return Result.failure(Failure.INSUFFICIENT_CHANNELS);
        }

        IdentityHashMap<MEChannelProvider, Boolean> seen =
            new IdentityHashMap<>();
        List<ProviderPath> paths = new ArrayList<>();
        Object network = null;

        for (MEChannelProvider provider : candidates) {
            if (provider == null || seen.put(provider, Boolean.TRUE) != null) {
                continue;
            }
            MEChannelPathSnapshot snapshot = provider.snapshotMEChannelPath();
            Failure stateFailure = failureFor(snapshot.getState());
            if (stateFailure != null) {
                return Result.failure(stateFailure);
            }
            Object candidateNetwork = snapshot.getNetworkIdentity();
            if (candidateNetwork == null) {
                return Result.failure(Failure.NETWORK_UNAVAILABLE);
            }
            if (network == null) {
                network = candidateNetwork;
            } else if (network != candidateNetwork) {
                return Result.failure(Failure.NETWORK_MISMATCH);
            }

            IdentityHashMap<Object, Boolean> pathSeen =
                new IdentityHashMap<>();
            List<MEChannelPathSnapshot.Capacity> unique = new ArrayList<>();
            for (MEChannelPathSnapshot.Capacity capacity
                : snapshot.getCapacities()) {
                if (pathSeen.put(capacity.getKey(), Boolean.TRUE) == null) {
                    unique.add(capacity);
                }
            }
            if (unique.isEmpty()) {
                return Result.failure(Failure.NETWORK_UNAVAILABLE);
            }
            paths.add(new ProviderPath(provider, unique));
        }

        if (paths.isEmpty()) {
            return Result.failure(Failure.NETWORK_UNAVAILABLE);
        }

        IdentityHashMap<Object, Integer> remainingByResource =
            new IdentityHashMap<>();
        IdentityHashMap<MEChannelProvider, Integer> assignments =
            new IdentityHashMap<>();
        int remainingRequest = requested;

        for (ProviderPath path : paths) {
            int bottleneck = Integer.MAX_VALUE;
            for (MEChannelPathSnapshot.Capacity capacity : path.capacities) {
                Integer remaining = remainingByResource.get(capacity.getKey());
                if (remaining == null) {
                    remaining = capacity.getAvailable();
                    remainingByResource.put(capacity.getKey(), remaining);
                }
                bottleneck = Math.min(bottleneck, remaining);
            }

            int allocated = Math.min(remainingRequest,
                Math.max(0, bottleneck));
            if (allocated > 0) {
                assignments.put(path.provider, allocated);
                for (MEChannelPathSnapshot.Capacity capacity
                    : path.capacities) {
                    Object key = capacity.getKey();
                    remainingByResource.put(key,
                        remainingByResource.get(key) - allocated);
                }
                remainingRequest -= allocated;
                if (remainingRequest == 0) {
                    break;
                }
            }
        }

        if (remainingRequest > 0) {
            return Result.failure(Failure.INSUFFICIENT_CHANNELS);
        }
        return Result.success(assignments, network);
    }

    private static Failure failureFor(MEChannelPathSnapshot.State state) {
        switch (state) {
            case READY:
                return null;
            case BOOTING:
                return Failure.NETWORK_BOOTING;
            case CONTROLLER_CONFLICT:
                return Failure.CONTROLLER_CONFLICT;
            case DISCONNECTED:
            default:
                return Failure.NETWORK_UNAVAILABLE;
        }
    }

    public enum Failure {
        NETWORK_UNAVAILABLE,
        NETWORK_BOOTING,
        CONTROLLER_CONFLICT,
        NETWORK_MISMATCH,
        INSUFFICIENT_CHANNELS
    }

    public static final class Result {
        private final Failure failure;
        private final Map<MEChannelProvider, Integer> assignments;
        private final Object networkIdentity;

        private Result(Failure failure,
                       Map<MEChannelProvider, Integer> assignments,
                       Object networkIdentity) {
            this.failure = failure;
            this.assignments = assignments;
            this.networkIdentity = networkIdentity;
        }

        private static Result failure(Failure failure) {
            return new Result(failure, Collections.emptyMap(), null);
        }

        private static Result success(
            IdentityHashMap<MEChannelProvider, Integer> assignments,
            Object networkIdentity) {
            return new Result(null,
                Collections.unmodifiableMap(new IdentityHashMap<>(assignments)),
                networkIdentity);
        }

        public boolean isSuccess() {
            return failure == null;
        }

        public Failure getFailure() {
            return failure;
        }

        public Map<MEChannelProvider, Integer> getAssignments() {
            return assignments;
        }

        public Object getNetworkIdentity() {
            return networkIdentity;
        }
    }

    private static final class ProviderPath {
        private final MEChannelProvider provider;
        private final List<MEChannelPathSnapshot.Capacity> capacities;

        private ProviderPath(MEChannelProvider provider,
                             List<MEChannelPathSnapshot.Capacity> capacities) {
            this.provider = provider;
            this.capacities = capacities;
        }
    }
}
