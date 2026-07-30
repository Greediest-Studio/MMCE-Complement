package net.edwin.mmcecomplement.cycle;

/** Marker used to order self-cycle assembly recipe components. */
public interface CycleComponentHandler {
    Mode mmceComplement$getCycleMode();

    enum Mode {
        INPUT,
        OUTPUT
    }
}
