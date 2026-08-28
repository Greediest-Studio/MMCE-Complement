package net.edwin.mmcecomplement.mechannel;

import net.edwin.mmcecomplement.Tags;
import net.minecraft.util.ResourceLocation;

/** Static registry entries for the ME channel component and requirement. */
public final class ModMEChannelTypes {
    public static final ComponentTypeMEChannel COMPONENT;
    public static final ComponentTypeMEConnectionShare SHARE_COMPONENT;
    public static final RequirementTypeMEChannel REQUIREMENT;

    static {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, "me_channel");
        COMPONENT = new ComponentTypeMEChannel();
        COMPONENT.setRegistryName(id);
        SHARE_COMPONENT = new ComponentTypeMEConnectionShare();
        SHARE_COMPONENT.setRegistryName(new ResourceLocation(Tags.MOD_ID,
            "me_connection_share"));
        REQUIREMENT = new RequirementTypeMEChannel();
        REQUIREMENT.setRegistryName(id);
    }

    private ModMEChannelTypes() { }
}
