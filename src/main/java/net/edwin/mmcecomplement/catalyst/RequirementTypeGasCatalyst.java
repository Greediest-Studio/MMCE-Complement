package net.edwin.mmcecomplement.catalyst;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraftforge.fml.common.Optional;

public class RequirementTypeGasCatalyst extends RequirementType<Object, RequirementGasCatalyst> {
    @Override public String requiresModid() { return "mekanism"; }
    @Override @Optional.Method(modid = "mekanism") public RequirementGasCatalyst createRequirement(IOType io, JsonObject json) {
        if (io != IOType.INPUT || !json.has("gas") || !json.has("amount")) throw new JsonParseException("gas_catalyst requires gas and amount");
        String name = json.get("gas").getAsString(); int amount = json.get("amount").getAsInt(); Gas gas = GasRegistry.getGas(name);
        if (gas == null || amount <= 0) throw new JsonParseException("Unknown gas catalyst: " + name);
        RequirementGasCatalyst result = new RequirementGasCatalyst(new GasStack(gas, amount));
        if (json.has("chance")) result.setChance(json.get("chance").getAsFloat());
        return result;
    }
}
