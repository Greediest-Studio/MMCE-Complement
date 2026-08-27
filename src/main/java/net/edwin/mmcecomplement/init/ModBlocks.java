package net.edwin.mmcecomplement.init;

import net.edwin.mmcecomplement.block.BlockFluxInputHatch;
import net.edwin.mmcecomplement.block.BlockFluxOutputHatch;
import net.edwin.mmcecomplement.block.BlockAcceleratorHatch;
import net.edwin.mmcecomplement.block.BlockBatchHatch;
import net.edwin.mmcecomplement.block.BlockCasing;
import net.edwin.mmcecomplement.block.BlockDataItemInputHatch;
import net.edwin.mmcecomplement.block.BlockItemInputAssemblyHatch;
import net.edwin.mmcecomplement.block.BlockItemOutputAssemblyHatch;
import net.edwin.mmcecomplement.block.BlockLiquidEnergizerHatch;
import net.edwin.mmcecomplement.block.BlockSelfCycleAssemblyHatch;
import net.edwin.mmcecomplement.block.BlockFilteredItemOutputHatch;
import net.edwin.mmcecomplement.block.BlockFilteredFluidOutputHatch;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEOreDictInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEItemInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEFluidInventoryInputBus;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEInventoryInputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEOutputAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEFullExposureAssembly;
import net.edwin.mmcecomplement.compat.ae.block.BlockMEPatternProviderII;
import net.edwin.mmcecomplement.block.BlockMachineGlass;
import net.edwin.mmcecomplement.block.BlockOverclockHatch;
import net.edwin.mmcecomplement.block.BlockThreadHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockQuadFluidOutputHatch;
import net.edwin.mmcecomplement.block.BlockNineFluidInputHatch;
import net.edwin.mmcecomplement.block.BlockNineFluidOutputHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneControlHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneSignalInputHatch;
import net.edwin.mmcecomplement.block.BlockRedstoneSignalOutputHatch;
import net.minecraft.block.Block;

/**
 * Static references to all blocks added by MMCE Complement.
 */
public final class ModBlocks {

    public static BlockFluxInputHatch FLUX_INPUT_HATCH;
    public static BlockFluxOutputHatch FLUX_OUTPUT_HATCH;
    public static BlockCasing BLOCK_CASING;
    public static BlockMachineGlass MACHINE_GLASS;
    public static BlockThreadHatch THREAD_HATCH;
    public static BlockOverclockHatch OVERCLOCK_HATCH;
    public static BlockAcceleratorHatch ACCELERATOR_HATCH;
    public static BlockBatchHatch BATCH_HATCH;
    public static BlockRedstoneControlHatch REDSTONE_CONTROL_HATCH;
    public static BlockRedstoneSignalInputHatch REDSTONE_SIGNAL_INPUT_HATCH;
    public static BlockRedstoneSignalOutputHatch REDSTONE_SIGNAL_OUTPUT_HATCH;
    public static BlockLiquidEnergizerHatch LIQUID_ENERGIZER_HATCH;
    public static BlockDataItemInputHatch DATA_INPUT_ASSEMBLY_HATCH;
    public static BlockItemInputAssemblyHatch INPUT_ASSEMBLY_HATCH;
    public static BlockItemOutputAssemblyHatch OUTPUT_ASSEMBLY_HATCH;
    public static BlockSelfCycleAssemblyHatch SELF_CYCLE_ASSEMBLY_HATCH;
    public static BlockFilteredItemOutputHatch FILTERED_ITEM_OUTPUT_HATCH;
    public static BlockFilteredFluidOutputHatch FILTERED_FLUID_OUTPUT_HATCH;
    public static BlockMEOreDictInputBus ME_ORE_DICT_INPUT_BUS;
    public static BlockMEItemInventoryInputBus ME_ITEM_INVENTORY_INPUT_BUS;
    public static BlockMEFluidInventoryInputBus ME_FLUID_INVENTORY_INPUT_BUS;
    // Kept as the vanilla base type so ModBlocks remains loadable without
    // the optional MekEng classes on the runtime class path.
    public static Block ME_GAS_INVENTORY_INPUT_BUS;
    // Requires AE2, Mekanism and Mekanism Energistics at runtime.
    public static Block ME_INPUT_ASSEMBLY;
    public static BlockMEInventoryInputAssembly ME_INVENTORY_INPUT_ASSEMBLY;
    public static BlockMEOutputAssembly ME_OUTPUT_ASSEMBLY;
    public static BlockMEFullExposureAssembly ME_FULL_EXPOSURE_ASSEMBLY;
    public static BlockQuadFluidInputHatch QUAD_FLUID_INPUT_HATCH_TINY;
    public static BlockQuadFluidOutputHatch QUAD_FLUID_OUTPUT_HATCH_TINY;
    public static BlockNineFluidInputHatch NINE_FLUID_INPUT_HATCH_NORMAL;
    public static BlockNineFluidOutputHatch NINE_FLUID_OUTPUT_HATCH_NORMAL;
    public static Block ME_ENERGY_INPUT_BUS;
    public static Block ME_ENERGY_OUTPUT_BUS;
    public static Block ME_MANA_INPUT_BUS;
    public static Block ME_MANA_OUTPUT_BUS;
    public static Block ME_CHANNEL_INPUT_HATCH;
    public static BlockMEPatternProviderII ME_PATTERN_PROVIDER_II;

    private ModBlocks() {}
}
