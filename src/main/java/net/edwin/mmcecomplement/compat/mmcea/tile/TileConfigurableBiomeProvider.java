package net.edwin.mmcecomplement.compat.mmcea.tile;

import github.alecsio.mmceaddons.common.hatch.vanilla.RequirementBiome;
import github.alecsio.mmceaddons.common.hatch.vanilla.TileBiomeProvider;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** Optional MMCE Addons biome hatch with a persisted biome override. */
public class TileConfigurableBiomeProvider extends TileBiomeProvider {

    private static final String TAG_BOUND_BIOME = "BoundBiome";
    private String boundBiomeRegistryName;
    private String snapshotBiomeRegistryName;

    @Override
    protected void updateSnapshot() {
        String biome = boundBiomeRegistryName;
        if (biome == null && world != null) {
            ResourceLocation registryName = world.getBiome(getPos()).getRegistryName();
            biome = registryName == null ? null : registryName.toString();
        }
        lock.writeLock().lock();
        try {
            snapshotBiomeRegistryName = biome;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected CraftCheck checkSnapshot(RequirementBiome requirement) {
        return snapshotBiomeRegistryName != null
                && snapshotBiomeRegistryName.equalsIgnoreCase(requirement.getBiome().getRegistryName())
                ? CraftCheck.success()
                : CraftCheck.failure("error.modularmachineryaddons.requirement.missing.biome");
    }

    public void bindBiome(String biomeRegistryName) {
        boundBiomeRegistryName = biomeRegistryName;
        updateSnapshot();
        markForUpdate();
    }

    public String getBoundBiomeRegistryName() {
        return boundBiomeRegistryName;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        if (boundBiomeRegistryName != null) {
            compound.setString(TAG_BOUND_BIOME, boundBiomeRegistryName);
        } else {
            compound.removeTag(TAG_BOUND_BIOME);
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        boundBiomeRegistryName = compound.hasKey(TAG_BOUND_BIOME)
                ? compound.getString(TAG_BOUND_BIOME) : null;
    }
}
