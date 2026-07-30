package net.edwin.mmcecomplement.config;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.common.config.Config;

/** Forge configuration for MMCE Complement. */
@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID)
public final class ModConfig {

    @Config.Name("thread_hatch")
    @Config.Comment("Thread Hatch settings.")
    public static ThreadHatchConfig threadHatch = new ThreadHatchConfig();

    @Config.Name("overclock_hatch")
    @Config.Comment("Overclock Hatch settings.")
    public static OverclockHatchConfig overclockHatch = new OverclockHatchConfig();

    @Config.Name("data_input_assembly")
    @Config.Comment("Fluid capacity settings for Data Input Assemblies.")
    public static DataInputAssemblyConfig dataInputAssembly =
        new DataInputAssemblyConfig();

    @Config.Name("input_assembly")
    @Config.Comment("Fluid capacity settings for Input Assemblies.")
    public static InputAssemblyConfig inputAssembly =
        new InputAssemblyConfig();

    @Config.Name("liquid_energizer_hatch")
    @Config.Comment("Liquid Energizer Hatch settings.")
    public static LiquidEnergizerHatchConfig liquidEnergizerHatch =
        new LiquidEnergizerHatchConfig();

    private ModConfig() {}

    /** Returns the configured per-tank capacity for a data input assembly. */
    public static int getDataInputAssemblyCapacity(DataInputAssemblyTier tier) {
        return dataInputAssembly.getCapacity(tier);
    }

    /** Returns the configured per-tank capacity for a normal input assembly. */
    public static int getInputAssemblyCapacity(DataInputAssemblyTier tier) {
        return inputAssembly.getCapacity(tier);
    }

    public static long getLiquidEnergizerFluidCapacity(FluidHatchSize tier) {
        return liquidEnergizerHatch.getTier(tier).getFluidCapacity();
    }

    public static long getLiquidEnergizerEnergyCapacity(FluidHatchSize tier) {
        return liquidEnergizerHatch.getTier(tier).getEnergyCapacity();
    }

    public static long getLiquidEnergizerRatio(Fluid fluid) {
        return liquidEnergizerHatch.getEnergyPerMb(fluid);
    }

