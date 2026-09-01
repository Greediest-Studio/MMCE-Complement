package net.edwin.mmcecomplement.preview;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class PreviewNBTDataTest {
    @Test
    void previewNBTIsCopiedAtTheBoundary() {
        PreviewNBTData data = new PreviewNBTData() {
            private NBTTagCompound value;
            @Override public void mmceComplement$setPreviewNBT(NBTTagCompound tag) {
                value = tag == null ? null : tag.copy();
            }
            @Override public NBTTagCompound mmceComplement$getPreviewNBT() {
                return value == null ? null : value.copy();
            }
        };
        NBTTagCompound source = new NBTTagCompound();
        source.setString("mode", "preview");
        data.mmceComplement$setPreviewNBT(source);
        NBTTagCompound result = data.mmceComplement$getPreviewNBT();
        assertEquals(source, result);
        assertNotSame(source, result);
        data.mmceComplement$setPreviewNBT(null);
        assertNull(data.mmceComplement$getPreviewNBT());
    }
}
