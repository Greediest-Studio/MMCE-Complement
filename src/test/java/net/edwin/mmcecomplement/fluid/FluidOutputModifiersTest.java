package net.edwin.mmcecomplement.fluid;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FluidOutputModifiersTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    void changesFluidTypeAndAmountWithoutMutatingTemplate() {
        FluidStack template = new FluidStack(FluidRegistry.WATER, 1000);

        FluidStack result = FluidOutputModifiers.apply(
            Collections.singletonList((controller, stack) ->
                new FluidStack(FluidRegistry.LAVA, stack.amount * 2)),
            null, template);

        assertSame(FluidRegistry.LAVA, result.getFluid());
        assertEquals(2000, result.amount);
        assertSame(FluidRegistry.WATER, template.getFluid());
        assertEquals(1000, template.amount);
    }

    @Test
    void appliesModifiersInDeclarationOrder() {
        FluidStack result = FluidOutputModifiers.apply(Arrays.asList(
            (controller, stack) -> {
                stack.amount += 250;
                return stack;
            },
            (controller, stack) ->
                new FluidStack(FluidRegistry.LAVA, stack.amount * 2)
        ), null, new FluidStack(FluidRegistry.WATER, 1000));

        assertSame(FluidRegistry.LAVA, result.getFluid());
        assertEquals(2500, result.amount);
    }

    @Test
    void nullResultSuppressesTheOutputAndStopsTheChain() {
        FluidStack result = FluidOutputModifiers.apply(Arrays.asList(
            (controller, stack) -> null,
            (controller, stack) -> {
                throw new AssertionError("modifier chain should have stopped");
            }
        ), null, new FluidStack(FluidRegistry.WATER, 1000));

        assertNull(result);
    }
}
