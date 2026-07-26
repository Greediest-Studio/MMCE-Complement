package net.edwin.mmcecomplement.network;

import net.edwin.mmcecomplement.MMCEComplement;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEEnergyBusBase;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEManaBusBase;
import net.edwin.mmcecomplement.tile.TileFluxHatchBase;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.edwin.mmcecomplement.gui.ContainerQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fluids.FluidUtil;
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
        if (CompatMods.isFluxCompatLoaded()) {
            CHANNEL.registerMessage(SetHatchNetworkHandler.class,
                SetHatchNetworkMessage.class, 0, Side.SERVER);
        }
        CHANNEL.registerMessage(SetHatchFieldHandler.class,
                SetHatchFieldMessage.class, 1, Side.SERVER);
        CHANNEL.registerMessage(InteractQuadFluidTankHandler.class,
                InteractQuadFluidTankMessage.class, 2, Side.SERVER);
    }

    // -- Field IDs ------------------------------------------------------

    public static final int FIELD_CUSTOM_NAME   = 1;
    public static final int FIELD_PRIORITY      = 2;
    public static final int FIELD_LIMIT         = 3;
    public static final int FIELD_SURGE_MODE    = 4;
    public static final int FIELD_DISABLE_LIMIT = 5;
    public static final int FIELD_CHUNK_LOAD    = 6;
    public static final int FIELD_BUFFER_CAP    = 7;
    public static final int FIELD_BATCH_MAX_TIME = 8;

    // -- Quad fluid tank interaction -----------------------------------

    public static final class InteractQuadFluidTankMessage implements IMessage {
        public BlockPos pos;
        public int dim;
        public int tank;

        public InteractQuadFluidTankMessage() {}

        public InteractQuadFluidTankMessage(BlockPos pos, int dim, int tank) {
            this.pos = pos;
            this.dim = dim;
            this.tank = tank;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            PacketBuffer pb = new PacketBuffer(buf);
            this.pos = pb.readBlockPos();
            this.dim = pb.readInt();
            this.tank = pb.readByte();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            PacketBuffer pb = new PacketBuffer(buf);
            pb.writeBlockPos(pos);
            pb.writeInt(dim);
            pb.writeByte(tank);
        }
    }

    public static final class InteractQuadFluidTankHandler
        implements IMessageHandler<InteractQuadFluidTankMessage, IMessage> {

        @Override
        public IMessage onMessage(InteractQuadFluidTankMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player != null) {
                player.getServerWorld().addScheduledTask(() -> apply(player, msg));
            }
            return null;
        }

        private static void apply(EntityPlayerMP player, InteractQuadFluidTankMessage msg) {
            if (msg.tank < 0 || msg.tank >= TileQuadFluidInputHatch.TANK_COUNT
                || player.dimension != msg.dim
                || player.getDistanceSq(msg.pos) > 64.0D
                || !(player.openContainer instanceof ContainerQuadFluidInputHatch)) {
                return;
            }
            WorldServer world = player.getServerWorld();
            TileEntity tile = world.getTileEntity(msg.pos);
            ContainerQuadFluidInputHatch container =
                (ContainerQuadFluidInputHatch) player.openContainer;
            if (!(tile instanceof TileQuadFluidInputHatch)
                || container.getTile() != tile) {
                return;
            }
            TileQuadFluidInputHatch hatch = (TileQuadFluidInputHatch) tile;
            if (FluidUtil.interactWithFluidHandler(
                player, EnumHand.MAIN_HAND, hatch.getTankInteractionHandler(msg.tank))) {
                hatch.markNoUpdateSync();
                player.inventoryContainer.detectAndSendChanges();
                container.detectAndSendChanges();
            }
        }
    }

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
            NBTTagCompound nbt = msg.payload;

            if (te instanceof TileBatchHatch
                    && msg.fieldId == FIELD_BATCH_MAX_TIME) {
                TileBatchHatch hatch = (TileBatchHatch) te;
                hatch.setMaxBatchTime(nbt.getInteger("v"));
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                return;
            }

            if (CompatMods.isFluxCompatLoaded() && te instanceof TileFluxHatchBase) {
                TileFluxHatchBase hatch = (TileFluxHatchBase) te;

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
                    case FIELD_BUFFER_CAP:
                        hatch.setBufferCapacityRaw(nbt.getLong("v"));
                        break;
                    default:
                        return;
                }
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                return;
            }

            if (CompatMods.isAeEnergyCompatLoaded()
                    && te instanceof TileMEEnergyBusBase
                    && msg.fieldId == FIELD_BUFFER_CAP) {
                TileMEEnergyBusBase bus = (TileMEEnergyBusBase) te;
                bus.setBufferCapacityRaw(nbt.getLong("v"));
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                return;
            }

            if (CompatMods.isAeManaCompatLoaded()
                    && te instanceof TileMEManaBusBase
                    && msg.fieldId == FIELD_BUFFER_CAP) {
                TileMEManaBusBase bus = (TileMEManaBusBase) te;
                bus.setBufferCapacityRaw(nbt.getLong("v"));
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
            }
        }

        private static String clampName(String s) {
            if (s == null) return "";
            return s.length() > 24 ? s.substring(0, 24) : s;
        }
    }
}
