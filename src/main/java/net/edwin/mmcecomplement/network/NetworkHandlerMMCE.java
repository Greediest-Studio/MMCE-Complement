package net.edwin.mmcecomplement.network;

import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae.AeEnergyNetworkCompat;
import net.edwin.mmcecomplement.compat.ae.AeGasNetworkCompat;
import net.edwin.mmcecomplement.compat.ae.AeManaNetworkCompat;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII;
import github.kasuminova.mmce.common.container.ContainerMEItemInputBus;
import github.kasuminova.mmce.common.container.ContainerMEPatternProvider;
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
import net.edwin.mmcecomplement.gui.ContainerStorage;
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
import net.edwin.mmcecomplement.compat.mekanism.StorageGasBridge;

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
        CHANNEL.registerMessage(StoragePageHandler.class,
                StoragePageMessage.class, 3, Side.SERVER);
        CHANNEL.registerMessage(StorageSortHandler.class,
                StorageSortMessage.class, 4, Side.SERVER);
        CHANNEL.registerMessage(StorageTabHandler.class, StorageTabMessage.class, 5, Side.SERVER);
        CHANNEL.registerMessage(StorageTankHandler.class, StorageTankMessage.class, 6, Side.SERVER);
    }

    public static final class StorageTabMessage implements IMessage {
        public int mode; public StorageTabMessage() {} public StorageTabMessage(int mode){this.mode=mode;}
        @Override public void fromBytes(ByteBuf b){mode=b.readByte();} @Override public void toBytes(ByteBuf b){b.writeByte(mode);}
    }
    public static final class StorageTabHandler implements IMessageHandler<StorageTabMessage,IMessage> {
        @Override public IMessage onMessage(StorageTabMessage m, MessageContext c){ EntityPlayerMP p=c.getServerHandler().player; if(p!=null)p.getServerWorld().addScheduledTask(()->{if(p.openContainer instanceof ContainerStorage){ContainerStorage x=(ContainerStorage)p.openContainer;if(x.canInteractWith(p))x.setMode(m.mode);}}); return null; }
    }
    public static final class StorageTankMessage implements IMessage {
        public int tank; public StorageTankMessage(){} public StorageTankMessage(int tank){this.tank=tank;}
        @Override public void fromBytes(ByteBuf b){tank=b.readInt();} @Override public void toBytes(ByteBuf b){b.writeInt(tank);}
    }
    public static final class StorageTankHandler implements IMessageHandler<StorageTankMessage,IMessage> {
        @Override public IMessage onMessage(StorageTankMessage m, MessageContext c){ EntityPlayerMP p=c.getServerHandler().player; if(p!=null)p.getServerWorld().addScheduledTask(()->apply(p,m.tank)); return null; }
        private static void apply(EntityPlayerMP player, int tank) {
            if (!(player.openContainer instanceof ContainerStorage)) return;
            ContainerStorage container = (ContainerStorage) player.openContainer;
            if (!container.canInteractWith(player) || tank < 0 || tank >= container.getTankCount()) return;
            boolean changed = false;
            if (container.getMode() == ContainerStorage.MODE_FLUIDS) {
                net.minecraftforge.fluids.FluidActionResult result =
                    net.edwin.mmcecomplement.variant.StorageFluidInteraction.interact(
                        player.inventory.getItemStack(), container.getFluidTank(tank),
                        new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.inventory), player);
                if (result.isSuccess()) {
                    player.inventory.setItemStack(result.getResult());
                    changed = true;
                }
            } else if (container.getMode() == ContainerStorage.MODE_GASES) {
                changed = StorageGasBridge.interact(container.getGasStorage(), player, tank);
            }
            if (changed) {
                player.inventory.markDirty();
                player.inventoryContainer.detectAndSendChanges();
                container.detectAndSendChanges();
                // A custom GUI packet bypasses vanilla's click acknowledgement.
                player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(
                    -1, -1, player.inventory.getItemStack()));
            }
        }
    }

    public static final class StoragePageMessage implements IMessage {
        public int page;
        public StoragePageMessage() {}
        public StoragePageMessage(int page) { this.page = page; }
        @Override public void fromBytes(ByteBuf buf) { page = buf.readInt(); }
        @Override public void toBytes(ByteBuf buf) { buf.writeInt(page); }
    }

    public static final class StoragePageHandler
        implements IMessageHandler<StoragePageMessage, IMessage> {
        @Override public IMessage onMessage(StoragePageMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player != null) player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerStorage) {
                    ContainerStorage container = (ContainerStorage) player.openContainer;
                    if (msg.page >= 0 && msg.page < container.getPageCount()
                        && container.canInteractWith(player)) {
                        container.setPage(msg.page);
                    }
                }
            });
            return null;
        }
    }

    /** Middle mouse action used by Inventory Sorter and supported natively by
     * the storage variant.  The packet carries no client supplied inventory
     * data; the server sorts the controller's real handler. */
    public static final class StorageSortMessage implements IMessage {
        @Override public void fromBytes(ByteBuf buf) {}
        @Override public void toBytes(ByteBuf buf) {}
    }

    public static final class StorageSortHandler
        implements IMessageHandler<StorageSortMessage, IMessage> {
        @Override public IMessage onMessage(StorageSortMessage msg,
                                            MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player != null) player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerStorage) {
                    ContainerStorage container =
                        (ContainerStorage) player.openContainer;
                    if (container.canInteractWith(player)) {
                        container.sortStorage();
                    }
                }
            });
            return null;
        }
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
    public static final int FIELD_ME_DATA_PATTERN = 19;
    public static final int FIELD_ME_DATA_PATTERN_II = 20;

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

            if (te instanceof TileMEDataPatternProvider
                    && msg.fieldId == FIELD_ME_DATA_PATTERN) {
                if (!(player.openContainer instanceof ContainerMEPatternProvider)
                    || ((ContainerMEPatternProvider) player.openContainer)
                        .getOwner() != te) {
                    return;
                }
                TileMEDataPatternProvider provider =
                    (TileMEDataPatternProvider) te;
                if (!provider.configurePattern(
                    nbt.getInteger("slot"), nbt.getString("type"),
                    nbt.getFloat("value"))) {
                    return;
                }
                net.minecraft.block.state.IBlockState state =
                    world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                player.openContainer.detectAndSendChanges();
                return;
            }

            if (te instanceof TileMEDataPatternProviderII
                    && msg.fieldId == FIELD_ME_DATA_PATTERN_II) {
                if (!(player.openContainer instanceof ContainerMEPatternProvider)
                    || ((ContainerMEPatternProvider) player.openContainer)
                        .getOwner() != te) {
                    return;
                }
                TileMEDataPatternProviderII provider =
                    (TileMEDataPatternProviderII) te;
                if (!provider.configurePattern(
                    nbt.getInteger("slot"), nbt.getString("type"),
                    nbt.getFloat("value"))) {
                    return;
                }
                net.minecraft.block.state.IBlockState state =
                    world.getBlockState(msg.pos);
                world.notifyBlockUpdate(msg.pos, state, state, 3);
                player.openContainer.detectAndSendChanges();
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
