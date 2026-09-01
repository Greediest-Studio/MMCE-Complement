package net.edwin.mmcecomplement.catalyst;
import net.minecraft.util.ResourceLocation;
public final class CatalystTypes {
    public static final RequirementTypeFluidCatalyst FLUID = new RequirementTypeFluidCatalyst();
    public static RequirementTypeGasCatalyst GAS;
    private CatalystTypes() { }
    public static void register(net.minecraftforge.registries.IForgeRegistry registry) {
        FLUID.setRegistryName(new ResourceLocation("mmce_complement", "fluid_catalyst")); registry.register(FLUID);
        if (net.edwin.mmcecomplement.compat.CompatMods.isMekanismCompatLoaded()) {
            GAS = new RequirementTypeGasCatalyst();
            GAS.setRegistryName(new ResourceLocation("mmce_complement", "gas_catalyst")); registry.register(GAS);
        }
    }
}
