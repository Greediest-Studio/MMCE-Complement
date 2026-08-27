package net.edwin.mmcecomplement.redstone;

/** Pure threshold rules shared by the redstone control hatch and its tests. */
public final class RedstoneControlLogic {

    public static final int MIN_THRESHOLD = 1;
    public static final int MAX_THRESHOLD = 15;

    private RedstoneControlLogic() {}

    public static int clampThreshold(int threshold) {
        return Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold));
    }

    public static int nextThreshold(int threshold) {
        int current = clampThreshold(threshold);
        return current >= MAX_THRESHOLD ? MIN_THRESHOLD : current + 1;
    }

    public static boolean shouldShutdown(int receivedSignal, int threshold) {
        return receivedSignal >= clampThreshold(threshold);
    }
}
