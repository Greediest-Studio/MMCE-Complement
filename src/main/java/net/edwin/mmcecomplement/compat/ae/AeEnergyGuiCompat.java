package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEEnergyBus;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEEnergyBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyBusBase;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyOutputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** CrazyAE energy GUI bridge. */
public final class AeEnergyGuiCompat {

    private AeEnergyGuiCompat() {}

    public static Object getServerGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if ((id == MMCEComplement.GUI_ME_ENERGY_INPUT_BUS
                || id == MMCEComplement.GUI_ME_ENERGY_OUTPUT_BUS)
                && tile instanceof TileMEEnergyBusBase) {
            return new ContainerMEEnergyBus(player,
                (TileMEEnergyBusBase) tile);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static Object getClientGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if ((id == MMCEComplement.GUI_ME_ENERGY_INPUT_BUS
                && tile instanceof TileMEEnergyInputBus)
                || (id == MMCEComplement.GUI_ME_ENERGY_OUTPUT_BUS
                && tile instanceof TileMEEnergyOutputBus)) {
            return new GuiMEEnergyBus(player, (TileMEEnergyBusBase) tile);
        }
        return null;
    }
}
