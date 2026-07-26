package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import net.minecraft.nbt.NBTTagCompound;

/** Stores the per-block maximum batch duration configured through the GUI. */
public class TileBatchHatch extends TileColorableMachineComponent {

    public static final int DEFAULT_MAX_BATCH_TIME = 600;
    private static final String TAG_MAX_BATCH_TIME = "maxBatchTime";

    private int maxBatchTime = DEFAULT_MAX_BATCH_TIME;

    public int getMaxBatchTime() {
        return maxBatchTime;
    }

    public void setMaxBatchTime(int maxBatchTime) {
        this.maxBatchTime = Math.max(0, maxBatchTime);
        markNoUpdateSync();
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.maxBatchTime = compound.hasKey(TAG_MAX_BATCH_TIME)
            ? Math.max(0, compound.getInteger(TAG_MAX_BATCH_TIME))
            : DEFAULT_MAX_BATCH_TIME;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger(TAG_MAX_BATCH_TIME, maxBatchTime);
    }
}
