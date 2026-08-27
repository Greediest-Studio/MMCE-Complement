package net.edwin.mmcecomplement.mechannel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MEChannelAllocationPlannerTest {

    @Test
    void allocatesAcrossHatchesWithoutDoubleCountingSharedCable() {
        Object network = new Object();
        Object sharedCable = new Object();
        FakeProvider first = provider(network,
            capacity(sharedCable, 8), capacity(new Object(), 5));
        FakeProvider second = provider(network,
            capacity(sharedCable, 8), capacity(new Object(), 8));

        MEChannelAllocationPlanner.Result result =
            MEChannelAllocationPlanner.plan(Arrays.asList(first, second), 8);

        assertTrue(result.isSuccess());
        assertEquals(8, result.getAssignments().values().stream()
            .mapToInt(Integer::intValue).sum());
        assertEquals(5, result.getAssignments().get(first));
        assertEquals(3, result.getAssignments().get(second));

        MEChannelAllocationPlanner.Result overCapacity =
            MEChannelAllocationPlanner.plan(Arrays.asList(first, second), 9);
        assertFalse(overCapacity.isSuccess());
        assertEquals(MEChannelAllocationPlanner.Failure.INSUFFICIENT_CHANNELS,
            overCapacity.getFailure());
    }

    @Test
    void rejectsHatchesOnDifferentNetworks() {
        FakeProvider first = provider(new Object(),
            capacity(new Object(), 8));
        FakeProvider second = provider(new Object(),
            capacity(new Object(), 8));

        MEChannelAllocationPlanner.Result result =
            MEChannelAllocationPlanner.plan(Arrays.asList(first, second), 1);

        assertFalse(result.isSuccess());
        assertEquals(MEChannelAllocationPlanner.Failure.NETWORK_MISMATCH,
            result.getFailure());
    }

    @Test
    void reportsNetworkRecalculationSeparately() {
        Object network = new Object();
        FakeProvider provider = new FakeProvider(
            MEChannelPathSnapshot.unavailable(
                MEChannelPathSnapshot.State.BOOTING, network));

        MEChannelAllocationPlanner.Result result =
            MEChannelAllocationPlanner.plan(Arrays.asList(provider), 1);

        assertFalse(result.isSuccess());
        assertEquals(MEChannelAllocationPlanner.Failure.NETWORK_BOOTING,
            result.getFailure());
    }

    private static FakeProvider provider(Object network,
                                         MEChannelPathSnapshot.Capacity... path) {
        return new FakeProvider(MEChannelPathSnapshot.ready(network,
            Arrays.asList(path)));
    }

    private static MEChannelPathSnapshot.Capacity capacity(Object key,
                                                            int available) {
        return new MEChannelPathSnapshot.Capacity(key, available);
    }

    private static final class FakeProvider implements MEChannelProvider {
        private final MEChannelPathSnapshot snapshot;

        private FakeProvider(MEChannelPathSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override public Object getMEChannelNetworkIdentity() {
            return snapshot.getNetworkIdentity();
        }
        @Override public MEChannelPathSnapshot snapshotMEChannelPath() {
            return snapshot;
        }
        @Override public boolean requestMEChannels(Object owner, int amount) {
            return true;
        }
        @Override public void releaseMEChannels(Object owner) { }
        @Override public boolean isMEChannelReservationSatisfied(
            Object owner, int amount) {
            return true;
        }
    }
}
