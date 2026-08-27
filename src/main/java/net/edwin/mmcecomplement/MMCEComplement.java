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
    dependencies = "required-after:modularmachinery;required-after:geckolib3;"
        + "after:fluxnetworks;after:appliedenergistics2;after:crazyae;"
        + "after:mekanism;after:mekeng;"
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
    /** GUI id for the Batch Hatch. */
    public static final int GUI_BATCH_HATCH = 7;
    /** GUI id for the Tiny Quadruple Fluid Input Hatch. */
    public static final int GUI_QUAD_FLUID_INPUT_HATCH = 8;
    /** GUI id for the Tiny Quadruple Fluid Output Hatch. */
    public static final int GUI_QUAD_FLUID_OUTPUT_HATCH = 9;
    /** GUI id for the Normal-and-above Ninefold Fluid Input Hatch. */
    public static final int GUI_NINE_FLUID_INPUT_HATCH = 10;
    /** GUI id for the Normal-and-above Ninefold Fluid Output Hatch. */
    public static final int GUI_NINE_FLUID_OUTPUT_HATCH = 11;
    /** GUI id for the combined smart-data/item input hatch. */
    public static final int GUI_DATA_INPUT_ASSEMBLY_HATCH = 12;
    /** GUI id for the item/fluid-only input assembly hatch. */
    public static final int GUI_INPUT_ASSEMBLY_HATCH = 13;
    /** GUI id for the item/fluid-only output assembly hatch. */
    public static final int GUI_OUTPUT_ASSEMBLY_HATCH = 14;
    /** GUI id for the self-cycle input/output assembly. */
    public static final int GUI_SELF_CYCLE_ASSEMBLY_HATCH = 15;
    /** GUI id for the eight-tier Liquid Energizer Hatch. */
    public static final int GUI_LIQUID_ENERGIZER_HATCH = 16;
    /** GUI id for the int-max ghost-filtered item output hatch. */
    public static final int GUI_FILTERED_ITEM_OUTPUT_HATCH = 17;
    /** GUI id for the int-max ghost-filtered fluid output hatch. */
    public static final int GUI_FILTERED_FLUID_OUTPUT_HATCH = 18;
    /** GUI id for the sixteen-channel item/fluid/gas ME input assembly. */
    public static final int GUI_ME_INPUT_ASSEMBLY = 19;
    /** GUI id for the mixed inventory input assembly. */
    public static final int GUI_ME_INVENTORY_INPUT_ASSEMBLY = 20;
    /** GUI id for the mixed ME output assembly. */
    public static final int GUI_ME_OUTPUT_ASSEMBLY = 21;
    /** GUI id for the unfiltered full-exposure assembly. */
    public static final int GUI_ME_FULL_EXPOSURE_ASSEMBLY = 22;
    /** GUI id for the named redstone signal input hatch. */
    public static final int GUI_REDSTONE_SIGNAL_INPUT_HATCH = 23;
    /** GUI id for the named redstone signal output hatch. */
    public static final int GUI_REDSTONE_SIGNAL_OUTPUT_HATCH = 24;
    /** GUI id for the expanded 144-slot ME pattern provider. */
    public static final int GUI_ME_PATTERN_PROVIDER_II = 25;
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
