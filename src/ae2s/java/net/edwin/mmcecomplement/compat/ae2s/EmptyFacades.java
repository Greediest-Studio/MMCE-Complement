package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.parts.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;

/** Full blocks never accept multipart facades. */
final class EmptyFacades implements IFacadeContainer {
    static final EmptyFacades INSTANCE = new EmptyFacades();
    @Override public boolean canAddFacade(IFacadePart part) { return false; }
    @Override public boolean addFacade(IFacadePart part) { return false; }
    @Override public void removeFacade(IPartHost host, EnumFacing side) { }
    @Override public IFacadePart getFacade(EnumFacing side) { return null; }
    @Override public void writeToNBT(NBTTagCompound data) { }
    @Override public void readFromNBT(NBTTagCompound data) { }
    @Override public boolean readFromStream(PacketBuffer data) { return false; }
    @Override public void writeToStream(PacketBuffer data) { }
    @Override public boolean isEmpty() { return true; }
}
