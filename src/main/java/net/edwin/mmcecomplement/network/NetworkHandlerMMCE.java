package net.edwin.mmcecomplement.network;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.AeEnergyNetworkCompat;
import net.edwin.mmcecomplement.compat.ae.AeGasNetworkCompat;
import net.edwin.mmcecomplement.compat.ae.AeManaNetworkCompat;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import net.edwin.mmcecomplement.compat.flux.FluxNetworkCompat;
import net.edwin.mmcecomplement.tile.TileBatchHatch;
import net.edwin.mmcecomplement.tile.TileRedstoneInterfaceHatch;
import net.edwin.mmcecomplement.gui.ContainerRedstoneInterfaceHatch;
import net.edwin.mmcecomplement.tile.TileDataItemInputHatch;
import net.edwin.mmcecomplement.tile.TileItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.gui.ContainerDataItemInputHatch;
import net.edwin.mmcecomplement.gui.ContainerItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.gui.ContainerQuadFluidInputHatch;
import net.edwin.mmcecomplement.gui.ContainerLiquidEnergizerHatch;
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.edwin.mmcecomplement.tile.TileLiquidEnergizerHatch;
import net.edwin.mmcecomplement.tile.TileFilteredItemOutputHatch;
import net.edwin.mmcecomplement.tile.TileFilteredFluidOutputHatch;
import net.edwin.mmcecomplement.gui.ContainerFilteredItemOutputHatch;
import net.edwin.mmcecomplement.gui.ContainerFilteredFluidOutputHatch;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.item.ItemStack;

import io.netty.buffer.ByteBuf;

