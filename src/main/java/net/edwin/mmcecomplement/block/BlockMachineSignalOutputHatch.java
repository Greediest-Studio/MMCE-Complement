package net.edwin.mmcecomplement.block;

import net.edwin.mmcecomplement.tile.TileMachineSignalOutputHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockMachineSignalOutputHatch extends BlockMachineSignalHatch {
    public BlockMachineSignalOutputHatch() { super("mmce_complement.machine_signal_output_hatch"); }
    @Override @Nullable public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileMachineSignalOutputHatch();
    }
}
