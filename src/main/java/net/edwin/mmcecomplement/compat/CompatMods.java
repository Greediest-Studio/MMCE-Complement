package net.edwin.mmcecomplement.compat;

import net.minecraftforge.fml.common.Loader;

public final class CompatMods {

    public static final String MODID_FLUX_NETWORKS = "fluxnetworks";
    public static final String MODID_AE2 = "appliedenergistics2";
    public static final String MODID_AE2S = "ae2";
    public static final String MODID_CRAZY_AE = "crazyae";
    public static final String MODID_MEKANISM = "mekanism";
    public static final String MODID_MEKENG = "mekeng";
    public static final String MODID_AE2FCR = "ae2fc";

    private static Boolean fluxCompatLoaded;
    private static Boolean mekanismCompatLoaded;
    private static Boolean aeEnergyCompatLoaded;
    private static Boolean aeItemCompatLoaded;
    private static Boolean aeGasCompatLoaded;
    private static Boolean aeManaCompatLoaded;
    private static Boolean ae2FcrCompatLoaded;
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

    public static boolean isMekanismCompatLoaded() {
        if (mekanismCompatLoaded == null) {
            mekanismCompatLoaded = Loader.isModLoaded(MODID_MEKANISM)
                && classExists("mekanism.api.gas.IGasHandler")
                && classExists("mekanism.common.capabilities.Capabilities");
        }
        return mekanismCompatLoaded;
    }

    public static boolean isAeEnergyCompatLoaded() {
        if (aeEnergyCompatLoaded == null) {
            aeEnergyCompatLoaded = isAe2Loaded()
                    && Loader.isModLoaded(MODID_CRAZY_AE)
                    && classExists("dev.beecube31.crazyae2.core.CrazyAE")
                    && classExists("dev.beecube31.crazyae2.core.api.storage.energy.IEnergyStorageChannel");
        }
        return aeEnergyCompatLoaded;
    }

    /** MMCE's ordinary ME item buses only require AE2, not CrazyAE. */
    public static boolean isAeItemCompatLoaded() {
        if (aeItemCompatLoaded == null) {
            aeItemCompatLoaded = isAe2Loaded()
                && classExists("github.kasuminova.mmce.common.tile.MEItemInputBus");
        }
        return aeItemCompatLoaded;
    }

    /** The gas bus additionally requires Mekanism Energistics' storage channel. */
    public static boolean isAeGasCompatLoaded() {
        if (aeGasCompatLoaded == null) {
            aeGasCompatLoaded = isAeItemCompatLoaded()
                && isMekanismCompatLoaded()
                && Loader.isModLoaded(MODID_MEKENG)
                && classExists("com.mekeng.github.common.me.storage.IGasStorageChannel")
                && classExists("github.kasuminova.mmce.common.tile.MEGasInputBus");
        }
        return aeGasCompatLoaded;
    }

    public static boolean isAeManaCompatLoaded() {
        if (aeManaCompatLoaded == null) {
            aeManaCompatLoaded = isAe2Loaded()
                    && Loader.isModLoaded(MODID_CRAZY_AE)
                    && classExists("dev.beecube31.crazyae2.core.CrazyAE")
                    && classExists("dev.beecube31.crazyae2.core.api.storage.IManaStorageChannel")
                    && classExists("kport.modularmagic.common.tile.TileManaProvider")
                    && classExists("kport.modularmagic.common.tile.machinecomponent.MachineComponentManaProvider");
        }
        return aeManaCompatLoaded;
    }

    public static boolean isAe2FcrCompatLoaded() {
        if (ae2FcrCompatLoaded == null) {
            ae2FcrCompatLoaded = Loader.isModLoaded(MODID_AE2FCR)
                && classExists("com.glodblock.github.common.item.fake.FakeFluids")
                && classExists("com.glodblock.github.common.item.fake.FakeItemRegister");
        }
        return ae2FcrCompatLoaded;
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

    /** Returns true when either AE2UEL or AE2S is present with its core API. */
    public static boolean isAe2Loaded() {
        return (Loader.isModLoaded(MODID_AE2) && classExists("appeng.core.AE2ELCore"))
                || (Loader.isModLoaded(MODID_AE2S) && classExists("ae2.core.AppEngBase"));
    }

    /** Returns the loaded branch's mod ID for MMCE requirement metadata. */
    public static String getLoadedAe2ModId() {
        if (Loader.isModLoaded(MODID_AE2)) {
            return MODID_AE2;
        }
        if (Loader.isModLoaded(MODID_AE2S)) {
            return MODID_AE2S;
        }
        return MODID_AE2;
    }
}
