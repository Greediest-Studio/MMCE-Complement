package net.edwin.mmcecomplement;

import net.edwin.mmcecomplement.gui.GuiHandlerMMCE;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Tags.MOD_ID,
    name = Tags.MOD_NAME,
    version = Tags.VERSION,
    dependencies = "required-after:fluxnetworks;required-after:modularmachinery;after:appliedenergistics2;after:crazyae;"
)
public class MMCEComplement {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    /** GUI id for the Wireless Flux Input Hatch. */
    public static final int GUI_FLUX_INPUT_HATCH = 1;
    /** GUI id for the Wireless Flux Output Hatch. */
    public static final int GUI_FLUX_OUTPUT_HATCH = 2;
    /** GUI id for the ME Mechanical Energy Input Bus. */
    public static final int GUI_ME_ENERGY_INPUT_BUS = 3;
    /** GUI id for the ME Mechanical Energy Output Bus. */
    public static final int GUI_ME_ENERGY_OUTPUT_BUS = 4;
    /** GUI id for the ME Mechanical Mana Input Bus. */
    public static final int GUI_ME_MANA_INPUT_BUS = 5;
    /** GUI id for the ME Mechanical Mana Output Bus. */
    public static final int GUI_ME_MANA_OUTPUT_BUS = 6;

    @Mod.Instance(Tags.MOD_ID)
    public static MMCEComplement instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandlerMMCE());
        NetworkHandlerMMCE.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Reserved for future integration hooks.
    }
}