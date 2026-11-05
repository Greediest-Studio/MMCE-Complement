package net.edwin.mmcecomplement.verification;

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.core.definitions.AEBlocks;
import ae2.me.helpers.IGridConnectedTile;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.tile.MEPatternProvider.WorkModeSetting;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import net.edwin.mmcecomplement.compat.ae.tile.IMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProvider;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEDataPatternProviderII;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Exercises end slots and the addon's work modes through native provider services. */
final class ProviderModeChecks {
    static volatile boolean completed;
    static final BlockPos DATA_I = new BlockPos(0, 80, 12), DATA_II = new BlockPos(4, 80, 12),
        ORE = new BlockPos(8, 80, 12);
    private final WorldServer world;
    private final List<String> checks;
    private TileEntity power;
    private MEPatternProvider first, second, expanded;
    private int stage;

    ProviderModeChecks(WorldServer world, List<String> checks) { this.world = world; this.checks = checks; }

    boolean tick() {
        if (stage == 0) {
            BlockPos powerPos = new BlockPos(12, 80, 12), expandedPos = new BlockPos(12, 80, 16);
            for (BlockPos pos : Arrays.asList(DATA_I, DATA_II, ORE, powerPos, expandedPos)) world.getChunk(pos);
            world.setBlockState(DATA_I, ModBlocks.ME_DATA_PATTERN_PROVIDER.getDefaultState(), 3);
            world.setBlockState(DATA_II, ModBlocks.ME_DATA_PATTERN_PROVIDER_II.getDefaultState(), 3);
            world.setBlockState(ORE, ModBlocks.ME_ORE_DICT_INPUT_BUS.getDefaultState(), 3);
            world.setBlockState(powerPos, AEBlocks.CREATIVE_ENERGY_CELL.block().getDefaultState(), 3);
            world.setBlockState(expandedPos, ModBlocks.ME_PATTERN_PROVIDER_II.getDefaultState(), 3);
            power = world.getTileEntity(powerPos);
            first = (MEPatternProvider) world.getTileEntity(DATA_I);
            second = (MEPatternProvider) world.getTileEntity(DATA_II);
            expanded = (MEPatternProvider) world.getTileEntity(expandedPos);
            require(first instanceof TileMEDataPatternProvider && second instanceof TileMEDataPatternProviderII,
                "Data provider blocks created the wrong tile types");
            stage = 1;
        } else if (stage == 1) {
            for (Object tile : Arrays.asList(power, first, second, expanded)) if (!node(tile).isReady()) return false;
            for (Object tile : Arrays.asList(first, second, expanded)) GridHelper.createConnection(node(power).getNode(), node(tile).getNode());
            stage = 2;
        } else if (stage == 2) {
            for (Object tile : Arrays.asList(first, second, expanded)) if (!node(tile).isActive()) return false;
            for (MEPatternProvider provider : Arrays.asList(first, second, expanded)) verifyEndSlot(provider);
            verifyModes(expanded);
            checks.add("native-provider-final-slots-35-and-143-data-markers-and-nbt");
            checks.add("native-provider-merged-inputs-blocking-enhanced-blocking-lock-and-isolation");
            completed = true;
            return true;
        }
        return false;
    }

    private void verifyEndSlot(MEPatternProvider provider) {
        int slot = provider.getPatterns().getSlots() - 1;
        ItemStack encoded = pattern(Items.DIAMOND);
        provider.getPatterns().setStackInSlot(slot, encoded);
        ICraftingProvider service = service(provider);
        IPatternDetails details = service.getAvailablePatterns().stream()
            .filter(p -> p.getDefinition().matches(encoded)).findFirst().orElseThrow(() ->
                new IllegalStateException("Native provider omitted slot " + slot));
        if (provider instanceof TileMEDataPatternProvider) {
            require(((TileMEDataPatternProvider) provider).configurePattern(slot, "", 17.5F), "Tier I marker rejected");
        } else if (provider instanceof TileMEDataPatternProviderII) {
            require(((TileMEDataPatternProviderII) provider).configurePattern(slot, "", 17.5F), "Tier II marker rejected");
        }
        require(service.pushPattern(details, inputs(details, 3), 3), "Native merged end-slot pattern rejected");
        require(itemCount(provider.getInfHandler()) == 6 && fluidCount(provider.getInfHandler()) == 750,
            "Merged input quantities were changed");
        if (provider instanceof IMEDataPatternProvider) {
            IMEDataPatternProvider data = (IMEDataPatternProvider) provider;
            require(data.getPatternValue(slot) == 17.5F && data.isDataProviderActiveForGroup(provider.getGroupId()),
                "Native push lost its marker or active data group");
            net.minecraft.nbt.NBTTagCompound saved = provider.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
            MEPatternProvider copy = provider instanceof TileMEDataPatternProviderII
                ? new TileMEDataPatternProviderII() : new TileMEDataPatternProvider();
            copy.setWorld(world); copy.setPos(provider.getPos()); copy.readFromNBT(saved);
            require(((IMEDataPatternProvider) copy).getPatternValue(slot) == 17.5F
                && !copy.getPatterns().getStackInSlot(slot).isEmpty(), "Data marker or final pattern slot lost in NBT");
        }
        clear(provider.getInfHandler());
        provider.returnItemsScheduled();
        provider.setWorkMode(WorkModeSetting.DEFAULT);
    }

