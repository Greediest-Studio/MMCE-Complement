package net.edwin.mmcecomplement.verification;

import ae2.api.config.Actionable;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.MEStorage;
import ae2.core.definitions.AEBlocks;
import ae2.core.definitions.AEItems;
import ae2.me.helpers.IGridConnectedTile;
import ae2.tile.storage.TileDrive;
import github.kasuminova.mmce.common.tile.MEGasInputBus;
import github.kasuminova.mmce.common.tile.MEGasOutputBus;
import me.ramidzkh.mekae2.ae2.AEGasKey;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.ae.tile.*;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import java.util.Arrays;
import java.util.List;

/** Optional installed MekEng checks; this class is loaded only when MekEng is present. */
final class GasBusChecks {
    static final BlockPos INPUT = new BlockPos(8, 80, 24), INVENTORY = new BlockPos(12, 80, 24);
    static final BlockPos MIXED_INPUT = new BlockPos(0, 80, 28), MIXED_OUTPUT = new BlockPos(4, 80, 28);
    static final BlockPos MIXED_INVENTORY = new BlockPos(0, 80, 32), FULL_EXPOSURE = new BlockPos(4, 80, 32);
    private final WorldServer world;
    private final List<String> checks;
    private final MEGasInputBus input;
    private final MEGasOutputBus output;
    private final TileMEGasInventoryInputBus inventory;
    private final TileMEInputAssembly mixedInput;
    private final TileMEOutputAssembly mixedOutput;
    private final TileMEInventoryInputAssembly mixedInventory;
    private final TileMEFullExposureAssembly fullExposure;
    private final TileDrive drive;
    private final TileEntity power;
    private final Gas hydrogen = GasRegistry.getGas("hydrogen"), oxygen = GasRegistry.getGas("oxygen");
    private int phase, ticks, since, capacity;
    private long hydrogenAfterConsumption;
    private final List<Object> nodes;
    static volatile boolean completed;

    GasBusChecks(WorldServer world, List<String> checks) {
        this.world = world; this.checks = checks;
        require(hydrogen != null && oxygen != null, "Mekanism gases missing");
        power = place(new BlockPos(0, 80, 24), AEBlocks.CREATIVE_ENERGY_CELL.block());
        drive = (TileDrive) place(new BlockPos(4, 80, 24), AEBlocks.DRIVE.block());
        input = (MEGasInputBus) place(INPUT, block("modularmachinery", "blockmegasinputbus"));
        output = (MEGasOutputBus) place(new BlockPos(8, 80, 28), block("modularmachinery", "blockmegasoutputbus"));
        inventory = (TileMEGasInventoryInputBus) place(INVENTORY, ModBlocks.ME_GAS_INVENTORY_INPUT_BUS);
        mixedInput = (TileMEInputAssembly) place(MIXED_INPUT, ModBlocks.ME_INPUT_ASSEMBLY);
        mixedOutput = (TileMEOutputAssembly) place(MIXED_OUTPUT, ModBlocks.ME_OUTPUT_ASSEMBLY);
        mixedInventory = (TileMEInventoryInputAssembly) place(MIXED_INVENTORY, ModBlocks.ME_INVENTORY_INPUT_ASSEMBLY);
        fullExposure = (TileMEFullExposureAssembly) place(FULL_EXPOSURE, ModBlocks.ME_FULL_EXPOSURE_ASSEMBLY);
        Item gasCell = ForgeRegistries.ITEMS.getValue(new ResourceLocation("mekeng", "gas_storage_cell_64k"));
        require(gasCell != null, "Official MekEng gas cell missing");
        drive.getInternalInventory().setItemDirect(0, new ItemStack(gasCell));
        drive.getInternalInventory().setItemDirect(1, AEItems.ITEM_CELL_64K.stack());
        drive.getInternalInventory().setItemDirect(2, AEItems.FLUID_CELL_64K.stack());
        nodes = Arrays.asList(power, drive, input, output, inventory, mixedInput, mixedOutput, mixedInventory, fullExposure);
    }

