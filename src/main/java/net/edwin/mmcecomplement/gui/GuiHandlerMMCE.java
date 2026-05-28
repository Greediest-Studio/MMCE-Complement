package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import sonar.fluxnetworks.common.core.ContainerCore;

import javax.annotation.Nullable;

/**
 * GUI handler for MMCE Complement.
 *
 * <p>Reuses Flux Networks' {@link ContainerCore} on the server and
 * {@link GuiFluxHatchHome} on the client for both the input and output
 * hatches — the GUIs are generic over {@link TileFluxHatchBase}.
 */
public class GuiHandlerMMCE implements IGuiHandler {

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == MMCEComplement.GUI_FLUX_INPUT_HATCH && te instanceof TileFluxInputHatch) {
            return new ContainerCore(player, (TileFluxInputHatch) te);
        }
        if (id == MMCEComplement.GUI_FLUX_OUTPUT_HATCH && te instanceof TileFluxOutputHatch) {
            return new ContainerCore(player, (TileFluxOutputHatch) te);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if ((id == MMCEComplement.GUI_FLUX_INPUT_HATCH || id == MMCEComplement.GUI_FLUX_OUTPUT_HATCH)
                && te instanceof TileFluxHatchBase) {
            return new GuiFluxHatchHome(player, (TileFluxHatchBase) te);
        }
        return null;
    }
}