    public static final class DataInputAssemblyConfig {
        @Config.Name("small_capacity")
        @Config.Comment("Per-tank fluid capacity of the Small Data Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int smallCapacity = 1000;

        @Config.Name("normal_capacity")
        @Config.Comment("Per-tank fluid capacity of the Normal Data Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int normalCapacity = 8000;

        @Config.Name("big_capacity")
        @Config.Comment("Per-tank fluid capacity of the Big Data Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int bigCapacity = 64000;

        @Config.Name("huge_capacity")
        @Config.Comment("Per-tank fluid capacity of the Huge Data Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int hugeCapacity = 512000;

        @Config.Name("ludicrous_capacity")
        @Config.Comment("Per-tank fluid capacity of the Super Data Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int ludicrousCapacity = 4096000;

        private int getCapacity(DataInputAssemblyTier tier) {
            switch (tier) {
                case SMALL: return smallCapacity;
                case BIG: return bigCapacity;
                case HUGE: return hugeCapacity;
                case LUDICROUS: return ludicrousCapacity;
                case NORMAL:
                default: return normalCapacity;
            }
        }
    }

    public static final class InputAssemblyConfig {
        @Config.Name("small_capacity")
        @Config.Comment("Per-tank fluid capacity of the Small Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int smallCapacity = 1000;

        @Config.Name("normal_capacity")
        @Config.Comment("Per-tank fluid capacity of the Normal Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int normalCapacity = 8000;

        @Config.Name("big_capacity")
        @Config.Comment("Per-tank fluid capacity of the Big Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int bigCapacity = 64000;

        @Config.Name("huge_capacity")
        @Config.Comment("Per-tank fluid capacity of the Huge Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int hugeCapacity = 512000;

        @Config.Name("ludicrous_capacity")
        @Config.Comment("Per-tank fluid capacity of the Super Input Assembly (mB).")
        @Config.RangeInt(min = 1, max = 2000000000)
        public int ludicrousCapacity = 4096000;

        private int getCapacity(DataInputAssemblyTier tier) {
            switch (tier) {
                case SMALL: return smallCapacity;
                case BIG: return bigCapacity;
                case HUGE: return hugeCapacity;
                case LUDICROUS: return ludicrousCapacity;
                case NORMAL:
                default: return normalCapacity;
            }
        }
    }

    public static final class LiquidEnergizerHatchConfig {

        @Config.Name("fluid_energy_ratios")
        @Config.Comment({
            "Accepted fluid conversion entries in the form fluid_registry_name=energy_per_mB.",
            "Examples: pack:liquid_energy=1000000 or lava=2000.",
            "Only positive integer ratios up to 9223372036854775807 are accepted.",
            "The default list is empty so no ordinary fluid becomes free energy unexpectedly."
        })
        public String[] fluidEnergyRatios = new String[0];

        public TierCapacity tiny = new TierCapacity("100");
        public TierCapacity small = new TierCapacity("400");
        public TierCapacity normal = new TierCapacity("1000");
        public TierCapacity reinforced = new TierCapacity("2000");
        public TierCapacity big = new TierCapacity("4500");
        public TierCapacity huge = new TierCapacity("8000");
        public TierCapacity ludicrous = new TierCapacity("16000");
        public TierCapacity vacuum = new TierCapacity("32000");

        private TierCapacity getTier(FluidHatchSize tier) {
            if (tier == null) return tiny;
            // Do not use an enum switch here. javac merges all enum switch
            // tables in this outer config class into one synthetic class;
            // that made otherwise unrelated config tests eagerly initialize
            // MMCE's FluidHatchSize and its optional Mekanism API references.
            if (tier == FluidHatchSize.SMALL) return small;
            if (tier == FluidHatchSize.NORMAL) return normal;
            if (tier == FluidHatchSize.REINFORCED) return reinforced;
            if (tier == FluidHatchSize.BIG) return big;
            if (tier == FluidHatchSize.HUGE) return huge;
            if (tier == FluidHatchSize.LUDICROUS) return ludicrous;
            if (tier == FluidHatchSize.VACUUM) return vacuum;
            return tiny;
        }

        private long getEnergyPerMb(Fluid fluid) {
            if (fluid == null || fluidEnergyRatios == null) return 0L;
            String fluidName = fluid.getName();
            long result = 0L;
            for (String entry : fluidEnergyRatios) {
                if (entry == null) continue;
                int separator = entry.lastIndexOf('=');
                if (separator <= 0 || separator == entry.length() - 1) continue;
                if (!fluidName.equals(entry.substring(0, separator).trim())) continue;
                result = parsePositiveLong(entry.substring(separator + 1), 0L);
            }
            return result;
        }
    }

    public static final class TierCapacity {

        @Config.Name("fluid_capacity")
        @Config.Comment({
            "Long-backed fluid capacity in mB. Stored as text because Forge 1.12 configs",
            "cannot represent exact long integers. Range: 1 to 9223372036854775807."
        })
        public String fluidCapacity = "100";

        @Config.Name("energy_capacity")
        @Config.Comment({
            "Long-backed internal energy capacity. Stored as text because Forge 1.12 configs",
            "cannot represent exact long integers. Range: 1 to 9223372036854775807."
        })
        public String energyCapacity = "100";

        public TierCapacity() { }

        private TierCapacity(String defaultCapacity) {
            this.fluidCapacity = defaultCapacity;
            this.energyCapacity = defaultCapacity;
        }

        private long getFluidCapacity() {
            return parsePositiveLong(fluidCapacity, 1L);
        }

        private long getEnergyCapacity() {
            return parsePositiveLong(energyCapacity, 1L);
        }
    }

    private static long parsePositiveLong(String value, long fallback) {
        if (value == null) return fallback;
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0L ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static final class ThreadHatchConfig {

        @Config.Name("mk1_multiplier")
        @Config.Comment("Base normal-thread multiplier supplied by a MK I Thread Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk1Multiplier = 2.0D;

        @Config.Name("mk2_multiplier")
        @Config.Comment("Base normal-thread multiplier supplied by a MK II Thread Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk2Multiplier = 3.0D;

        @Config.Name("mk3_multiplier")
        @Config.Comment("Base normal-thread multiplier supplied by a MK III Thread Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk3Multiplier = 5.0D;

        @Config.Name("mk4_multiplier")
        @Config.Comment("Base normal-thread multiplier supplied by a MK IV Thread Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk4Multiplier = 8.0D;

        @Config.Name("mk5_multiplier")
        @Config.Comment("Base normal-thread multiplier supplied by a MK V Thread Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk5Multiplier = 12.0D;

        @Config.Name("mk6_multiplier")
        @Config.Comment("Base normal-thread multiplier supplied by a MK VI Thread Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk6Multiplier = 16.0D;

        @Config.Name("allow_stacking")
        @Config.Comment({
            "If true, every Thread Hatch in a formed machine multiplies the effects of the others.",
            "If false, only one hatch of the highest installed tier takes effect."
        })
        public boolean allowStacking = false;

        public double[] getMultipliers() {
            return new double[] {
                mk1Multiplier,
                mk2Multiplier,
                mk3Multiplier,
                mk4Multiplier,
                mk5Multiplier,
                mk6Multiplier
            };
        }
    }

    public static final class OverclockHatchConfig {

        @Config.Name("mk1_energy_multiplier")
        @Config.Comment("Energy-consumption multiplier supplied by a MK I Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk1EnergyMultiplier = 4.0D;

        @Config.Name("mk1_duration_multiplier")
        @Config.Comment("Recipe-duration multiplier supplied by a MK I Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk1DurationMultiplier = 0.5D;

        @Config.Name("mk2_energy_multiplier")
        @Config.Comment("Energy-consumption multiplier supplied by a MK II Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk2EnergyMultiplier = 16.0D;

        @Config.Name("mk2_duration_multiplier")
        @Config.Comment("Recipe-duration multiplier supplied by a MK II Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk2DurationMultiplier = 0.25D;

        @Config.Name("mk3_energy_multiplier")
        @Config.Comment("Energy-consumption multiplier supplied by a MK III Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk3EnergyMultiplier = 64.0D;

        @Config.Name("mk3_duration_multiplier")
        @Config.Comment("Recipe-duration multiplier supplied by a MK III Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk3DurationMultiplier = 0.125D;

        @Config.Name("mk4_energy_multiplier")
        @Config.Comment("Energy-consumption multiplier supplied by a MK IV Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk4EnergyMultiplier = 256.0D;

        @Config.Name("mk4_duration_multiplier")
        @Config.Comment("Recipe-duration multiplier supplied by a MK IV Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk4DurationMultiplier = 0.0625D;

        @Config.Name("mk5_energy_multiplier")
        @Config.Comment("Energy-consumption multiplier supplied by a MK V Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk5EnergyMultiplier = 1024.0D;

        @Config.Name("mk5_duration_multiplier")
        @Config.Comment("Recipe-duration multiplier supplied by a MK V Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk5DurationMultiplier = 0.03125D;

        @Config.Name("mk6_energy_multiplier")
        @Config.Comment("Energy-consumption multiplier supplied by a MK VI Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk6EnergyMultiplier = 4096.0D;

        @Config.Name("mk6_duration_multiplier")
        @Config.Comment("Recipe-duration multiplier supplied by a MK VI Overclock Hatch.")
        @Config.RangeDouble(min = 0.0D, max = 1000000.0D)
        public double mk6DurationMultiplier = 0.015625D;

        @Config.Name("allow_stacking")
        @Config.Comment({
            "If true, every Overclock Hatch in a formed machine multiplies the effects of the others.",
            "If false, only one hatch of the highest installed tier takes effect."
        })
        public boolean allowStacking = false;

        public double[] getEnergyMultipliers() {
            return new double[] {
                mk1EnergyMultiplier,
                mk2EnergyMultiplier,
                mk3EnergyMultiplier,
                mk4EnergyMultiplier,
                mk5EnergyMultiplier,
                mk6EnergyMultiplier
            };
        }

        public double[] getDurationMultipliers() {
            return new double[] {
                mk1DurationMultiplier,
                mk2DurationMultiplier,
                mk3DurationMultiplier,
                mk4DurationMultiplier,
                mk5DurationMultiplier,
                mk6DurationMultiplier
            };
        }
    }
}
