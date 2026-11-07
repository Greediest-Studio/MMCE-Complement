package net.edwin.mmcecomplement.verification;

import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridConnection;
import ae2.api.networking.IManagedGridNode;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.core.definitions.AEBlocks;
import ae2.me.helpers.IGridConnectedTile;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import net.edwin.mmcecomplement.compat.ae.tile.MEConnectionShareManager;
import net.edwin.mmcecomplement.compat.ae2s.TileStoragePort;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEConnectionShareHatch;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Real registered machine definitions, controller matching and production hatches. */
final class PhysicalMachineChecks {
    private final WorldServer world;
    private final List<String> checks;
    private final List<TileEntity> parts = new ArrayList<>();
    private TileMachineController controller;
    private TileStoragePort firstPort, secondPort;
    private TileMEConnectionShareHatch firstShare, secondShare;
    private TileEntity firstTarget, secondTarget, power, otherPower;
    private StorageController storage;
    private BlockPos stone;
    private IGridConnection firstLink, secondLink;
    private int stage, ticks, changedAt;
    private boolean connected, tearingDown;

    PhysicalMachineChecks(WorldServer world, List<String> checks) {
        this.world = world;
        this.checks = checks;
    }

    boolean tick() {
        ticks++;
        if (controller != null) controller.doControllerTick();
        switch (stage) {
            case 0:
                placeMachine("storage", new BlockPos(32, 80, 0));
                firstPort = (TileStoragePort) parts.get(0);
                secondPort = (TileStoragePort) parts.get(1);
                storage = (StorageController) controller;
                power = placePower(controller.getPos().add(-4, 0, 0));
                stage = 1;
                break;
            case 1:
                if (!ready(firstPort, secondPort, power) || !controller.isStructureFormed()) break;
                if (!connected) {
                    firstLink = GridHelper.createConnection(node(power).getNode(), node(firstPort).getNode());
                    GridHelper.createConnection(node(power).getNode(), node(secondPort).getNode());
                    storage.mmceComplement$getStorageInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 17));
                    storage.mmceComplement$getFluidStorage().fill(new FluidStack(FluidRegistry.WATER, 500), true);
                    connected = true;
                }
                if (!settled(power) || iron() != 17 || water() != 500) break;
                require(node(firstPort).getGrid().getPathingService().getUsedChannels() == 2,
                    "Storage ports do not each consume one real channel");
                checks.add("physical-storage-controller-formed-two-ports-counted-once-items-and-fluids");
                world.setBlockToAir(stone);
                stage = 2;
                break;
            case 2:
                if (controller.isStructureFormed() || iron() != 0 || water() != 0) break;
                require(storage.mmceComplement$getStorageInventory().getStackInSlot(0).getCount() == 17,
                    "Breaking a storage structure discarded its sealed contents");
                checks.add("physical-storage-structure-break-seals-inventory");
                world.setBlockState(stone, Blocks.STONE.getDefaultState(), 3);
                stage = 3;
                break;
            case 3:
                if (!controller.isStructureFormed() || iron() != 17 || water() != 500) break;
                checks.add("physical-storage-structure-reformation-restores-inventory");
                firstLink.destroy();
                changedAt = ticks;
                stage = 4;
                break;
            case 4:
                if (ticks < changedAt + 80 || !settled(secondPort) || !node(secondPort).isActive()) break;
                // The powered secondary port takes over the same controller inventory.
                require(iron() == 17 && water() == 500, "Secondary storage port failed to take over");
                GridHelper.createConnection(node(power).getNode(), node(firstPort).getNode());
                changedAt = ticks;
                stage = 5;
                break;
            case 5:
                if (ticks < changedAt + 80 || !settled(power)) break;
                require(iron() == 17 && water() == 500, "Rejoining storage port duplicated inventory");
                checks.add("physical-storage-port-disconnect-failover-and-rejoin-without-duplication");
                placeMachine("share", new BlockPos(32, 80, 16));
                firstShare = (TileMEConnectionShareHatch) parts.get(0);
                secondShare = (TileMEConnectionShareHatch) parts.get(1);
                firstTarget = parts.get(2);
                secondTarget = parts.get(3);
                power = placePower(controller.getPos().add(-4, 0, 0));
                otherPower = placePower(controller.getPos().add(-8, 0, 0));
                connected = false;
                stage = 6;
                break;
            case 6:
                if (!ready(firstShare, secondShare, firstTarget, secondTarget, power, otherPower)
                    || !controller.isStructureFormed()) break;
                if (!connected) {
                    GridHelper.createConnection(node(power).getNode(), node(firstShare).getNode());
                    secondLink = GridHelper.createConnection(node(power).getNode(), node(secondShare).getNode());
                    connected = true;
                    changedAt = ticks;
                }
                MEConnectionShareManager.refresh(controller);
                if (ticks < changedAt + 20) break;
                if (!settled(power) || !node(firstTarget).isActive() || !node(secondTarget).isActive()) break;
                require(node(firstTarget).getGrid() == node(power).getGrid()
                    && node(secondTarget).getGrid() == node(power).getGrid(), "Shared hatches use different native grids");
                require(node(power).getGrid().getPathingService().getUsedChannels() == 4,
                    "Two sharing hatches did not charge both targets and their duplicate channels; actual="
                        + node(power).getGrid().getPathingService().getUsedChannels() + " nodes=" + describeNodes());
                checks.add("physical-connection-sharing-complement-and-original-mmce-hatches-with-duplicate-channel-charge");
                secondLink.destroy();
                secondLink = GridHelper.createConnection(node(otherPower).getNode(), node(secondShare).getNode());
                changedAt = ticks;
                stage = 7;
                break;
            case 7:
                MEConnectionShareManager.refresh(controller);
                if (ticks < changedAt + 20) break;
                if (!settled(power) || !settled(otherPower)) break;
                require(MEConnectionShareManager.failure(controller) != null,
                    "Sharing two powered networks did not report a conflict");
                require(node(firstTarget).getGrid() != node(power).getGrid()
                    && node(secondTarget).getGrid() != node(power).getGrid(), "Conflict retained the manager's old links");
                checks.add("connection-sharing-network-conflict-detaches-owned-links");
                secondLink.destroy();
                secondLink = GridHelper.createConnection(node(power).getNode(), node(secondShare).getNode());
                changedAt = ticks;
                stage = 8;
                break;
            case 8:
                MEConnectionShareManager.refresh(controller);
                if (ticks < changedAt + 20) break;
                if (!settled(power) || !node(firstTarget).isActive() || !node(secondTarget).isActive()) break;
                require(MEConnectionShareManager.failure(controller) == null, "Sharing did not recover from conflict");
                checks.add("connection-sharing-recovers-after-network-conflict");
                world.setBlockToAir(stone);
                changedAt = ticks;
                stage = 9;
                break;
            case 9:
                if (controller.isStructureFormed()) break;
                if (!tearingDown) {
                    MEConnectionShareManager.clear(controller);
                    changedAt = ticks;
                    tearingDown = true;
                }
                if (ticks < changedAt + 20) break;
                if (!settled(power)) break;
                require(node(firstTarget).getGrid() != node(power).getGrid()
                    && node(secondTarget).getGrid() != node(power).getGrid()
                    && node(power).getGrid().getPathingService().getUsedChannels() == 0,
                    "Breaking the sharing structure leaked native links or channel endpoints");
                checks.add("physical-sharing-structure-break-releases-links-and-duplicate-channels");
                return true;
            default: throw new IllegalStateException("Unexpected physical fixture stage " + stage);
        }
        if (ticks > 1200) throw new IllegalStateException("Physical machine fixture timed out in stage " + stage);
        return false;
    }

    private void placeMachine(String suffix, BlockPos center) {
        String path = "mmce_complement_verification_" + suffix;
        BlockController block = null;
        DynamicMachine machine = null;
        for (Map.Entry<DynamicMachine, BlockController> entry : BlockController.MACHINE_CONTROLLERS.entrySet()) {
            if (entry.getKey().getRegistryName().getPath().equals(path)) {
                machine = entry.getKey(); block = entry.getValue(); break;
            }
        }
        require(block != null, "Missing verification machine definition: " + path);
        parts.clear();
        IBlockState state = block.getDefaultState().withProperty(BlockController.FACING, EnumFacing.NORTH);
        world.getChunk(center);
        world.setBlockState(center, state, 3);
        controller = (TileMachineController) world.getTileEntity(center);
        // Sort by coordinates so the ports/sharing hatches precede the two input buses.
        List<Map.Entry<BlockPos, BlockArray.BlockInformation>> entries = new ArrayList<>(
            BlockArrayCache.getBlockArrayCache(machine.getPattern(), EnumFacing.NORTH).getPattern().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : entries) {
            BlockPos pos = center.add(entry.getKey());
            IBlockState part = entry.getValue().getSampleState();
            world.getChunk(pos);
            world.setBlockState(pos, part, 3);
            if (part.getBlock() == Blocks.STONE) stone = pos;
            else if (world.getTileEntity(pos) != null) parts.add(world.getTileEntity(pos));
        }
    }

    private TileEntity placePower(BlockPos pos) {
        world.getChunk(pos);
        world.setBlockState(pos, AEBlocks.CREATIVE_ENERGY_CELL.block().getDefaultState(), 3);
        return world.getTileEntity(pos);
    }
    private IManagedGridNode node(Object tile) {
        return tile instanceof TileStoragePort ? ((TileStoragePort) tile).bus().getMainNode()
            : ((IGridConnectedTile) tile).getMainNode();
    }
    private String describeNodes() {
        StringBuilder out = new StringBuilder();
        for (ae2.api.networking.IGridNode node : node(power).getGrid().getNodes()) out.append(node.getOwner().getClass().getName())
            .append(':').append(node.hasFlag(ae2.api.networking.GridFlags.REQUIRE_CHANNEL)).append(';');
        return out.toString();
    }
    private boolean ready(Object... tiles) {
        for (Object tile : tiles) if (!node(tile).isReady()) return false;
        return true;
    }
    private boolean settled(Object tile) {
        return node(tile).getGrid() != null && !node(tile).getGrid().getPathingService().isNetworkBooting();
    }
    private long iron() { return node(secondPort).getGrid().getStorageService().getCachedInventory().get(AEItemKey.of(Items.IRON_INGOT)); }
    private long water() { return node(secondPort).getGrid().getStorageService().getCachedInventory().get(AEFluidKey.of(FluidRegistry.WATER)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
