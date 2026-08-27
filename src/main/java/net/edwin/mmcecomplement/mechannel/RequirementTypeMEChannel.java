package net.edwin.mmcecomplement.mechannel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import hellfirepvp.modularmachinery.common.crafting.requirement.type.RequirementType;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.jei.mechannel.MEChannelIngredient;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Parses the {@code mmce_complement:me_channel} recipe input. */
public class RequirementTypeMEChannel
    extends RequirementType<MEChannelIngredient, RequirementMEChannel> {

    @Override
    public RequirementMEChannel createRequirement(IOType type,
                                                  JsonObject json) {
        if (type != IOType.INPUT) {
            throw new JsonParseException(
                "The ME channel requirement only supports io-type 'input'.");
        }
        if (!json.has("amount") || !json.get("amount").isJsonPrimitive()) {
            throw new JsonParseException(
                "The ME channel requirement expects a positive integer 'amount'.");
        }
        JsonPrimitive primitive = json.getAsJsonPrimitive("amount");
        if (!primitive.isNumber()) {
            throw new JsonParseException(
                "The ME channel requirement 'amount' must be a positive integer.");
        }

        final BigInteger exact;
        try {
            exact = new BigDecimal(primitive.getAsString()).toBigIntegerExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new JsonParseException(
                "The ME channel requirement 'amount' must be a positive int.", ex);
        }
        if (exact.signum() <= 0
            || exact.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new JsonParseException(
                "The ME channel requirement 'amount' must be between 1 and "
                    + Integer.MAX_VALUE + '.');
        }
        return new RequirementMEChannel(exact.intValue());
    }

    @Nullable
    @Override
    public String requiresModid() {
        return CompatMods.MODID_AE2;
    }
}
