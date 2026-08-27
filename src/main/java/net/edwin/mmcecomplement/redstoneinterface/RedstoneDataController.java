package net.edwin.mmcecomplement.redstoneinterface;

/** Internal bridge implemented by MMCE machine controllers through a mixin. */
public interface RedstoneDataController {

    int mmceComplement$getRedstone(String name);

    void mmceComplement$setRedstone(String name, int value);
}
