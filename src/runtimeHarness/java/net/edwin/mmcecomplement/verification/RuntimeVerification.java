package net.edwin.mmcecomplement.verification;

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.core.definitions.AEBlocks;
import ae2.me.helpers.IGridConnectedTile;
import ae2.tile.networking.TileCreativeEnergyCell;
import appeng.fluids.util.AEFluidStack;
import com.google.gson.GsonBuilder;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.compat.ae2s.TileStoragePort;
import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.variant.StorageFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEChannelInputHatch;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFluidInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.mechannel.MEChannelPathSnapshot;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.ItemStackHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Separate verification mod, excluded from the MMCE Complement installation jar. */
@Mod(modid = "mmce_complement_verification", name = "MMCE Complement AE2S Verification", version = "1",
    dependencies = "required-after:mmce_complement;required-after:ae2;required-after:appliedenergistics2")
public final class RuntimeVerification {
    public static final BlockPos PROVIDER = new BlockPos(4, 80, 0);
    static final BlockPos ITEMS = new BlockPos(0, 80, 4), FLUIDS = new BlockPos(4, 80, 4);
    private static final BlockPos POWER = new BlockPos(0, 80, 0), PORT = new BlockPos(8, 80, 0),
        CHANNEL = new BlockPos(12, 80, 0);
    private final List<String> checks = new ArrayList<>();
    private final Object reservation = new Object();
    private MinecraftServer server;
    private WorldServer world;
    private TileCreativeEnergyCell power;
    private TileMEChannelInputHatch channel;
    private TileMEPatternProviderII provider;
    private TileMEItemInventoryInputBus itemBus;
    private TileMEFluidInventoryInputBus fluidBus;
    private FixtureStoragePort port;
    private int ticks, phase, phaseTick, catalogCount;
    private boolean finished;
    private ItemStack pattern;
    private PhysicalMachineChecks physical;
    private ProviderModeChecks providerModes;
    private GasBusChecks gasChecks;

