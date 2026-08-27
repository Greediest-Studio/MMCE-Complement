package net.edwin.mmcecomplement.compat;

import net.minecraftforge.fml.common.Loader;

public final class CompatMods {

    public static final String MODID_FLUX_NETWORKS = "fluxnetworks";
    public static final String MODID_AE2 = "appliedenergistics2";
    public static final String MODID_CRAZY_AE = "crazyae";
    public static final String MODID_MEKANISM = "mekanism";
    public static final String MODID_MEKENG = "mekeng";

    private static Boolean fluxCompatLoaded;
    private static Boolean aeEnergyCompatLoaded;
    private static Boolean aeItemCompatLoaded;
    private static Boolean aeGasCompatLoaded;
    private static Boolean aeManaCompatLoaded;
    private static Boolean guguManaCompatLoaded;

    private CompatMods() {}

    public static boolean isFluxCompatLoaded() {
        if (fluxCompatLoaded == null) {
            fluxCompatLoaded = Loader.isModLoaded(MODID_FLUX_NETWORKS)
                    && classExists("sonar.fluxnetworks.api.tiles.IFluxConnector")
                    && classExists("sonar.fluxnetworks.api.tiles.IFluxPoint")
                    && classExists("sonar.fluxnetworks.api.tiles.IFluxPlug")
                    && classExists("sonar.fluxnetworks.common.connection.FluxNetworkCache");
        }
        return fluxCompatLoaded;
    }

    public static boolean isAeEnergyCompatLoaded() {
        if (aeEnergyCompatLoaded == null) {
            aeEnergyCompatLoaded = Loader.isModLoaded(MODID_AE2)
                    && Loader.isModLoaded(MODID_CRAZY_AE)
                    && classExists("appeng.core.AE2ELCore")
                    && classExists("dev.beecube31.crazyae2.core.CrazyAE")
                    && classExists("dev.beecube31.crazyae2.core.api.storage.energy.IEnergyStorageChannel");
        }
        return aeEnergyCompatLoaded;
    }

    /** MMCE's ordinary ME item buses only require AE2, not CrazyAE. */
    public static boolean isAeItemCompatLoaded() {
        if (aeItemCompatLoaded == null) {
            aeItemCompatLoaded = Loader.isModLoaded(MODID_AE2)
                && classExists("appeng.core.AE2ELCore")
                && classExists("github.kasuminova.mmce.common.tile.MEItemInputBus");
        }
        return aeItemCompatLoaded;
    }

    /** The gas bus additionally requires Mekanism Energistics' storage channel. */
    public static boolean isAeGasCompatLoaded() {
        if (aeGasCompatLoaded == null) {
            aeGasCompatLoaded = isAeItemCompatLoaded()
                && Loader.isModLoaded(MODID_MEKANISM)
                && Loader.isModLoaded(MODID_MEKENG)
                && classExists("com.mekeng.github.common.me.storage.IGasStorageChannel")
                && classExists("github.kasuminova.mmce.common.tile.MEGasInputBus");
        }
        return aeGasCompatLoaded;
    }

    public static boolean isAeManaCompatLoaded() {
        if (aeManaCompatLoaded == null) {
            aeManaCompatLoaded = Loader.isModLoaded(MODID_AE2)
                    && Loader.isModLoaded(MODID_CRAZY_AE)
                    && classExists("appeng.core.AE2ELCore")
                    && classExists("dev.beecube31.crazyae2.core.CrazyAE")
                    && classExists("dev.beecube31.crazyae2.core.api.storage.IManaStorageChannel")
                    && classExists("kport.modularmagic.common.tile.TileManaProvider")
                    && classExists("kport.modularmagic.common.tile.machinecomponent.MachineComponentManaProvider");
        }
        return aeManaCompatLoaded;
    }

    public static boolean isGuguManaCompatLoaded() {
        if (guguManaCompatLoaded == null) {
            guguManaCompatLoaded = classExists("com.warmthdawn.mod.gugu_utils.modularmachenary.MMCompoments")
                    && classExists("com.warmthdawn.mod.gugu_utils.modularmachenary.components.GenericMachineCompoment")
                    && classExists("com.warmthdawn.mod.gugu_utils.modularmachenary.requirements.RequirementMana$RT");
        }
        return guguManaCompatLoaded;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, CompatMods.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
