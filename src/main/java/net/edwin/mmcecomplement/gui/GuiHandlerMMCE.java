package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEEnergyBus;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEManaBus;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEEnergyBus;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEManaBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyBusBase;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyOutputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaBusBase;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaOutputBus;
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
        if (CompatMods.isAeEnergyCompatLoaded()
                && (id == MMCEComplement.GUI_ME_ENERGY_INPUT_BUS || id == MMCEComplement.GUI_ME_ENERGY_OUTPUT_BUS)
                && te instanceof TileMEEnergyBusBase) {
            return new ContainerMEEnergyBus(player, (TileMEEnergyBusBase) te);
        }
        if (CompatMods.isAeManaCompatLoaded()
                && (id == MMCEComplement.GUI_ME_MANA_INPUT_BUS || id == MMCEComplement.GUI_ME_MANA_OUTPUT_BUS)
                && te instanceof TileMEManaBusBase) {
            return new ContainerMEManaBus(player, (TileMEManaBusBase) te);
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
        if (CompatMods.isAeEnergyCompatLoaded()) {
            if (id == MMCEComplement.GUI_ME_ENERGY_INPUT_BUS && te instanceof TileMEEnergyInputBus) {
                return new GuiMEEnergyBus(player, (TileMEEnergyBusBase) te);
            }
            if (id == MMCEComplement.GUI_ME_ENERGY_OUTPUT_BUS && te instanceof TileMEEnergyOutputBus) {
                return new GuiMEEnergyBus(player, (TileMEEnergyBusBase) te);
            }
        }
        if (CompatMods.isAeManaCompatLoaded()) {
            if (id == MMCEComplement.GUI_ME_MANA_INPUT_BUS && te instanceof TileMEManaInputBus) {
                return new GuiMEManaBus(player, (TileMEManaBusBase) te);
            }
            if (id == MMCEComplement.GUI_ME_MANA_OUTPUT_BUS && te instanceof TileMEManaOutputBus) {
                return new GuiMEManaBus(player, (TileMEManaBusBase) te);
            }
        }
        return null;
    }
}
