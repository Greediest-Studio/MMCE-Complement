package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEManaBus;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEManaBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaBusBase;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaOutputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** CrazyAE/Modular Magic mana GUI bridge. */
public final class AeManaGuiCompat {

    private AeManaGuiCompat() {}

    public static Object getServerGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if ((id == MMCEComplement.GUI_ME_MANA_INPUT_BUS
                || id == MMCEComplement.GUI_ME_MANA_OUTPUT_BUS)
                && tile instanceof TileMEManaBusBase) {
            return new ContainerMEManaBus(player, (TileMEManaBusBase) tile);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static Object getClientGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if ((id == MMCEComplement.GUI_ME_MANA_INPUT_BUS
                && tile instanceof TileMEManaInputBus)
                || (id == MMCEComplement.GUI_ME_MANA_OUTPUT_BUS
                && tile instanceof TileMEManaOutputBus)) {
            return new GuiMEManaBus(player, (TileMEManaBusBase) tile);
        }
        return null;
    }
}
