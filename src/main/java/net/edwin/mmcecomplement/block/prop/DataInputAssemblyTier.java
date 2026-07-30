package net.edwin.mmcecomplement.block.prop;

import net.minecraft.util.IStringSerializable;

/** Fixed capacities for the five data input assembly tiers. */
public enum DataInputAssemblyTier implements IStringSerializable {

    SMALL("small", 1, 2, 1, 1_000, 2, 204),
    NORMAL("normal", 0, 6, 2, 8_000, 3, 204),
    BIG("big", 2, 12, 4, 64_000, 4, 204),
    HUGE("huge", 3, 20, 6, 512_000, 5, 220),
    LUDICROUS("ludicrous", 4, 30, 9, 4_096_000, 5, 252);

    private final String name;
    private final int metadata;
    private final int itemSlots;
    private final int fluidTanks;
    private final int perTankCapacity;
    private final int itemColumns;
    private final int guiHeight;

    DataInputAssemblyTier(String name, int metadata, int itemSlots,
                          int fluidTanks,
                          int perTankCapacity, int itemColumns,
                          int guiHeight) {
        this.name = name;
        this.metadata = metadata;
        this.itemSlots = itemSlots;
        this.fluidTanks = fluidTanks;
        this.perTankCapacity = perTankCapacity;
        this.itemColumns = itemColumns;
        this.guiHeight = guiHeight;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getItemSlots() {
        return itemSlots;
    }

    public int getMetadata() {
        return metadata;
    }

    public int getFluidTanks() {
        return fluidTanks;
    }

    public int getPerTankCapacity() {
        return perTankCapacity;
    }

    public int getItemColumns() {
        return itemColumns;
    }

    public int getItemRows() {
        return (itemSlots + itemColumns - 1) / itemColumns;
    }

    public int getGuiHeight() {
        return guiHeight;
    }

    public static DataInputAssemblyTier fromMeta(int meta) {
        for (DataInputAssemblyTier tier : values()) {
            if (tier.metadata == meta) {
                return tier;
            }
        }
        return NORMAL;
    }
}
