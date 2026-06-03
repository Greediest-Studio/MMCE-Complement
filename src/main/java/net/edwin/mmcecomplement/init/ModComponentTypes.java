package net.edwin.mmcecomplement.init;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import net.edwin.mmcecomplement.Tags;
import net.minecraft.util.ResourceLocation;

public final class ModComponentTypes {

    public static final ResourceLocation KEY_COMPONENT_MACHINE_CONTROL_INTERFACE =
            new ResourceLocation(Tags.MOD_ID, "machine_control_interface");

    public static ComponentType MACHINE_CONTROL_INTERFACE;

    private ModComponentTypes() {}
}