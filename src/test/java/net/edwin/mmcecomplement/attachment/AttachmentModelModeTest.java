package net.edwin.mmcecomplement.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttachmentModelModeTest {

    @Test
    public void acceptsTheThreeConfigurationModes() {
        assertEquals(AttachmentModelMode.DEFAULT,
            AttachmentModelMode.parse("none"));
        assertEquals(AttachmentModelMode.HIDE,
            AttachmentModelMode.parse("hide"));
        assertEquals(AttachmentModelMode.SEPARATE,
            AttachmentModelMode.parse("separate"));
    }
}
