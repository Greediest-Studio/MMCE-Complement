package net.edwin.mmcecomplement.compat.flux;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.network.NetworkHandlerMMCE;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkCache;

/** Flux packet actions isolated from the always-loaded network entry point. */
public final class FluxNetworkCompat {

    private FluxNetworkCompat() {}

    public static void applyNetwork(EntityPlayerMP player, WorldServer world,
                                    BlockPos pos, int networkId) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileFluxHatchBase)) return;
        TileFluxHatchBase hatch = (TileFluxHatchBase) tile;

        IFluxNetwork current = hatch.getNetwork();
        if (current != null && !current.isInvalid()) {
            current.queueConnectionRemoval(hatch, false);
            hatch.disconnect(current);
        }
        if (networkId > 0) {
            IFluxNetwork target = FluxNetworkCache.instance.getNetwork(networkId);
            if (target != null && !target.isInvalid()) {
                hatch.connect(target);
                target.queueConnectionAddition(hatch);
            }
        }
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        MMCEComplement.LOGGER.debug(
            "Hatch at {} connected to network id={} by {}",
            pos, networkId, player.getName());
    }

    public static boolean applyField(TileEntity tile, int fieldId,
                                     NBTTagCompound nbt, WorldServer world,
                                     BlockPos pos) {
        if (!(tile instanceof TileFluxHatchBase)) return false;
        TileFluxHatchBase hatch = (TileFluxHatchBase) tile;
        switch (fieldId) {
            case NetworkHandlerMMCE.FIELD_CUSTOM_NAME:
                hatch.setCustomNameRaw(clampName(nbt.getString("v")));
                break;
            case NetworkHandlerMMCE.FIELD_PRIORITY:
                hatch.setPriorityRaw(nbt.getInteger("v"));
                break;
            case NetworkHandlerMMCE.FIELD_LIMIT:
                hatch.setTransferLimitRaw(nbt.getLong("v"));
                break;
            case NetworkHandlerMMCE.FIELD_SURGE_MODE:
                hatch.setSurgeModeRaw(nbt.getBoolean("v"));
                break;
            case NetworkHandlerMMCE.FIELD_DISABLE_LIMIT:
                hatch.setDisableLimitRaw(nbt.getBoolean("v"));
                break;
            case NetworkHandlerMMCE.FIELD_CHUNK_LOAD:
                hatch.setChunkLoadingRequested(nbt.getBoolean("v"));
                break;
            case NetworkHandlerMMCE.FIELD_BUFFER_CAP:
                hatch.setBufferCapacityRaw(nbt.getLong("v"));
                break;
            default:
                return false;
        }
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
        return true;
    }

    private static String clampName(String value) {
        if (value == null) return "";
        return value.length() > 24 ? value.substring(0, 24) : value;
    }
}
