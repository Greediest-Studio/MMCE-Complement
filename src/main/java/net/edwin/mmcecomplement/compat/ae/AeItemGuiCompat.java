package net.edwin.mmcecomplement.compat.ae;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEDataPatternProviderII;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssemblyFallback;
import net.edwin.mmcecomplement.compat.ae.gui.ContainerMEFullExposureFallback;
import net.edwin.mmcecomplement.compat.ae.gui.GuiMEFullExposureFallback;

/** Loaded only by the UEL branch; common GUI verification never resolves appeng. */
public final class AeItemGuiCompat {
    public static Object getServerGui(int id, EntityPlayer player, TileEntity te) {
        if (id == MMCEComplement.GUI_ME_FULL_EXPOSURE_ASSEMBLY
                && te instanceof TileMEFullExposureAssemblyFallback) {
            return new ContainerMEFullExposureFallback(
                (TileMEFullExposureAssemblyFallback) te, player);
        }
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II
                && te instanceof TileMEDataPatternProviderII) {
            MMCEComplement.LOGGER.info(
                "Opening server GUI for data pattern provider II at {}, tile={}",
                te.getPos(), te.getClass().getName());
            return new ContainerMEPatternProviderII(
                (TileMEDataPatternProviderII) te, player);
        }
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II) {
            MMCEComplement.LOGGER.error(
                "Rejected server GUI {}: tile at {} is {}", id,
                te == null ? null : te.getPos(),
                te == null ? "null" : te.getClass().getName());
        }
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER
                && te instanceof TileMEDataPatternProvider) {
            return new github.kasuminova.mmce.common.container
                .ContainerMEPatternProvider(
                    (TileMEDataPatternProvider) te, player);
        }
        if (CompatMods.isAeItemCompatLoaded()
                && id == MMCEComplement.GUI_ME_PATTERN_PROVIDER_II
                && te instanceof TileMEPatternProviderII) {
            return new ContainerMEPatternProviderII(
                (TileMEPatternProviderII) te, player);
        }
        return null;
    }
    @SideOnly(Side.CLIENT)
    public static Object getClientGui(int id, EntityPlayer player, World world, BlockPos pos, TileEntity te) {
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II) {
            te = BlockMEDataPatternProviderII.upgradeStaleProviderTile(
                world, pos, te);
        }
        if (id == MMCEComplement.GUI_ME_FULL_EXPOSURE_ASSEMBLY
                && te instanceof TileMEFullExposureAssemblyFallback) {
            return new GuiMEFullExposureFallback(
                (TileMEFullExposureAssemblyFallback) te, player);
        }
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II
                && te instanceof TileMEDataPatternProviderII) {
            MMCEComplement.LOGGER.info(
                "Opening client GUI for data pattern provider II at {}, tile={}",
                pos, te.getClass().getName());
            return new GuiMEDataPatternProviderII(
                (TileMEDataPatternProviderII) te, player);
        }
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER_II) {
            MMCEComplement.LOGGER.error(
                "Rejected client GUI {}: tile at {} is {}", id,
                pos,
                te == null ? "null" : te.getClass().getName());
        }
        if (id == MMCEComplement.GUI_ME_DATA_PATTERN_PROVIDER
                && te instanceof TileMEDataPatternProvider) {
            return new GuiMEDataPatternProvider(
                (TileMEDataPatternProvider) te, player);
        }
        if (CompatMods.isAeItemCompatLoaded()
                && id == MMCEComplement.GUI_ME_PATTERN_PROVIDER_II
                && te instanceof TileMEPatternProviderII) {
            return new GuiMEPatternProviderII(
                (TileMEPatternProviderII) te, player);
        }
        return null;
    }
}
