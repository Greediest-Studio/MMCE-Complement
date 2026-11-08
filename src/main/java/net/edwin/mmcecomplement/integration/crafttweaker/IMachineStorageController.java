package net.edwin.mmcecomplement.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.mmce.common.helper.IMachineController;
import net.edwin.mmcecomplement.variant.StorageController;
import net.edwin.mmcecomplement.variant.StorageResizing;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** Storage-specialized controller API. All returned stacks are detached snapshots. */
@ZenRegister
@ZenClass("mods.modularmachinery.IMachineStorageController")
public interface IMachineStorageController extends IMachineController {
    @ZenGetter("storageAvailable")
    default boolean isStorageAvailable() { return StorageScriptAccess.available(this) != null; }

    @ZenGetter("maxItemSlots")
    default int getMaxItemSlots() {
        StorageController storage = StorageScriptAccess.storage(this);
        return storage == null ? 0 : Math.max(0, storage.mmceComplement$getConfiguredStorageSlots());
    }

    @ZenGetter("maxFluidSlots")
    default int getMaxFluidSlots() {
        StorageController storage = StorageScriptAccess.storage(this);
        return storage == null || !storage.mmceComplement$hasFluidStorage()
            ? 0 : Math.max(0, storage.mmceComplement$getFluidStorageSlots());
    }

    @ZenGetter("maxGasSlots")
    default int getMaxGasSlots() {
        StorageController storage = StorageScriptAccess.storage(this);
        return storage == null || !storage.mmceComplement$hasGasStorage()
            ? 0 : Math.max(0, storage.mmceComplement$getGasStorageSlots());
    }

    @ZenGetter("fluidSlotCapacity")
    default int getFluidSlotCapacity() {
        StorageController storage = StorageScriptAccess.storage(this);
        return getMaxFluidSlots() == 0 ? 0 : Math.max(0, storage.mmceComplement$getFluidStorageCapacity());
    }

    @ZenGetter("gasSlotCapacity")
    default int getGasSlotCapacity() {
        StorageController storage = StorageScriptAccess.storage(this);
        return getMaxGasSlots() == 0 ? 0 : Math.max(0, storage.mmceComplement$getGasStorageCapacity());
    }

    @ZenMethod
    default int getFluidCapacityInSlot(int slot) {
        StorageController storage = StorageScriptAccess.storage(this);
        return storage == null ? 0 : storage.mmceComplement$getFluidCapacityInSlot(slot);
    }

    @ZenMethod
    default int getGasCapacityInSlot(int slot) {
        StorageController storage = StorageScriptAccess.storage(this);
        return storage == null ? 0 : storage.mmceComplement$getGasCapacityInSlot(slot);
    }

    @ZenMethod
    default boolean setMaxItemSlots(int slots) {
        return StorageResizing.slots(StorageScriptAccess.available(this), "Item", slots);
    }

    @ZenMethod
    default boolean setMaxFluidSlots(int slots) {
        return StorageResizing.slots(StorageScriptAccess.available(this), "Fluid", slots);
    }

    @ZenMethod
    default boolean setMaxGasSlots(int slots) {
        return StorageResizing.slots(StorageScriptAccess.available(this), "Gas", slots);
    }

    @ZenMethod
    default boolean setFluidCapacityInSlot(int slot, int capacity) {
        return slot >= 0 && StorageResizing.capacity(StorageScriptAccess.available(this), "Fluid", slot, capacity);
    }

    @ZenMethod
    default boolean setGasCapacityInSlot(int slot, int capacity) {
        return slot >= 0 && StorageResizing.capacity(StorageScriptAccess.available(this), "Gas", slot, capacity);
    }

    @ZenMethod
    default boolean setFluidSlotCapacity(int capacity) {
        return StorageResizing.capacity(StorageScriptAccess.available(this), "Fluid", -1, capacity);
    }

    @ZenMethod
    default boolean setGasSlotCapacity(int capacity) {
        return StorageResizing.capacity(StorageScriptAccess.available(this), "Gas", -1, capacity);
    }

