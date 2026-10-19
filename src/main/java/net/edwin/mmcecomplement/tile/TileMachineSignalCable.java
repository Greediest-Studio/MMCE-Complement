package net.edwin.mmcecomplement.tile;

import net.edwin.mmcecomplement.machineconnection.MachineConnectionRegistry;
import net.minecraft.nbt.NBTTagCompound;

/** Cable node. A negative color means uncolored and connects to every color. */
public class TileMachineSignalCable extends hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent {
    private int cableColor = -1;

    public int getCableColor() {
        return cableColor;
    }

    /** Sets the registry colour while the tile is being created, before it has
     * a world or position.  This deliberately avoids update notifications. */
    public void initializeCableColor(int color) {
        cableColor = color;
    }

    public void setCableColor(int color) {
        cableColor = color;
        markDirty();
        if (world != null) {
            markForUpdateSync();
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1),
                pos.add(1, 1, 1));
            if (!world.isRemote && world.getMinecraftServer() != null) {
                world.getMinecraftServer().addScheduledTask(() ->
                    MachineConnectionRegistry.refresh(world));
            }
        }
    }

    @Override
    public void readCustomNBT(NBTTagCompound tag) {
        super.readCustomNBT(tag);
        cableColor = tag.hasKey("signalCableColor", 3) ? tag.getInteger("signalCableColor") : -1;
    }

    @Override
    public void writeCustomNBT(NBTTagCompound tag) {
        super.writeCustomNBT(tag);
        tag.setInteger("signalCableColor", cableColor);
    }
}
