package net.edwin.mmcecomplement.batch;

/** Runtime batch-hatch data exposed by formed machine controllers. */
public interface BatchController {

    /** Largest configured maximum batch duration among formed batch hatches. */
    int mmceComplement$getMaxBatchTime();
}
