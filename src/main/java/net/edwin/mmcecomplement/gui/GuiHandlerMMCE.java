package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.AeEnergyGuiCompat;
import net.edwin.mmcecomplement.compat.ae.AeGasGuiCompat;
import net.edwin.mmcecomplement.compat.ae.AeManaGuiCompat;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.edwin.mmcecomplement.compat.flux.FluxGuiCompat;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneInterfaceHatch;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileLiquidEnergizerHatch;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidOutputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileNineFluidOutputHatch;
import net.edwin.mmcecomplement.tile.TileFilteredItemOutputHatch;
import net.edwin.mmcecomplement.tile.TileFilteredFluidOutputHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/**
 * GUI handler for MMCE Complement.
 *
 * <p>Optional GUI implementations are delegated only after their dependency
 * probes succeed, keeping this common Forge entry point safe to load.
 */
public class GuiHandlerMMCE implements IGuiHandler {

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (CompatMods.isFluxCompatLoaded()) {
            Object gui = FluxGuiCompat.getServerGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeEnergyCompatLoaded()) {
            Object gui = AeEnergyGuiCompat.getServerGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeManaCompatLoaded()) {
            Object gui = AeManaGuiCompat.getServerGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeGasCompatLoaded()) {
            Object gui = AeGasGuiCompat.getServerGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeItemCompatLoaded()
                && id == MMCEComplement.GUI_ME_PATTERN_PROVIDER_II
                && te instanceof TileMEPatternProviderII) {
            return new ContainerMEPatternProviderII(
                (TileMEPatternProviderII) te, player);
        }
        if (id == MMCEComplement.GUI_BATCH_HATCH && te instanceof TileBatchHatch) {
            return new ContainerBatchHatch(player, (TileBatchHatch) te);
        }
        if ((id == MMCEComplement.GUI_REDSTONE_SIGNAL_INPUT_HATCH
                || id == MMCEComplement.GUI_REDSTONE_SIGNAL_OUTPUT_HATCH)
                && te instanceof TileRedstoneInterfaceHatch) {
            return new ContainerRedstoneInterfaceHatch(player,
                (TileRedstoneInterfaceHatch) te);
        }
        if (id == MMCEComplement.GUI_LIQUID_ENERGIZER_HATCH
                && te instanceof TileLiquidEnergizerHatch) {
            return new ContainerLiquidEnergizerHatch(
                player, (TileLiquidEnergizerHatch) te);
        }
        if (id == MMCEComplement.GUI_FILTERED_ITEM_OUTPUT_HATCH
                && te instanceof TileFilteredItemOutputHatch) {
            return new ContainerFilteredItemOutputHatch(player,
                (TileFilteredItemOutputHatch) te);
        }
        if (id == MMCEComplement.GUI_FILTERED_FLUID_OUTPUT_HATCH
                && te instanceof TileFilteredFluidOutputHatch) {
            return new ContainerFilteredFluidOutputHatch(player,
                (TileFilteredFluidOutputHatch) te);
        }
        if (id == MMCEComplement.GUI_DATA_INPUT_ASSEMBLY_HATCH
                && te instanceof TileDataItemInputHatch) {
            return new ContainerDataItemInputHatch(
                player, (TileDataItemInputHatch) te);
        }
        if (id == MMCEComplement.GUI_INPUT_ASSEMBLY_HATCH
                && te instanceof TileItemInputAssemblyHatch) {
            return new ContainerDataItemInputHatch(
                player, (TileItemInputAssemblyHatch) te, 71);
        }
        if (id == MMCEComplement.GUI_OUTPUT_ASSEMBLY_HATCH
                && te instanceof TileItemOutputAssemblyHatch) {
            return new ContainerItemOutputAssemblyHatch(
                player, (TileItemOutputAssemblyHatch) te);
        }
        if (id == MMCEComplement.GUI_SELF_CYCLE_ASSEMBLY_HATCH
                && te instanceof TileSelfCycleAssemblyHatch) {
            return new ContainerDataItemInputHatch(player,
                (TileSelfCycleAssemblyHatch) te, 71);
        }
        if (id == MMCEComplement.GUI_QUAD_FLUID_INPUT_HATCH
                && te instanceof TileQuadFluidInputHatch) {
            return new ContainerQuadFluidInputHatch(player, (TileQuadFluidInputHatch) te);
        }
        if (id == MMCEComplement.GUI_QUAD_FLUID_OUTPUT_HATCH
                && te instanceof TileQuadFluidOutputHatch) {
            return new ContainerQuadFluidInputHatch(player, (TileQuadFluidOutputHatch) te);
        }
        if (id == MMCEComplement.GUI_NINE_FLUID_INPUT_HATCH
                && te instanceof TileQuadFluidInputHatch
                && ((TileQuadFluidInputHatch) te).getTankCount() == TileNineFluidInputHatch.TANK_COUNT) {
            return new ContainerQuadFluidInputHatch(player, (TileQuadFluidInputHatch) te);
        }
        if (id == MMCEComplement.GUI_NINE_FLUID_OUTPUT_HATCH
                && te instanceof TileQuadFluidOutputHatch
                && ((TileQuadFluidOutputHatch) te).getTankCount() == TileNineFluidOutputHatch.TANK_COUNT) {
            return new ContainerQuadFluidInputHatch(player, (TileQuadFluidOutputHatch) te);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (CompatMods.isFluxCompatLoaded()) {
            Object gui = FluxGuiCompat.getClientGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeEnergyCompatLoaded()) {
            Object gui = AeEnergyGuiCompat.getClientGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeManaCompatLoaded()) {
            Object gui = AeManaGuiCompat.getClientGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeGasCompatLoaded()) {
            Object gui = AeGasGuiCompat.getClientGui(id, player, te);
            if (gui != null) return gui;
        }
        if (CompatMods.isAeItemCompatLoaded()
                && id == MMCEComplement.GUI_ME_PATTERN_PROVIDER_II
                && te instanceof TileMEPatternProviderII) {
            return new GuiMEPatternProviderII(
                (TileMEPatternProviderII) te, player);
        }
        if (id == MMCEComplement.GUI_BATCH_HATCH && te instanceof TileBatchHatch) {
            return new GuiBatchHatch(player, (TileBatchHatch) te);
        }
        if ((id == MMCEComplement.GUI_REDSTONE_SIGNAL_INPUT_HATCH
                || id == MMCEComplement.GUI_REDSTONE_SIGNAL_OUTPUT_HATCH)
                && te instanceof TileRedstoneInterfaceHatch) {
            return new GuiRedstoneInterfaceHatch(player,
                (TileRedstoneInterfaceHatch) te);
        }
        if (id == MMCEComplement.GUI_LIQUID_ENERGIZER_HATCH
                && te instanceof TileLiquidEnergizerHatch) {
            return new GuiLiquidEnergizerHatch(
                player, (TileLiquidEnergizerHatch) te);
        }
        if (id == MMCEComplement.GUI_FILTERED_ITEM_OUTPUT_HATCH
                && te instanceof TileFilteredItemOutputHatch) {
            return new GuiFilteredItemOutputHatch(player,
                (TileFilteredItemOutputHatch) te);
        }
        if (id == MMCEComplement.GUI_FILTERED_FLUID_OUTPUT_HATCH
                && te instanceof TileFilteredFluidOutputHatch) {
            return new GuiFilteredFluidOutputHatch(player,
                (TileFilteredFluidOutputHatch) te);
        }
        if (id == MMCEComplement.GUI_DATA_INPUT_ASSEMBLY_HATCH
                && te instanceof TileDataItemInputHatch) {
            return new GuiDataItemInputHatch(
                player, (TileDataItemInputHatch) te);
        }
        if (id == MMCEComplement.GUI_INPUT_ASSEMBLY_HATCH
                && te instanceof TileItemInputAssemblyHatch) {
            return new GuiItemInputAssemblyHatch(
                player, (TileItemInputAssemblyHatch) te);
        }
        if (id == MMCEComplement.GUI_OUTPUT_ASSEMBLY_HATCH
                && te instanceof TileItemOutputAssemblyHatch) {
            return new GuiItemOutputAssemblyHatch(
                player, (TileItemOutputAssemblyHatch) te);
        }
        if (id == MMCEComplement.GUI_SELF_CYCLE_ASSEMBLY_HATCH
                && te instanceof TileSelfCycleAssemblyHatch) {
            return new GuiItemInputAssemblyHatch(player,
                (TileSelfCycleAssemblyHatch) te);
        }
        if (id == MMCEComplement.GUI_QUAD_FLUID_INPUT_HATCH
                && te instanceof TileQuadFluidInputHatch) {
            return new GuiQuadFluidInputHatch(player, (TileQuadFluidInputHatch) te);
        }
        if (id == MMCEComplement.GUI_QUAD_FLUID_OUTPUT_HATCH
                && te instanceof TileQuadFluidOutputHatch) {
            return new GuiQuadFluidInputHatch(player, (TileQuadFluidOutputHatch) te);
        }
        if (id == MMCEComplement.GUI_NINE_FLUID_INPUT_HATCH
                && te instanceof TileQuadFluidInputHatch
                && ((TileQuadFluidInputHatch) te).getTankCount() == TileNineFluidInputHatch.TANK_COUNT) {
            return new GuiQuadFluidInputHatch(player, (TileQuadFluidInputHatch) te);
        }
        if (id == MMCEComplement.GUI_NINE_FLUID_OUTPUT_HATCH
                && te instanceof TileQuadFluidOutputHatch
                && ((TileQuadFluidOutputHatch) te).getTankCount() == TileNineFluidOutputHatch.TANK_COUNT) {
            return new GuiQuadFluidInputHatch(player, (TileQuadFluidOutputHatch) te);
        }
        return null;
    }
}
