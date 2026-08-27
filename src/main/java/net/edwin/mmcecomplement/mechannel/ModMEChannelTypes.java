package net.edwin.mmcecomplement.mechannel;

import net.edwin.mmcecomplement.Tags;
import net.minecraft.util.ResourceLocation;

/** Static registry entries for the ME channel component and requirement. */
public final class ModMEChannelTypes {
    public static final ComponentTypeMEChannel COMPONENT;
    public static final RequirementTypeMEChannel REQUIREMENT;

    static {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, "me_channel");
        COMPONENT = new ComponentTypeMEChannel();
        COMPONENT.setRegistryName(id);
        REQUIREMENT = new RequirementTypeMEChannel();
        REQUIREMENT.setRegistryName(id);
    }

    private ModMEChannelTypes() { }
}
