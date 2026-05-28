package net.edwin.mmcecomplement.network;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkCache;

import io.netty.buffer.ByteBuf;

/**
 * Tiny SimpleNetworkWrapper plus the single packet we need to ask the server
 * to connect a {@link TileFluxHatchBase} to a Flux Network the player just
 * picked in the GUI.
 *
 * <p>Flux Networks' own {@code PacketTile} does an {@code instanceof TileFluxCore}
 * check on the server and drops anything else, which makes it unusable for our
 * tile (we do not extend {@code TileFluxCore}). This packet is the minimum
 * required to drive the FN selection UI for a third-party tile.
 */
public final class NetworkHandlerMMCE {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private NetworkHandlerMMCE() {}

    public static void register() {
        CHANNEL.registerMessage(SetHatchNetworkHandler.class,
                SetHatchNetworkMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(SetHatchFieldHandler.class,
                SetHatchFieldMessage.class, 1, Side.SERVER);
    }

    // -- Field IDs ------------------------------------------------------

    public static final int FIELD_CUSTOM_NAME   = 1;
    public static final int FIELD_PRIORITY      = 2;
    public static final int FIELD_LIMIT         = 3;
    public static final int FIELD_SURGE_MODE    = 4;
    public static final int FIELD_DISABLE_LIMIT = 5;
    public static final int FIELD_CHUNK_LOAD    = 6;

    // -- Packet ---------------------------------------------------------

    public static final class SetHatchNetworkMessage implements IMessage {
        public BlockPos pos;
        public int dim;
        public int networkID;

        public SetHatchNetworkMessage() {}

        public SetHatchNetworkMessage(BlockPos pos, int dim, int networkID) {
            this.pos = pos;
            this.dim = dim;
            this.networkID = networkID;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            PacketBuffer pb = new PacketBuffer(buf);
            this.pos = pb.readBlockPos();
            this.dim = pb.readInt();
            this.networkID = pb.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            PacketBuffer pb = new PacketBuffer(buf);
            pb.writeBlockPos(this.pos);
            pb.writeInt(this.dim);
            pb.writeInt(this.networkID);
        }
    }

    public static final class SetHatchNetworkHandler
            implements IMessageHandler<SetHatchNetworkMessage, IMessage> {

        @Override
        public IMessage onMessage(SetHatchNetworkMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(() -> apply(player, msg));
            return null;
        }

        private static void apply(EntityPlayerMP player, SetHatchNetworkMessage msg) {
            WorldServer world = FMLCommonHandler.instance()
                    .getMinecraftServerInstance()
                    .getWorld(msg.dim);
            if (world == null) {
                return;
            }
            // Basic distance check — match vanilla container interact range.
            if (player.getDistanceSq(msg.pos) > 64.0D) {
                return;
            }
            TileEntity te = world.getTileEntity(msg.pos);
            if (!(te instanceof TileFluxHatchBase)) {
                return;
            }
            TileFluxHatchBase hatch = (TileFluxHatchBase) te;

            IFluxNetwork current = hatch.getNetwork();
            if (current != null && !current.isInvalid()) {
                current.queueConnectionRemoval(hatch, false);
                hatch.disconnect(current);
            }

            if (msg.networkID > 0) {
                IFluxNetwork target = FluxNetworkCache.instance.getNetwork(msg.networkID);
                if (target != null && !target.isInvalid()) {
                    hatch.connect(target);
                    target.queueConnectionAddition(hatch);
                }
            }
            net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
            world.notifyBlockUpdate(msg.pos, state, state, 3);
            MMCEComplement.LOGGER.debug(
                    "Hatch at {} connected to network id={} by {}",
                    msg.pos, msg.networkID, player.getName());
        }
    }

    // -- SetHatchField (edits home-tab fields) --------------------------

    public static final class SetHatchFieldMessage implements IMessage {
        public BlockPos pos;
        public int dim;
        public int fieldId;
        public NBTTagCompound payload;

        public SetHatchFieldMessage() {}

        public SetHatchFieldMessage(BlockPos pos, int dim, int fieldId, NBTTagCompound payload) {
            this.pos = pos;
            this.dim = dim;
            this.fieldId = fieldId;
            this.payload = payload;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            PacketBuffer pb = new PacketBuffer(buf);
            this.pos = pb.readBlockPos();
            this.dim = pb.readInt();
            this.fieldId = pb.readInt();
            try {
                this.payload = pb.readCompoundTag();
            } catch (Exception e) {
                this.payload = new NBTTagCompound();
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            PacketBuffer pb = new PacketBuffer(buf);
            pb.writeBlockPos(this.pos);
            pb.writeInt(this.dim);
            pb.writeInt(this.fieldId);
            pb.writeCompoundTag(this.payload == null ? new NBTTagCompound() : this.payload);
        }
    }

    public static final class SetHatchFieldHandler
            implements IMessageHandler<SetHatchFieldMessage, IMessage> {

        @Override
        public IMessage onMessage(SetHatchFieldMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(() -> apply(player, msg));
            return null;
        }

        private static void apply(EntityPlayerMP player, SetHatchFieldMessage msg) {
            WorldServer world = FMLCommonHandler.instance()
                    .getMinecraftServerInstance()
                    .getWorld(msg.dim);
            if (world == null || msg.payload == null) {
                return;
            }
            if (player.getDistanceSq(msg.pos) > 64.0D) {
                return;
            }
            TileEntity te = world.getTileEntity(msg.pos);
            if (!(te instanceof TileFluxHatchBase)) {
                return;
            }
            TileFluxHatchBase hatch = (TileFluxHatchBase) te;
            NBTTagCompound nbt = msg.payload;

            switch (msg.fieldId) {
                case FIELD_CUSTOM_NAME:
                    hatch.setCustomNameRaw(clampName(nbt.getString("v")));
                    break;
                case FIELD_PRIORITY:
                    hatch.setPriorityRaw(nbt.getInteger("v"));
                    break;
                case FIELD_LIMIT:
                    hatch.setTransferLimitRaw(nbt.getLong("v"));
                    break;
                case FIELD_SURGE_MODE:
                    hatch.setSurgeModeRaw(nbt.getBoolean("v"));
                    break;
                case FIELD_DISABLE_LIMIT:
                    hatch.setDisableLimitRaw(nbt.getBoolean("v"));
                    break;
                case FIELD_CHUNK_LOAD:
                    hatch.setChunkLoadingRequested(nbt.getBoolean("v"));
                    break;
                default:
                    return;
            }
            net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
            world.notifyBlockUpdate(msg.pos, state, state, 3);
        }

        private static String clampName(String s) {
            if (s == null) return "";
            return s.length() > 24 ? s.substring(0, 24) : s;
        }
    }
}
