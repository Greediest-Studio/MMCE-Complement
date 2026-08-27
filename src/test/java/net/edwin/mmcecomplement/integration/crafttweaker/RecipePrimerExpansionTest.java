package net.edwin.mmcecomplement.integration.crafttweaker;

import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import net.edwin.mmcecomplement.mechannel.RequirementMEChannel;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipePrimerExpansionTest {

    @Test
    void addsMEChannelInputAndKeepsThePrimerChainable() {
        RecipePrimer primer = primer();

        RecipePrimer returned =
            RecipePrimerExpansion.addMEChannelInput(primer, 32);

        assertSame(primer, returned);
        assertEquals(1, primer.getComponents().size());
        assertTrue(primer.getComponents().get(0)
            instanceof RequirementMEChannel);
        assertEquals(32, ((RequirementMEChannel) primer.getComponents().get(0))
            .getAmount());
    }

    @Test
    void rejectsNonPositiveMEChannelInput() {
        RecipePrimer primer = primer();

        assertThrows(IllegalArgumentException.class,
            () -> RecipePrimerExpansion.addMEChannelInput(primer, 0));
        assertThrows(IllegalArgumentException.class,
            () -> RecipePrimerExpansion.addMEChannelInput(primer, -1));
        assertTrue(primer.getComponents().isEmpty());
    }

    private static RecipePrimer primer() {
        return new RecipePrimer(
            new ResourceLocation("mmce_complement", "crt_test"),
            new ResourceLocation("mmce_complement", "test_machine"),
            20, 0, false);
    }
}
