package net.edwin.mmcecomplement.mechannel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hellfirepvp.modularmachinery.common.machine.IOType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequirementTypeMEChannelTest {

    private final RequirementTypeMEChannel type =
        new RequirementTypeMEChannel();

    @Test
    void parsesPositiveIntAmount() {
        JsonObject json = new JsonObject();
        json.addProperty("amount", 16);
        assertEquals(16,
            type.createRequirement(IOType.INPUT, json).getAmount());
    }

    @Test
    void rejectsOutputFractionZeroAndOverflow() {
        JsonObject valid = new JsonObject();
        valid.addProperty("amount", 1);
        assertThrows(JsonParseException.class,
            () -> type.createRequirement(IOType.OUTPUT, valid));

        JsonObject fraction = new JsonObject();
        fraction.addProperty("amount", 1.5D);
        assertThrows(JsonParseException.class,
            () -> type.createRequirement(IOType.INPUT, fraction));

        JsonObject zero = new JsonObject();
        zero.addProperty("amount", 0);
        assertThrows(JsonParseException.class,
            () -> type.createRequirement(IOType.INPUT, zero));

        JsonObject overflow = new JsonObject();
        overflow.addProperty("amount", "2147483648");
        assertThrows(JsonParseException.class,
            () -> type.createRequirement(IOType.INPUT, overflow));
    }
}
