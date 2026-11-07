package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.parts.*;
import appeng.api.util.AEPartLocation;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

/** A full block has no installable multipart facades. */
public final class EmptyFacades implements IFacadeContainer {
    public static final EmptyFacades INSTANCE = new EmptyFacades();
    private EmptyFacades() { }
    @Override public boolean canAddFacade(IFacadePart part) { return false; }
    @Override public boolean addFacade(IFacadePart part) { return false; }
    @Override public void removeFacade(IPartHost host, AEPartLocation side) { }
    @Override public IFacadePart getFacade(AEPartLocation side) { return null; }
    @Override public void rotateLeft() { }
    @Override public void writeToNBT(NBTTagCompound data) { }
    @Override public void readFromNBT(NBTTagCompound data) { }
    @Override public boolean readFromStream(ByteBuf data) { return false; }
    @Override public void writeToStream(ByteBuf data) { }
    @Override public boolean isEmpty() { return true; }
}
