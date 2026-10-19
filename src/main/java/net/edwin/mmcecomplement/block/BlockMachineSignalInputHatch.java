package net.edwin.mmcecomplement.block;

import net.edwin.mmcecomplement.tile.TileMachineSignalInputHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockMachineSignalInputHatch extends BlockMachineSignalHatch {
    public BlockMachineSignalInputHatch() { super("mmce_complement.machine_signal_input_hatch"); }
    @Override @Nullable public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileMachineSignalInputHatch();
    }
}
