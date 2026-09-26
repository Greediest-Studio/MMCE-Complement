package net.edwin.mmcecomplement.compat.mmcea.tile;

import github.alecsio.mmceaddons.common.hatch.vanilla.RequirementDimension;
import github.alecsio.mmceaddons.common.hatch.vanilla.TileDimensionProvider;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import net.minecraft.nbt.NBTTagCompound;

/** Optional MMCE Addons dimension hatch with a persisted dimension override. */
public class TileConfigurableDimensionProvider extends TileDimensionProvider {

    private static final String TAG_BOUND_DIMENSION = "BoundDimension";
    private Integer boundDimensionId;
    private int snapshotDimensionId;

    @Override
    protected void updateSnapshot() {
        int dimension = boundDimensionId != null
                ? boundDimensionId : (world == null ? 0 : world.provider.getDimension());
        lock.writeLock().lock();
        try {
            snapshotDimensionId = dimension;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected CraftCheck checkSnapshot(RequirementDimension requirement) {
        return snapshotDimensionId == requirement.getDimension().getId()
                ? CraftCheck.success()
                : CraftCheck.failure("error.modularmachineryaddons.requirement.missing.dimension");
    }

    public void bindDimension(int dimensionId) {
        boundDimensionId = dimensionId;
        updateSnapshot();
        markForUpdate();
    }

    public Integer getBoundDimensionId() {
        return boundDimensionId;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        if (boundDimensionId != null) {
            compound.setInteger(TAG_BOUND_DIMENSION, boundDimensionId);
        } else {
            compound.removeTag(TAG_BOUND_DIMENSION);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        boundDimensionId = compound.hasKey(TAG_BOUND_DIMENSION)
                ? compound.getInteger(TAG_BOUND_DIMENSION) : null;
    }
}
