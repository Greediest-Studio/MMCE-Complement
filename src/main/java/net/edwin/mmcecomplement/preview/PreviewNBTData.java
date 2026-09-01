package net.edwin.mmcecomplement.preview;

import net.minecraft.nbt.NBTTagCompound;

/** Stores display-only NBT metadata for requirements without native support. */
public interface PreviewNBTData {
    void mmceComplement$setPreviewNBT(NBTTagCompound tag);

    NBTTagCompound mmceComplement$getPreviewNBT();
}