    @ZenMethod
    default IItemStack getItemInSlot(int slot) {
        net.minecraft.item.ItemStack stack = StorageScriptAccess.itemAt(
            StorageScriptAccess.items(StorageScriptAccess.available(this)), slot);
        return stack.isEmpty() ? null : CraftTweakerMC.getIItemStack(stack);
    }

    @ZenMethod
    default ILiquidStack getFluidInSlot(int slot) {
        if (slot < 0) return null;
        net.minecraftforge.fluids.FluidStack stack = StorageScriptAccess.fluidAt(
            StorageScriptAccess.fluids(StorageScriptAccess.available(this), slot));
        return stack == null ? null : CraftTweakerMC.getILiquidStack(stack);
    }

    @ZenMethod
    default IIngredient getGasInSlot(int slot) {
        if (slot < 0 || slot >= getMaxGasSlots()) return null;
        return StorageScriptGasBridge.stack(StorageScriptGasBridge.handler(StorageScriptAccess.available(this)), slot);
    }

    @ZenMethod
    default long countItem(IItemStack stack) {
        return stack == null ? 0 : StorageScriptAccess.countItem(
            StorageScriptAccess.items(StorageScriptAccess.available(this)), CraftTweakerMC.getItemStack(stack));
    }

    @ZenMethod
    default long countFluid(ILiquidStack stack) {
        return stack == null ? 0 : StorageScriptAccess.countFluid(
            StorageScriptAccess.fluids(StorageScriptAccess.available(this), -1), CraftTweakerMC.getLiquidStack(stack));
    }

    @ZenMethod
    default long countGas(IIngredient stack) {
        return StorageScriptGasBridge.count(StorageScriptGasBridge.handler(StorageScriptAccess.available(this)), stack);
    }

    /** Presence checks ignore request quantity; use countX for quantity requirements. */
    @ZenMethod
    default boolean hasItem(IItemStack stack) { return countItem(stack) > 0; }

    @ZenMethod
    default boolean hasFluid(ILiquidStack stack) { return countFluid(stack) > 0; }

    @ZenMethod
    default boolean hasGas(IIngredient stack) { return countGas(stack) > 0; }

    @ZenMethod
    default int insertItem(IItemStack stack, @Optional boolean simulate) {
        return stack == null ? 0 : StorageScriptAccess.transferItem(
            StorageScriptAccess.items(StorageScriptAccess.available(this)), CraftTweakerMC.getItemStack(stack), -1, true, simulate);
    }

    @ZenMethod
    default int insertItemInSlot(int slot, IItemStack stack, @Optional boolean simulate) {
        if (slot < 0) return 0;
        return stack == null ? 0 : StorageScriptAccess.transferItem(
            StorageScriptAccess.items(StorageScriptAccess.available(this)), CraftTweakerMC.getItemStack(stack), slot, true, simulate);
    }

    @ZenMethod
    default int extractItem(IItemStack stack, @Optional boolean simulate) {
        return stack == null ? 0 : StorageScriptAccess.transferItem(
            StorageScriptAccess.items(StorageScriptAccess.available(this)), CraftTweakerMC.getItemStack(stack), -1, false, simulate);
    }

    @ZenMethod
    default int extractItemInSlot(int slot, IItemStack stack, @Optional boolean simulate) {
        if (slot < 0) return 0;
        return stack == null ? 0 : StorageScriptAccess.transferItem(
            StorageScriptAccess.items(StorageScriptAccess.available(this)), CraftTweakerMC.getItemStack(stack), slot, false, simulate);
    }

    @ZenMethod
    default int insertFluid(ILiquidStack stack, @Optional boolean simulate) {
        return stack == null ? 0 : StorageScriptAccess.transferFluid(
            StorageScriptAccess.fluids(StorageScriptAccess.available(this), -1), CraftTweakerMC.getLiquidStack(stack), true, simulate);
    }

