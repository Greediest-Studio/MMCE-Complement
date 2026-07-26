package net.edwin.mmcecomplement.batch;

/** Per-active-recipe state used to keep a batch factor stable while running. */
public interface BatchRecipeData {

    int mmceComplement$getOrCalculateBatchFactor(float theoreticalDuration,
                                                  int maxBatchTime);

    void mmceComplement$beginBatchCalculation(int baseMaxParallelism);

    void mmceComplement$finishBatchCalculation();

    void mmceComplement$adjustBatchFactorToActualParallelism(int actualParallelism);

    int mmceComplement$getBaseMaxParallelism();
}
