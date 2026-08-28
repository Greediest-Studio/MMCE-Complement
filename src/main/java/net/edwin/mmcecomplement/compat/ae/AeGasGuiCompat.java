package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEOutputAssembly;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEOutputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Mekanism Energistics GUI bridge. */
public final class AeGasGuiCompat {

    private AeGasGuiCompat() {}

    public static Object getServerGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if (isInputAssembly(id) && tile instanceof TileMEInputAssembly) {
            return new ContainerMEInputAssembly(
                (TileMEInputAssembly) tile, player);
        }
        if (id == MMCEComplement.GUI_ME_OUTPUT_ASSEMBLY
                && tile instanceof TileMEOutputAssembly) {
            return new ContainerMEOutputAssembly(
                (TileMEOutputAssembly) tile, player);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static Object getClientGui(int id, EntityPlayer player,
                                      TileEntity tile) {
        if (isInputAssembly(id) && tile instanceof TileMEInputAssembly) {
            return new GuiMEInputAssembly((TileMEInputAssembly) tile, player);
        }
        if (id == MMCEComplement.GUI_ME_OUTPUT_ASSEMBLY
                && tile instanceof TileMEOutputAssembly) {
            return new GuiMEOutputAssembly((TileMEOutputAssembly) tile, player);
        }
        return null;
    }

    private static boolean isInputAssembly(int id) {
        return id == MMCEComplement.GUI_ME_INPUT_ASSEMBLY
            || id == MMCEComplement.GUI_ME_INVENTORY_INPUT_ASSEMBLY
            || id == MMCEComplement.GUI_ME_FULL_EXPOSURE_ASSEMBLY;
    }
}
