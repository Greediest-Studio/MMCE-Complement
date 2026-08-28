package net.edwin.mmcecomplement.compat.mekanism;

import net.edwin.mmcecomplement.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Loaded only after {@code CompatMods} verifies the Mekanism gas API. */
public final class MekanismTileRegistryCompat {

    private MekanismTileRegistryCompat() {}

    public static void registerTiles() {
        register(TileDataItemInputHatchMekanism.class,
            "data_input_assembly_hatch");
        register(TileItemInputAssemblyHatchMekanism.class,
            "input_assembly_hatch");
        register(TileItemOutputAssemblyHatchMekanism.class,
            "output_assembly_hatch");
        register(TileSelfCycleAssemblyHatchMekanism.class,
            "self_cycle_assembly_hatch");
        register(TileQuadFluidInputHatchMekanism.class,
            "quad_fluid_input_hatch_tiny");
        register(TileQuadFluidOutputHatchMekanism.class,
            "quad_fluid_output_hatch_tiny");
        register(TileNineFluidInputHatchMekanism.class,
            "nine_fluid_input_hatch_normal");
        register(TileNineFluidOutputHatchMekanism.class,
            "nine_fluid_output_hatch_normal");
    }

    private static void register(Class<? extends net.minecraft.tileentity.TileEntity> type,
                                 String name) {
        GameRegistry.registerTileEntity(type,
            new ResourceLocation(Tags.MOD_ID, name));
    }
}