    private void verifyModes(MEPatternProvider provider) {
        ItemStack other = pattern(Items.EMERALD);
        provider.getPatterns().setStackInSlot(0, other);
        List<? extends IPatternDetails> patterns = service(provider).getAvailablePatterns();
        IPatternDetails first = patterns.stream().filter(p -> p.getDefinition().matches(other)).findFirst().get();
        IPatternDetails last = patterns.stream().filter(p -> !p.getDefinition().matches(other)).findFirst().get();
        ICraftingProvider service = service(provider);
        provider.setWorkMode(WorkModeSetting.BLOCKING_MODE);
        require(service.pushPattern(first, inputs(first, 1), 1) && service.isBusy(), "Blocking mode did not become busy");
        KeyCounter[] rejected = inputs(first, 1);
        require(!service.pushPattern(first, rejected, 1) && !rejected[0].isEmpty(), "Rejected push consumed inputs");
        clear(provider.getInfHandler());
        provider.setWorkMode(WorkModeSetting.ENHANCED_BLOCKING_MODE);
        require(service.pushPattern(first, inputs(first, 1), 1), "Enhanced blocking rejected initial pattern");
        require(!service.pushPattern(last, inputs(last, 1), 1), "Enhanced blocking mixed different patterns");
        require(service.pushPattern(first, inputs(first, 2), 2) && itemCount(provider.getInfHandler()) == 6,
            "Enhanced blocking rejected matching merged inputs");
        clear(provider.getInfHandler());
        provider.setWorkMode(WorkModeSetting.CRAFTING_LOCK_MODE);
        require(!service.canMergePatternPush(first) && service.pushPattern(first, inputs(first, 1), 1)
            && service.isBusy() && !service.pushPattern(first, inputs(first, 1), 1), "Crafting lock was bypassed");
        clear(provider.getInfHandler());
        provider.setWorkMode(WorkModeSetting.ISOLATION_INPUT);
        require(service.pushPattern(last, inputs(last, 1), 1), "Isolated final-slot push rejected");
        InfItemFluidHandler isolated = (InfItemFluidHandler) provider.getCombinationComponents().get(143).getContainerProvider();
        require(itemCount(isolated) == 2 && fluidCount(isolated) == 250 && provider.getInfHandler().isEmpty(),
            "Isolation routed the final slot into the shared input buffer");
        clear(isolated);
        provider.setWorkMode(WorkModeSetting.DEFAULT);
    }

    private static ItemStack pattern(net.minecraft.item.Item output) {
        return PatternDetailsHelper.encodeProcessingPattern(Arrays.asList(new GenericStack(AEItemKey.of(Items.APPLE), 2),
            new GenericStack(AEFluidKey.of(FluidRegistry.WATER), 250)),
            Collections.singletonList(new GenericStack(AEItemKey.of(output), 1)));
    }
    private static KeyCounter[] inputs(IPatternDetails details, int multiplier) {
        KeyCounter[] result = new KeyCounter[details.getInputs().length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new KeyCounter();
            GenericStack stack = details.getInputs()[i].possibleInputs()[0];
            result[i].add(stack.what(), stack.amount() * details.getInputs()[i].getMultiplier() * multiplier);
        }
        return result;
    }
    private static int itemCount(InfItemFluidHandler handler) {
        return handler.getItemStackList().stream().filter(s -> s.getItem() == Items.APPLE).mapToInt(ItemStack::getCount).sum();
    }
    private static int fluidCount(InfItemFluidHandler handler) {
        return handler.getFluidStackList().stream().filter(s -> s != null && s.getFluid() == FluidRegistry.WATER).mapToInt(s -> s.amount).sum();
    }
    private static void clear(InfItemFluidHandler handler) { handler.getItemStackList().clear(); handler.getFluidStackList().clear(); }
    private static IManagedGridNode node(Object tile) { return ((IGridConnectedTile) tile).getMainNode(); }
    private static ICraftingProvider service(MEPatternProvider tile) { return node(tile).getNode().getService(ICraftingProvider.class); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
