package net.edwin.mmcecomplement.config;

import net.edwin.mmcecomplement.Tags;
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

    private ModConfig() {}

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
