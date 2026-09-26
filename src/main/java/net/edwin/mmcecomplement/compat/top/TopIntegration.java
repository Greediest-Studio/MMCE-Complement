package net.edwin.mmcecomplement.compat.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.minecraftforge.fml.common.event.FMLInterModComms;

import java.util.function.Function;

/** Optional TOPCE integration entry point. */
public final class TopIntegration {

    private TopIntegration() {
    }

    public static void register() {
        if (CompatMods.isTopLoaded() && CompatMods.isMMCEAddonsLoaded()) {
            FMLInterModComms.sendFunctionMessage(
                    CompatMods.MODID_TOP,
                    "getTheOneProbe",
                    Register.class.getName());
        }
    }

    public static class Register implements Function<ITheOneProbe, Void> {
        @Override
        public Void apply(ITheOneProbe probe) {
            probe.registerProvider(new ConfigurableDetectorProbeProvider());
            return null;
        }
    }
}