    @Mod.EventHandler public void initialize(FMLPreInitializationEvent event) throws Exception {
        GameRegistry.registerTileEntity(FixtureStoragePort.class, new ResourceLocation("mmce_complement_verification", "storage_port"));
        MinecraftForge.EVENT_BUS.register(this);
        if (event.getSide().isClient()) Class.forName("net.edwin.mmcecomplement.verification.ClientVerification")
            .getMethod("install").invoke(null);
    }

    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        server = FMLCommonHandler.instance().getMinecraftServerInstance();
        world = server.getWorld(0);
        world.setSpawnPoint(new BlockPos(4, 80, 4));
        world.getGameRules().setOrCreateGameRule("spawnRadius", "0");
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null || finished) return;
        try {
            ticks++;
            if (Boolean.getBoolean("mmceComplement.verification.gasOnly")) {
                if (gasChecks == null) gasChecks = new GasBusChecks(world, checks);
                if (gasChecks.tick()) finish("PASS", null);
                return;
            }
            if (ticks > 1800) throw new IllegalStateException("Timed out in phase " + phase);
            switch (phase) {
                case 0: if (ticks >= 20) createFixture(); break;
                case 1: connect(); break;
                case 2: verifyStorage(); break;
                case 3: verifyExternalChanges(); break;
                case 4: verifyPulling(); break;
                case 5: verifyReturn(); break;
                case 6: verifyPattern(); break;
                case 7: verifyReservation(); break;
                case 8: verifyRelease(); break;
                case 9: if (physical.tick()) { providerModes = new ProviderModeChecks(world, checks); phase = 10; } break;
                case 10: if (providerModes.tick()) phase = 11; break;
                case 11:
                    if (!net.minecraftforge.fml.common.Loader.isModLoaded("mekeng")) { phase = 12; break; }
                    if (gasChecks == null) gasChecks = new GasBusChecks(world, checks);
                    if (gasChecks.tick()) phase = 12;
                    break;
                case 12: if (FMLCommonHandler.instance().getSide().isServer() || clientInteractionsComplete()) verifyInvalidation(); break;
                default: throw new IllegalStateException("Unexpected phase " + phase);
            }
        } catch (Throwable failure) { failure.printStackTrace(); finish("FAIL", failure.toString()); }
    }

    private void createFixture() {
        world.getChunk(POWER);
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!"mmce_complement".equals(block.getRegistryName().getNamespace()) || !block.hasTileEntity(block.getDefaultState())) continue;
            TileEntity tile = block.createTileEntity(world, block.getDefaultState());
            if (tile == null) continue;
            tile.setWorld(world);
            tile.setPos(new BlockPos(24, 80, catalogCount));
            NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
            tile.readFromNBT(data);
            if (tile instanceof MEMachineComponent) {
                IManagedGridNode node = ((IGridConnectedTile) tile).getMainNode();
                require(node != null, "Official MMCE native node missing: " + tile.getClass().getName());
            }
            catalogCount++;
        }
        checks.add("registered-tile-construction-nbt-and-official-mmce-native-node:" + catalogCount);
        world.setBlockState(POWER, AEBlocks.CREATIVE_ENERGY_CELL.block().getDefaultState(), 3);
        world.setBlockState(PROVIDER, ModBlocks.ME_PATTERN_PROVIDER_II.getDefaultState(), 3);
        world.setBlockState(PORT, ModBlocks.AE_STORAGE_ACCESS_PORT.getDefaultState(), 3);
        world.setBlockState(CHANNEL, ModBlocks.ME_CHANNEL_INPUT_HATCH.getDefaultState(), 3);
        world.setBlockState(ITEMS, ModBlocks.ME_ITEM_INVENTORY_INPUT_BUS.getDefaultState(), 3);
        world.setBlockState(FLUIDS, ModBlocks.ME_FLUID_INVENTORY_INPUT_BUS.getDefaultState(), 3);
        power = (TileCreativeEnergyCell) world.getTileEntity(POWER);
        provider = (TileMEPatternProviderII) world.getTileEntity(PROVIDER);
        channel = (TileMEChannelInputHatch) world.getTileEntity(CHANNEL);
        itemBus = (TileMEItemInventoryInputBus) world.getTileEntity(ITEMS);
        fluidBus = (TileMEFluidInventoryInputBus) world.getTileEntity(FLUIDS);
        world.removeTileEntity(PORT);
        port = new FixtureStoragePort();
        world.setTileEntity(PORT, port);
        port.items.setStackInSlot(0, new ItemStack(Items.APPLE, 32));
        port.fluids.fill(new FluidStack(FluidRegistry.WATER, 2000), true);
        phase = 1;
    }

    private IManagedGridNode node(Object tile) {
        return tile instanceof TileStoragePort ? ((TileStoragePort) tile).bus().getMainNode()
            : ((IGridConnectedTile) tile).getMainNode();
    }

    private void connect() {
        if (ticks == 100) {
            for (Object tile : Arrays.asList(power, provider, port, channel, itemBus, fluidBus)) {
                System.out.println("MMCE Complement verification node " + tile.getClass().getSimpleName()
                    + ": ready=" + node(tile).isReady() + ", invalid=" + ((TileEntity) tile).isInvalid());
            }
            System.out.println("MMCE Complement verification queued nodes: "
                + ae2.hooks.ticking.TickHandler.instance().getBlockEntityReport());
        }
        if (!node(power).isReady() || !node(provider).isReady() || !node(port).isReady()
            || !node(channel).isReady() || !node(itemBus).isReady() || !node(fluidBus).isReady()) return;
        for (Object tile : Arrays.asList(provider, port, channel, itemBus, fluidBus)) {
            GridHelper.createConnection(node(power).getNode(), node(tile).getNode());
        }
        phase = 2;
    }

    private long stored(AEItemKey key) { return node(port).getGrid().getStorageService().getCachedInventory().get(key); }
    private long water() { return node(port).getGrid().getStorageService().getCachedInventory().get(AEFluidKey.of(FluidRegistry.WATER)); }

    private void verifyStorage() {
        if (!node(port).isActive() || !node(provider).isActive() || !node(itemBus).isActive() || !node(fluidBus).isActive()) return;
        AEItemKey apple = AEItemKey.of(Items.APPLE);
        require(stored(apple) == 32 && water() == 2000, "Storage port did not mount both real backing inventories");
        ae2.api.storage.MEStorage storage = node(port).getGrid().getStorageService().getInventory();
        require(storage.insert(apple, 8, Actionable.SIMULATE, IActionSource.empty()) == 8 && stored(apple) == 32,
            "Simulated native insertion changed the inventory");
        require(storage.insert(apple, 8, Actionable.MODULATE, IActionSource.empty()) == 8 && stored(apple) == 40,
            "Insertion did not synchronously publish an exact storage delta");
        require(storage.extract(apple, 7, Actionable.MODULATE, IActionSource.empty()) == 7 && stored(apple) == 33,
            "Extraction delta was lost or counted twice");
        require(storage.extract(AEFluidKey.of(FluidRegistry.WATER), 250, Actionable.MODULATE, IActionSource.empty()) == 250
            && water() == 1750, "Native fluid extraction did not update the terminal cache");
        checks.add("native-item-fluid-storage-simulation-insert-extract-exact-deltas");
        port.items.setStackInSlot(0, new ItemStack(Items.APPLE, 32));
        phase = 3;
    }

    private void verifyExternalChanges() {
        if (stored(AEItemKey.of(Items.APPLE)) != 32) return;
        checks.add("external-storage-edits-publish-to-native-cache");
        itemBus.getConfigInventory().setStackInSlot(0, new ItemStack(Items.APPLE));
        itemBus.setPermanentReserve(5);
        itemBus.setActivePull(true);
        fluidBus.getConfig().setFluidInSlot(0, AEFluidStack.fromFluidStack(new FluidStack(FluidRegistry.WATER, 1)));
        fluidBus.setActivePull(true);
        phase = 4;
    }

    private void verifyPulling() {
        if (itemBus.getInternalInventory().getStackInSlot(0).getCount() != 27
            || fluidBus.getTanks().getFluidInSlot(0) == null || fluidBus.getTanks().getFluidInSlot(0).getStackSize() != 1750) return;
        require(stored(AEItemKey.of(Items.APPLE)) == 5 && water() == 0, "Input buses ignored reserve or native fluid storage");
        checks.add("real-native-grid-scheduler-item-fluid-input-and-permanent-reserve");
        itemBus.setActivePull(false);
        fluidBus.setActivePull(false);
        phase = 5;
    }

    private void verifyReturn() {
        if (stored(AEItemKey.of(Items.APPLE)) != 32 || water() != 1750) return;
        checks.add("item-fluid-buffers-return-to-the-same-network");
        pattern = PatternDetailsHelper.encodeProcessingPattern(Arrays.asList(
            new GenericStack(AEItemKey.of(Items.APPLE), 2), new GenericStack(AEFluidKey.of(FluidRegistry.WATER), 250)),
            Collections.singletonList(new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1)));
        provider.getPatterns().setStackInSlot(40, pattern);
        phase = 6;
    }

    private void verifyPattern() {
        List<IPatternDetails> available = new ArrayList<>();
        for (IPatternDetails candidate : node(provider).getGrid().getCraftingService().getCraftingFor(AEItemKey.of(Items.GOLD_INGOT))) {
            if (candidate.getDefinition().equals(AEItemKey.of(pattern))) available.add(candidate);
        }
        if (available.isEmpty()) return;
        IPatternDetails detail = available.get(0);
        KeyCounter[] inputs = new KeyCounter[detail.getInputs().length];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new KeyCounter();
            GenericStack option = detail.getInputs()[i].possibleInputs()[0];
            inputs[i].add(option.what(), option.amount() * detail.getInputs()[i].getMultiplier());
        }
        ICraftingProvider nativeProvider = node(provider).getNode().getService(ICraftingProvider.class);
        require(nativeProvider.pushPattern(detail, inputs, 1), "AE2S rejected the extended-slot processing pattern");
        int apples = 0, fluid = 0;
        for (ItemStack stack : provider.getInfHandler().getItemStackList()) if (stack.getItem() == Items.APPLE) apples += stack.getCount();
        for (FluidStack stack : provider.getInfHandler().getFluidStackList()) if (stack != null && stack.getFluid() == FluidRegistry.WATER) fluid += stack.amount;
        require(apples == 2 && fluid == 250, "Mixed processing inputs were not delivered as real items and fluid");
        for (KeyCounter input : inputs) require(input.isEmpty(), "A successful pattern push did not consume its allocated inputs");
        checks.add("ae2s-native-pattern-index-extended-slot-40-and-mixed-item-fluid-push");
        require(channel.snapshotMEChannelPath().getState() == MEChannelPathSnapshot.State.READY, "Native channel path unavailable");
        require(channel.requestMEChannels(reservation, 2), "Channel reservation rejected");
        phase = 7;
    }

    private void verifyReservation() {
        if (!channel.isMEChannelReservationSatisfied(reservation, 2)) return;
        require(channel.getMEChannelNetworkIdentity() == node(channel).getGrid(), "Channel network identity is a transient legacy wrapper");
        checks.add("real-ae2s-channel-path-and-two-channel-reservation");
        channel.releaseMEChannels(reservation);
        phaseTick = ticks;
        phase = 8;
    }

    private void verifyRelease() {
        if (ticks < phaseTick + 80 || node(channel).getGrid().getPathingService().isNetworkBooting()) return;
        require(channel.getRequestedChannels() == 0 && !channel.isMEChannelReservationSatisfied(reservation, 2),
            "Released channels remained reserved");
        require(node(channel).getGrid().getPathingService().getUsedChannels() == 4, "Private channel endpoints leaked after release");
        checks.add("channel-release-destroys-private-endpoints");
        physical = new PhysicalMachineChecks(world, checks);
        phase = 9;
    }

    private void verifyInvalidation() {
        provider.invalidate();
        require(node(provider).getNode() == null, "Invalidated provider retained a live native node");
        checks.add("native-node-invalidation");
        finish("PASS", null);
    }

    private boolean clientInteractionsComplete() throws Exception {
        return (Boolean) Class.forName("net.edwin.mmcecomplement.verification.ClientVerification")
            .getMethod("interactionsComplete").invoke(null);
    }

    private void finish(String result, String error) {
        finished = true;
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("result", result); report.put("cleanroom", System.getProperty("mmceComplement.verification.cleanroom", "unknown")); report.put("ticks", ticks);
            report.put("checks", checks); if (error != null) report.put("error", error);
            Files.write(Paths.get("verification-report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
            if (FMLCommonHandler.instance().getSide().isClient()) {
                Class.forName("net.edwin.mmcecomplement.verification.ClientVerification").getMethod("finish").invoke(null);
            } else server.initiateShutdown();
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    /** Uses the production port/monitor adapter with deterministic backing containers. */
    public static final class FixtureStoragePort extends TileStoragePort implements StorageController {
        private long version;
        final ItemStackHandler items = new ItemStackHandler(2) {
            @Override protected void onContentsChanged(int slot) { mmceComplement$markStorageChanged(); }
        };
        final StorageFluidHandler fluids = new StorageFluidHandler(this, 1, 16000);
        @Override protected StorageController storage() { return this; }
        @Override public boolean mmceComplement$isStorageController() { return true; }
        @Override public int mmceComplement$getStorageSlots() { return 2; }
        @Override public ItemStackHandler mmceComplement$getStorageInventory() { return items; }
        @Override public boolean mmceComplement$hasFluidStorage() { return true; }
        @Override public int mmceComplement$getFluidStorageSlots() { return 1; }
        @Override public int mmceComplement$getFluidStorageCapacity() { return 16000; }
        @Override public IFluidHandler mmceComplement$getFluidStorage() { return fluids; }
        @Override public IFluidHandler mmceComplement$getFluidTank(int slot) { return fluids.getTankInteractionHandler(slot); }
        @Override public boolean mmceComplement$hasGasStorage() { return false; }
        @Override public int mmceComplement$getGasStorageSlots() { return 0; }
        @Override public int mmceComplement$getGasStorageCapacity() { return 0; }
        @Override public Object mmceComplement$getGasStorage() { return null; }
        @Override public long mmceComplement$getStorageChangeVersion() { return version; }
        @Override public void mmceComplement$markStorageChanged() { version++; }
    }
}
