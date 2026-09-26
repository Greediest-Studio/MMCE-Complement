package net.edwin.mmcecomplement.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableBiomeProvider;
import net.edwin.mmcecomplement.compat.mmcea.tile.TileConfigurableDimensionProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/** Displays the stored override on configurable MMCE Addons detector hatches. */
public class ConfigurableDetectorProbeProvider implements IProbeInfoProvider {

    @Override
    public String getID() {
        return "mmce_complement:configurable_detector";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player,
                             World world, IBlockState blockState, IProbeHitData data) {
        TileEntity tile = world.getTileEntity(data.getPos());
        if (tile instanceof TileConfigurableBiomeProvider) {
            TileConfigurableBiomeProvider biome = (TileConfigurableBiomeProvider) tile;
            String bound = biome.getBoundBiomeRegistryName();
            if (bound == null) {
                probeInfo.text(new TextComponentTranslation(
                        "top.mmce_complement.detector.unbound"));
            } else {
                probeInfo.text(new TextComponentTranslation(
                        "top.mmce_complement.detector.biome", bound));
            }
        } else if (tile instanceof TileConfigurableDimensionProvider) {
            TileConfigurableDimensionProvider dimension =
                    (TileConfigurableDimensionProvider) tile;
            Integer bound = dimension.getBoundDimensionId();
            if (bound == null) {
                probeInfo.text(new TextComponentTranslation(
                        "top.mmce_complement.detector.unbound"));
            } else {
                probeInfo.text(new TextComponentTranslation(
                        "top.mmce_complement.detector.dimension", bound));
            }
        }
    }
}
