package net.edwin.mmcecomplement.compat.flux;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.gui.GuiFluxHatchHome;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.edwin.mmcecomplement.tile.TileFluxInputHatch;
import net.edwin.mmcecomplement.tile.TileFluxOutputHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.fluxnetworks.common.core.ContainerCore;

/** Flux GUI bridge loaded only while Flux Networks is available. */
public final class FluxGuiCompat {

    private FluxGuiCompat() {}

    public static Object getServerGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if (id == MMCEComplement.GUI_FLUX_INPUT_HATCH
                && tile instanceof TileFluxInputHatch) {
            return new ContainerCore(player, (TileFluxInputHatch) tile);
        }
        if (id == MMCEComplement.GUI_FLUX_OUTPUT_HATCH
                && tile instanceof TileFluxOutputHatch) {
            return new ContainerCore(player, (TileFluxOutputHatch) tile);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static Object getClientGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if ((id == MMCEComplement.GUI_FLUX_INPUT_HATCH
                || id == MMCEComplement.GUI_FLUX_OUTPUT_HATCH)
                && tile instanceof TileFluxHatchBase) {
            return new GuiFluxHatchHome(player, (TileFluxHatchBase) tile);
        }
        return null;
    }
}
