package net.edwin.mmcecomplement.redstoneinterface;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedstoneInterfaceRegistryTest {

    @AfterEach
    void clearRegistry() {
        RedstoneInterfaceRegistry.clear();
    }

    @Test
    void preservesDefinitionOrderAndRejectsDuplicateNames() {
        ResourceLocation machine = new ResourceLocation(
            "modularmachinery", "test_machine");
        assertTrue(RedstoneInterfaceRegistry.register(machine,
            new RedstoneValueDefinition("temperature", 0)));
        assertTrue(RedstoneInterfaceRegistry.register(machine,
            new RedstoneValueDefinition("pressure", 2)));
        assertFalse(RedstoneInterfaceRegistry.register(machine,
            new RedstoneValueDefinition("temperature", 1)));

        assertEquals(java.util.Arrays.asList("temperature", "pressure"),
            RedstoneInterfaceRegistry.getNames(machine));
        assertEquals(0, RedstoneInterfaceRegistry
            .get(machine, "temperature").getOperator());
    }

    @Test
    void simpleMachineIdsUseTheMmceNamespace() {
        assertEquals(new ResourceLocation("modularmachinery", "test_machine"),
            RedstoneInterfaceRegistry.normalizeMachineId("test_machine"));
        assertEquals(new ResourceLocation("example", "test_machine"),
            RedstoneInterfaceRegistry.normalizeMachineId(
                "example:test_machine"));
    }
}
