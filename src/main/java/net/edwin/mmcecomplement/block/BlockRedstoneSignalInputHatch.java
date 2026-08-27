package net.edwin.mmcecomplement.block;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.tile.TileRedstoneSignalInputHatch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Redstone data sink. */
public class BlockRedstoneSignalInputHatch extends BlockRedstoneInterfaceHatch {

    public BlockRedstoneSignalInputHatch() {
        super("mmce_complement.redstone_signal_input_hatch",
            MMCEComplement.GUI_REDSTONE_SIGNAL_INPUT_HATCH,
            "tile.mmce_complement.redstone_signal_input_hatch.tip");
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        return new TileRedstoneSignalInputHatch();
    }
}
