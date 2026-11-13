package net.edwin.mmcecomplement.redstone;

/** Matches MMCE's energy-hatch comparator scaling without linking RedstoneHelper. */
public final class EnergyHatchRedstoneLevel {
    private EnergyHatchRedstoneLevel() { }

    public static int fromEnergy(long currentEnergy, long maxEnergy) {
        float ratio = (float) ((double) currentEnergy / (double) maxEnergy);
        int level = Math.round(15.0F * ratio);
        return Math.max(0, Math.min(15, level));
    }
}
