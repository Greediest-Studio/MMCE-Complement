package net.edwin.mmcecomplement.compat;

import net.minecraftforge.fml.common.Loader;

public final class CompatMods {

    public static final String MODID_AE2 = "appliedenergistics2";
    public static final String MODID_CRAZY_AE = "crazyae";

    private static Boolean aeEnergyCompatLoaded;
    private static Boolean aeManaCompatLoaded;
    private static Boolean guguManaCompatLoaded;

    private CompatMods() {}

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