/**
 * Tiny SimpleNetworkWrapper plus the single packet we need to ask the server
 * to connect an optional Flux hatch to a Flux Network the player just
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
    public static final int FIELD_DATA_INPUT_ASSEMBLY_VALUE = 9;
    public static final int FIELD_ME_ORE_DICT_WHITELIST = 10;
    public static final int FIELD_ME_ORE_DICT_BLACKLIST = 11;
    public static final int FIELD_ME_ORE_DICT_ACTIVE = 12;
    public static final int FIELD_FILTERED_ITEM_OUTPUT = 13;
    public static final int FIELD_FILTERED_FLUID_OUTPUT = 14;
    public static final int FIELD_ME_INVENTORY_ACTIVE = 15;
    public static final int FIELD_ME_INVENTORY_RESERVE = 16;
    public static final int FIELD_ME_INPUT_ASSEMBLY_MARKER = 17;
    public static final int FIELD_REDSTONE_INTERFACE_NAME = 18;

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
            if (msg.tank < 0 || player.dimension != msg.dim
                || player.getDistanceSq(msg.pos) > 64.0D) {
                return;
            }
            WorldServer world = player.getServerWorld();
            TileEntity tile = world.getTileEntity(msg.pos);
            net.minecraftforge.fluids.capability.IFluidHandler handler;
            Container container;
            if (tile instanceof TileQuadFluidInputHatch
                && player.openContainer instanceof ContainerQuadFluidInputHatch
                && ((ContainerQuadFluidInputHatch) player.openContainer)
                    .getTile() == tile) {
                TileQuadFluidInputHatch hatch =
                    (TileQuadFluidInputHatch) tile;
                if (msg.tank >= hatch.getTankCount()) {
                    return;
                }
                handler = hatch.getTankInteractionHandler(msg.tank);
                container = (ContainerQuadFluidInputHatch) player.openContainer;
            } else if (tile instanceof TileDataItemInputHatch
                && player.openContainer instanceof ContainerDataItemInputHatch
                && ((ContainerDataItemInputHatch) player.openContainer)
                    .getTile() == tile) {
                TileDataItemInputHatch hatch =
                    (TileDataItemInputHatch) tile;
                if (msg.tank >= hatch.getTankCount()) {
                    return;
                }
                handler = hatch.getTankInteractionHandler(msg.tank);
                container = (ContainerDataItemInputHatch) player.openContainer;
            } else if (tile instanceof TileItemOutputAssemblyHatch
                && player.openContainer instanceof ContainerItemOutputAssemblyHatch
                && ((ContainerItemOutputAssemblyHatch) player.openContainer)
                    .getTile() == tile) {
                TileItemOutputAssemblyHatch hatch =
                    (TileItemOutputAssemblyHatch) tile;
                if (msg.tank >= hatch.getTankCount()) return;
                handler = hatch.getTankInteractionHandler(msg.tank);
                container = (ContainerItemOutputAssemblyHatch) player.openContainer;
            } else if (msg.tank == 0
                && tile instanceof TileLiquidEnergizerHatch
                && player.openContainer instanceof ContainerLiquidEnergizerHatch
                && ((ContainerLiquidEnergizerHatch) player.openContainer)
                    .getTile() == tile) {
                handler = (TileLiquidEnergizerHatch) tile;
                container = (ContainerLiquidEnergizerHatch) player.openContainer;
            } else if (msg.tank == 0
                && tile instanceof TileFilteredFluidOutputHatch
                && player.openContainer
                    instanceof ContainerFilteredFluidOutputHatch
                && ((ContainerFilteredFluidOutputHatch) player.openContainer)
                    .getTile() == tile) {
                handler = ((TileFilteredFluidOutputHatch) tile)
                    .getTankInteractionHandler();
                container = (ContainerFilteredFluidOutputHatch)
                    player.openContainer;
            } else {
                return;
            }
            if (FluidUtil.interactWithFluidHandler(
                player, EnumHand.MAIN_HAND, handler)) {
                ((hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized) tile)
                    .markForUpdateSync();
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
            if (CompatMods.isFluxCompatLoaded()) {
                FluxNetworkCompat.applyNetwork(
                    player, world, msg.pos, msg.networkID);
            }
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

            if (te instanceof TileFilteredItemOutputHatch
                && msg.fieldId == FIELD_FILTERED_ITEM_OUTPUT) {
                if (!(player.openContainer
                    instanceof ContainerFilteredItemOutputHatch)
                    || ((ContainerFilteredItemOutputHatch) player.openContainer)
                        .getTile() != te) {
                    return;
                }
                ItemStack filter = nbt.getKeySet().isEmpty()
                    ? ItemStack.EMPTY : new ItemStack(nbt);
                if (!filter.isEmpty()) filter.setCount(1);
                ((TileFilteredItemOutputHatch) te).setFilter(filter);
                net.minecraft.block.state.IBlockState state =
                    world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                player.openContainer.detectAndSendChanges();
                return;
            }

            if (te instanceof TileFilteredFluidOutputHatch
                && msg.fieldId == FIELD_FILTERED_FLUID_OUTPUT) {
                if (!(player.openContainer
                    instanceof ContainerFilteredFluidOutputHatch)
                    || ((ContainerFilteredFluidOutputHatch) player.openContainer)
                        .getTile() != te) {
                    return;
                }
                FluidStack filter = nbt.getKeySet().isEmpty() ? null
                    : FluidStack.loadFluidStackFromNBT(nbt);
                if (filter != null) filter.amount = 1;
                ((TileFilteredFluidOutputHatch) te).setFilter(filter);
                net.minecraft.block.state.IBlockState state =
                    world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                player.openContainer.detectAndSendChanges();
                return;
            }

            if (te instanceof TileBatchHatch
                    && msg.fieldId == FIELD_BATCH_MAX_TIME) {
                TileBatchHatch hatch = (TileBatchHatch) te;
                hatch.setMaxBatchTime(nbt.getInteger("v"));
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                return;
            }

            if (te instanceof TileMEOreDictInputBus
                    && player.openContainer instanceof ContainerMEItemInputBus
                    && ((ContainerMEItemInputBus) player.openContainer)
                        .getOwner() == te) {
                TileMEOreDictInputBus bus = (TileMEOreDictInputBus) te;
                if (msg.fieldId == FIELD_ME_ORE_DICT_WHITELIST) {
                    bus.setWhitelist(clampFilter(nbt.getString("v")));
                } else if (msg.fieldId == FIELD_ME_ORE_DICT_BLACKLIST) {
                    bus.setBlacklist(clampFilter(nbt.getString("v")));
                } else if (msg.fieldId == FIELD_ME_ORE_DICT_ACTIVE) {
                    bus.setActivePull(nbt.getBoolean("v"));
                } else if (msg.fieldId == FIELD_ME_INVENTORY_RESERVE) {
                    bus.setPermanentReserve(nbt.getLong("v"));
                } else {
                    return;
                }
                // Flush fake-slot changes to the currently open GUI now. A
                // normal block update replaces the client tile's config
                // inventory object and cannot update SlotFake instances which
                // were bound when the container was opened.
                player.openContainer.detectAndSendChanges();
                return;
            }

            if (te instanceof TileRedstoneInterfaceHatch
                && msg.fieldId == FIELD_REDSTONE_INTERFACE_NAME) {
                if (!(player.openContainer
                    instanceof ContainerRedstoneInterfaceHatch)
                    || ((ContainerRedstoneInterfaceHatch) player.openContainer)
                        .getTile() != te) {
                    return;
                }
                if (((TileRedstoneInterfaceHatch) te)
                    .setSelectedName(nbt.getString("v"))) {
                    net.minecraft.block.state.IBlockState state =
                        world.getBlockState(msg.pos);
                    world.notifyBlockUpdate(msg.pos, state, state, 3);
                    player.openContainer.detectAndSendChanges();
                }
                return;
            }

            if (te instanceof MEInventoryInputBus
                && (msg.fieldId == FIELD_ME_INVENTORY_ACTIVE
                    || msg.fieldId == FIELD_ME_INVENTORY_RESERVE)) {
                if (getContainerOwner(player.openContainer) != te) return;
                MEInventoryInputBus bus = (MEInventoryInputBus) te;
                if (msg.fieldId == FIELD_ME_INVENTORY_ACTIVE) {
                    bus.setActivePull(nbt.getBoolean("v"));
                } else {
                    bus.setPermanentReserve(nbt.getLong("v"));
                }
                player.openContainer.detectAndSendChanges();
                return;
            }

            if (CompatMods.isAeGasCompatLoaded()
                    && AeGasNetworkCompat.applyMarker(
                        player, te, msg.fieldId, nbt)) {
                return;
            }

            if (te instanceof TileDataItemInputHatch
                    && msg.fieldId == FIELD_DATA_INPUT_ASSEMBLY_VALUE) {
                if (!(player.openContainer
                    instanceof ContainerDataItemInputHatch)
                    || ((ContainerDataItemInputHatch) player.openContainer)
                        .getTile() != te) {
                    return;
                }
                TileDataItemInputHatch hatch =
                    (TileDataItemInputHatch) te;
                BlockPos controllerPos = BlockPos.fromLong(
                    nbt.getLong("controllerPos"));
                float value = nbt.getFloat("value");
                if (!Float.isFinite(value)
                    || !hatch.getDataProvider()
                        .setMachineValue(controllerPos, value)) {
                    return;
                }
                net.minecraft.block.state.IBlockState state =
                    world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                return;
            }

            if (CompatMods.isFluxCompatLoaded()
                    && FluxNetworkCompat.applyField(
                        te, msg.fieldId, nbt, world, msg.pos)) {
                return;
            }

            if (CompatMods.isAeEnergyCompatLoaded()
                    && msg.fieldId == FIELD_BUFFER_CAP
                    && AeEnergyNetworkCompat.setBufferCapacity(
                        te, nbt.getLong("v"))) {
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                return;
            }

            if (CompatMods.isAeManaCompatLoaded()
                    && msg.fieldId == FIELD_BUFFER_CAP
                    && AeManaNetworkCompat.setBufferCapacity(
                        te, nbt.getLong("v"))) {
                net.minecraft.block.state.IBlockState state = world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
            }
        }

        private static String clampName(String s) {
            if (s == null) return "";
            return s.length() > 24 ? s.substring(0, 24) : s;
        }

        private static String clampFilter(String s) {
            if (s == null) return "";
            return s.length() > 256 ? s.substring(0, 256) : s;
        }

        private static Object getContainerOwner(Container container) {
            if (container == null) return null;
            try {
                return container.getClass().getMethod("getOwner")
                    .invoke(container);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
    }
}