    @ZenMethod
    default int insertFluidInSlot(int slot, ILiquidStack stack, @Optional boolean simulate) {
        if (slot < 0) return 0;
        return stack == null ? 0 : StorageScriptAccess.transferFluid(
            StorageScriptAccess.fluids(StorageScriptAccess.available(this), slot), CraftTweakerMC.getLiquidStack(stack), true, simulate);
    }

    @ZenMethod
    default int extractFluid(ILiquidStack stack, @Optional boolean simulate) {
        return stack == null ? 0 : StorageScriptAccess.transferFluid(
            StorageScriptAccess.fluids(StorageScriptAccess.available(this), -1), CraftTweakerMC.getLiquidStack(stack), false, simulate);
    }

    @ZenMethod
    default int extractFluidInSlot(int slot, ILiquidStack stack, @Optional boolean simulate) {
        if (slot < 0) return 0;
        return stack == null ? 0 : StorageScriptAccess.transferFluid(
            StorageScriptAccess.fluids(StorageScriptAccess.available(this), slot), CraftTweakerMC.getLiquidStack(stack), false, simulate);
    }

    @ZenMethod
    default int insertGas(IIngredient stack, @Optional boolean simulate) {
        return stack == null ? 0 : StorageScriptGasBridge.transfer(
            StorageScriptGasBridge.handler(StorageScriptAccess.available(this)), stack, -1, true, simulate);
    }

    @ZenMethod
    default int insertGasInSlot(int slot, IIngredient stack, @Optional boolean simulate) {
        if (slot < 0) return 0;
        return stack == null ? 0 : StorageScriptGasBridge.transfer(
            StorageScriptGasBridge.handler(StorageScriptAccess.available(this)), stack, slot, true, simulate);
    }

    @ZenMethod
    default int extractGas(IIngredient stack, @Optional boolean simulate) {
        return stack == null ? 0 : StorageScriptGasBridge.transfer(
            StorageScriptGasBridge.handler(StorageScriptAccess.available(this)), stack, -1, false, simulate);
    }

    @ZenMethod
    default int extractGasInSlot(int slot, IIngredient stack, @Optional boolean simulate) {
        if (slot < 0) return 0;
        return stack == null ? 0 : StorageScriptGasBridge.transfer(
            StorageScriptGasBridge.handler(StorageScriptAccess.available(this)), stack, slot, false, simulate);
    }

    /** Stable across inventory changes, empty slots and changes in other type counts. */
    @ZenMethod
    default long getSlotId(String type, int index) {
        int kind = "item".equals(type) ? 0 : "fluid".equals(type) ? 1 : "gas".equals(type) ? 2 : -1;
        int count = kind == 0 ? getMaxItemSlots() : kind == 1 ? getMaxFluidSlots() : kind == 2 ? getMaxGasSlots() : 0;
        return index < 0 || index >= count ? -1L : 3L * index + kind;
    }

    @ZenMethod
    default int getSlotIndex(long id) {
        if (id < 0 || id / 3 > Integer.MAX_VALUE) return -1;
        String type = id % 3 == 0 ? "item" : id % 3 == 1 ? "fluid" : "gas";
        int index = (int) (id / 3);
        return getSlotId(type, index) == id ? index : -1;
    }

    @ZenMethod
    default String getSlotType(long id) {
        return getSlotIndex(id) < 0 ? null : id % 3 == 0 ? "item" : id % 3 == 1 ? "fluid" : "gas";
    }

    @ZenMethod
    default IItemStack getItemBySlotId(long id) {
        return "item".equals(getSlotType(id)) ? getItemInSlot(getSlotIndex(id)) : null;
    }

    @ZenMethod
    default ILiquidStack getFluidBySlotId(long id) {
        return "fluid".equals(getSlotType(id)) ? getFluidInSlot(getSlotIndex(id)) : null;
    }

    @ZenMethod
    default IIngredient getGasBySlotId(long id) {
        return "gas".equals(getSlotType(id)) ? getGasInSlot(getSlotIndex(id)) : null;
    }
}