    boolean tick() {
        ticks++;
        require(ticks - since <= 260, "Gas verification timed out in phase " + phase + ", H=" + stored(hydrogen)
            + ", input=" + amount(input.getTanks().getGasStack(0)) + ", inventory=" + amount(inventory.getTanks().getGasStack(0)));
        switch (phase) {
            case 0:
                for (Object tile : nodes) if (!node(tile).isReady()) return false;
                for (Object tile : nodes) if (tile != power) GridHelper.createConnection(node(power).getNode(), node(tile).getNode());
                checks.add("original-empty-mmce-gas-bus-native-node-ready-with-nine-tank-nbt");
                next(); break;
            case 1:
                for (Object tile : nodes) if (!node(tile).isActive()) return false;
                mixedInput.getInternalInventory().setStackInSlot(15, new ItemStack(Items.APPLE, 3));
                mixedOutput.getInternalInventory().setStackInSlot(15, new ItemStack(Items.APPLE, 4));
                require(mixedInput.getDisplayInventory().getStackInSlot(15).getCount() == 3, "Input assembly listener lost callback");
                require(mixedOutput.getDisplayInventory().getStackInSlot(15).getCount() == 4, "Output assembly listener lost callback");
                mixedInput.getInternalInventory().setStackInSlot(15, ItemStack.EMPTY);
                mixedOutput.getInternalInventory().setStackInSlot(15, ItemStack.EMPTY);
                checks.add("pr1-input-output-assembly-listeners-refresh-slot-immediately");
                require(network().insert(AEGasKey.of(hydrogen), 20000, Actionable.MODULATE, IActionSource.empty()) == 20000, "Gas cell did not mount");
                network().insert(AEGasKey.of(oxygen), 5000, Actionable.MODULATE, IActionSource.empty());
                network().insert(AEItemKey.of(Items.APPLE), 20, Actionable.MODULATE, IActionSource.empty());
                network().insert(AEFluidKey.of(FluidRegistry.WATER), 2000, Actionable.MODULATE, IActionSource.empty());
                capacity = input.getTanks().getTanks()[0].getMaxGas();
                input.getTanks().setGas(0, new GasStack(oxygen, 250));
                NBTTagCompound save = input.writeToNBT(new NBTTagCompound());
                input.readFromNBT(save);
                require(amount(input.getTanks().getGasStack(0)) == 250 && input.getConfig().size() == 9, "Gas NBT was changed or lost");
                input.getConfig().setGas(0, new GasStack(hydrogen, 1));
                output.getTanks().setGas(0, new GasStack(oxygen, 350));
                mixedInput.setMarker(0, new ItemStack(Items.APPLE, 2));
                mixedInput.setMarker(1, MixedMEInputMarker.fluid(new FluidStack(FluidRegistry.WATER, 100)));
                mixedInput.setMarker(15, MixedMEInputMarker.gas(new GasStack(hydrogen, 200)));
                mixedOutput.getInternalInventory().setStackInSlot(0, new ItemStack(Items.APPLE, 3));
                ((IFluidHandler) mixedOutput.getFluidTanks()).fill(new FluidStack(FluidRegistry.WATER, 250), true);
                mixedOutput.getGasTanks().setGas(0, new GasStack(oxygen, 100));
                next(); break;
            case 2:
                if (amount(input.getTanks().getGasStack(0)) != capacity
                    || amount(output.getTanks().getGasStack(0)) != 0
                    || amount(mixedInput.getGasTanks().getGasStack(15)) != 200
                    || mixedInput.getFluidTanks().getFluidInSlot(1) == null
                    || mixedInput.getInternalInventory().getStackInSlot(0).getCount() != 2) return false;
                require(stored(hydrogen) + capacity + 200 == 20000, "Native gas pull duplicated or lost gas");
                require(stored(oxygen) == 5700, "Original and mixed output did not return exact gas amounts");
                require(network().getAvailableStacks().get(AEFluidKey.of(FluidRegistry.WATER)) == 2150, "Mixed fluid transfer changed total");
                require(network().getAvailableStacks().get(AEItemKey.of(Items.APPLE)) == 21, "Mixed item transfer changed total");
                checks.add("real-native-gas-input-output-and-mixed-item-fluid-gas-scheduler-conserves-resources");
                input.getTanks().removeGas(0, 500, false);
                mixedInput.getGasTanks().removeGas(15, 100, false);
                hydrogenAfterConsumption = 19400;
                next(); break;
            case 3:
                if (amount(input.getTanks().getGasStack(0)) != capacity || amount(mixedInput.getGasTanks().getGasStack(15)) != 200) return false;
                require(stored(hydrogen) + capacity + 200 == hydrogenAfterConsumption, "Recipe consumption/refill lost gas");
                input.getConfig().setGas(0, null);
                mixedInput.setMarker(15, ItemStack.EMPTY);
                next(); break;
            case 4:
                if (stored(hydrogen) != hydrogenAfterConsumption || amount(input.getTanks().getGasStack(0)) != 0
                    || amount(mixedInput.getGasTanks().getGasStack(15)) != 0) return false;
                checks.add("gas-recipe-consumption-refill-and-cleared-config-returns-without-loss");
                inventory.setPermanentReserve(1000);
                inventory.getConfig().setGas(0, new GasStack(hydrogen, 1));
                inventory.setActivePull(true);
                next(); break;
            case 5:
                if (amount(inventory.getTanks().getGasStack(0)) != hydrogenAfterConsumption - 1000) return false;
                require(stored(hydrogen) == 1000, "Gas permanent reserve was ignored");
                NBTTagCompound saved = inventory.writeToNBT(new NBTTagCompound());
                inventory.readFromNBT(saved);
                require(inventory.isActivePull() && inventory.getPermanentReserve() == 1000
                    && amount(inventory.getTanks().getGasStack(0)) == hydrogenAfterConsumption - 1000,
                    "Inventory gas NBT lost buffer/settings: active=" + inventory.isActivePull()
                        + ", reserve=" + inventory.getPermanentReserve() + ", buffer=" + amount(inventory.getTanks().getGasStack(0)));
                inventory.getInventoryByName("upgrades").setStackInSlot(0, AEItems.CAPACITY_CARD.stack());
                inventory.getInventoryByName("upgrades").setStackInSlot(0, ItemStack.EMPTY);
                require(amount(inventory.getTanks().getGasStack(0)) == hydrogenAfterConsumption - 1000,
                    "Changing a gas inventory upgrade truncated its buffer");
                checks.add("inventory-gas-active-pull-permanent-reserve-and-nbt-preserve-buffer");
                inventory.setActivePull(false);
                next(); break;
            case 6:
                if (stored(hydrogen) != hydrogenAfterConsumption || amount(inventory.getTanks().getGasStack(0)) != 0) return false;
                checks.add("inventory-gas-disable-pull-returns-buffer-to-original-network");
                mixedInventory.setPermanentReserve(7);
                mixedInventory.setMarker(0, new ItemStack(Items.APPLE, 99));
                mixedInventory.setMarker(1, MixedMEInputMarker.fluid(new FluidStack(FluidRegistry.WATER, 999)));
                mixedInventory.setMarker(2, MixedMEInputMarker.gas(new GasStack(hydrogen, 999)));
                mixedInventory.setActivePull(true);
                next(); break;
            case 7:
                if (amount(mixedInventory.getGasTanks().getGasStack(2)) != 19393
                    || mixedInventory.getInternalInventory().getStackInSlot(0).getCount() != 14
                    || mixedInventory.getFluidTanks().getFluidInSlot(1) == null
                    || mixedInventory.getFluidTanks().getFluidInSlot(1).getStackSize() != 2143) return false;
                for (int i = 0; i < 3; i++) require(MixedMEInputMarker.getAmount(mixedInventory.getConfigInventory().getStackInSlot(i)) == 1, "Mixed inventory filter has a quantity");
                NBTTagCompound mixedSave = mixedInventory.writeToNBT(new NBTTagCompound());
                mixedInventory.readFromNBT(mixedSave);
                require(amount(mixedInventory.getGasTanks().getGasStack(2)) == 19393
                    && mixedInventory.getFluidTanks().getFluidInSlot(1).getStackSize() == 2143,
                    "Mixed inventory NBT lost large buffers");
                mixedInventory.setActivePull(false);
                checks.add("mixed-inventory-type-only-markers-reserve-large-buffer-nbt-and-native-scheduler");
                next(); break;
            case 8:
                if (stored(hydrogen) != 19400 || network().getAvailableStacks().get(AEItemKey.of(Items.APPLE)) != 21
                    || network().getAvailableStacks().get(AEFluidKey.of(FluidRegistry.WATER)) != 2150) return false;
                fullExposure.setPermanentReserve(7);
                fullExposure.setActivePull(true);
                next(); break;
            case 9:
                if (stored(hydrogen) != 7 || stored(oxygen) != 7
                    || network().getAvailableStacks().get(AEItemKey.of(Items.APPLE)) != 7
                    || network().getAvailableStacks().get(AEFluidKey.of(FluidRegistry.WATER)) != 7) return false;
                long gasTotal = 0, fluidTotal = 0, itemTotal = 0;
                for (int slot = 0; slot < 16; slot++) {
                    gasTotal += amount(fullExposure.getGasTanks().getGasStack(slot));
                    if (fullExposure.getFluidTanks().getFluidInSlot(slot) != null) fluidTotal += fullExposure.getFluidTanks().getFluidInSlot(slot).getStackSize();
                    itemTotal += fullExposure.getInternalInventory().getStackInSlot(slot).getCount();
                }
                require(gasTotal == 25086 && fluidTotal == 2143 && itemTotal == 14, "Full exposure mixed buffers lost resources");
                fullExposure.setActivePull(false);
                next(); break;
            case 10:
                if (stored(hydrogen) != 19400 || stored(oxygen) != 5700
                    || network().getAvailableStacks().get(AEFluidKey.of(FluidRegistry.WATER)) != 2150
                    || network().getAvailableStacks().get(AEItemKey.of(Items.APPLE)) != 21) return false;
                checks.add("unfiltered-full-exposure-mixed-pull-per-resource-reserve-and-standby-exact-return");
                completed = true;
                return true;
            default: throw new IllegalStateException("Unexpected gas phase " + phase);
        }
        return false;
    }
    private void next() { phase++; since = ticks; }
    private static IManagedGridNode node(Object tile) { return ((IGridConnectedTile) tile).getMainNode(); }
    private MEStorage network() { return node(drive).getGrid().getStorageService().getInventory(); }
    private long stored(Gas gas) { return node(drive).getGrid() == null ? -1 : network().getAvailableStacks().get(AEGasKey.of(gas)); }
    private static int amount(GasStack gas) { return gas == null ? 0 : gas.amount; }
    private TileEntity place(BlockPos pos, Block block) {
        require(block != null, "Fixture block missing"); world.setBlockState(pos, block.getDefaultState(), 3); return world.getTileEntity(pos);
    }
    private static Block block(String namespace, String name) { return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, name)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
