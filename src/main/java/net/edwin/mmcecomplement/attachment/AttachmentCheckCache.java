package net.edwin.mmcecomplement.attachment;

/**
 * Tracks whether attachment patterns need another expensive world match.
 * World-change detection stays in the controller mixin; this class keeps the
 * state transition rules small and independently testable.
 */
public final class AttachmentCheckCache {

    public static final int DEFAULT_FALLBACK_INTERVAL = 100;

    private final int fallbackInterval;
    private boolean dirty = true;
    private boolean evaluated;
    private long lastRefreshWorldTime = Long.MIN_VALUE;

    public AttachmentCheckCache() {
        this(DEFAULT_FALLBACK_INTERVAL);
    }

    AttachmentCheckCache(int fallbackInterval) {
        if (fallbackInterval <= 0) {
            throw new IllegalArgumentException("fallbackInterval must be positive");
        }
        this.fallbackInterval = fallbackInterval;
    }

    public boolean shouldRefresh(long worldTime, boolean contextChanged) {
        return dirty || !evaluated || contextChanged || isFallbackDue(worldTime);
    }

    public void observeWorldChange(long worldTime, boolean areaChanged) {
        if (areaChanged || isFallbackDue(worldTime)) {
            dirty = true;
        }
    }

    public void markRefreshed(long worldTime, boolean allAreasLoaded) {
        evaluated = true;
        dirty = !allAreasLoaded;
        lastRefreshWorldTime = worldTime;
    }

    public void reset() {
        dirty = true;
        evaluated = false;
        lastRefreshWorldTime = Long.MIN_VALUE;
    }

    private boolean isFallbackDue(long worldTime) {
        return lastRefreshWorldTime != Long.MIN_VALUE
            && (worldTime < lastRefreshWorldTime
                || worldTime - lastRefreshWorldTime >= fallbackInterval);
    }
}
