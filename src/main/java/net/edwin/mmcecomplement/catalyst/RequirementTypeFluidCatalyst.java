package net.edwin.mmcecomplement.catalyst;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public class RequirementTypeFluidCatalyst extends RequirementType<Object, RequirementFluidCatalyst> {
    @Override public RequirementFluidCatalyst createRequirement(IOType io, JsonObject json) {
        if (io != IOType.INPUT || !json.has("fluid") || !json.has("amount")) throw new JsonParseException("fluid_catalyst requires fluid and amount");
        String name = json.get("fluid").getAsString(); int amount = json.get("amount").getAsInt();
        if (amount <= 0 || FluidRegistry.getFluid(name) == null) throw new JsonParseException("Unknown fluid catalyst: " + name);
        RequirementFluidCatalyst result = new RequirementFluidCatalyst(new FluidStack(FluidRegistry.getFluid(name), amount));
        if (json.has("chance")) result.setChance(json.get("chance").getAsFloat());
        return result;
    }
}
