package net.edwin.mmcecomplement.gas;

import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GasOutputModifiersTest {
    private static final Gas HYDROGEN = new Gas("test_hydrogen", 0xFFFFFF);
    private static final Gas OXYGEN = new Gas("test_oxygen", 0x55AAFF);

    @Test
    void changesGasTypeAndAmountWithoutMutatingTemplate() {
        GasStack template = new GasStack(HYDROGEN, 1000);

        GasStack result = GasOutputModifiers.apply(
            Collections.singletonList((controller, stack) ->
                new GasStack(OXYGEN, stack.amount * 2)), null, template);

        assertSame(OXYGEN, result.getGas());
        assertEquals(2000, result.amount);
        assertSame(HYDROGEN, template.getGas());
        assertEquals(1000, template.amount);
    }

    @Test
    void appliesModifiersInDeclarationOrder() {
        GasStack result = GasOutputModifiers.apply(Arrays.asList(
            (controller, stack) -> {
                stack.amount += 250;
                return stack;
            },
            (controller, stack) -> new GasStack(OXYGEN, stack.amount * 2)
        ), null, new GasStack(HYDROGEN, 1000));

        assertSame(OXYGEN, result.getGas());
        assertEquals(2500, result.amount);
    }

    @Test
    void nullResultSuppressesTheOutputAndStopsTheChain() {
        GasStack result = GasOutputModifiers.apply(Arrays.asList(
            (controller, stack) -> null,
            (controller, stack) -> {
                throw new AssertionError("modifier chain should have stopped");
            }
        ), null, new GasStack(HYDROGEN, 1000));

        assertNull(result);
    }
}
