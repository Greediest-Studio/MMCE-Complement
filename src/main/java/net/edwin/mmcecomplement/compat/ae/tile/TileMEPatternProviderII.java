package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.fluids.util.IAEFluidTank;
import appeng.helpers.DualityInterface;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.common.util.PatternItemFilter;
import hellfirepvp.modularmachinery.ModularMachinery;
import net.edwin.mmcecomplement.compat.ae2fc.Ae2FcrPatternCompat;
import net.edwin.mmcecomplement.compat.mekeng.MekEngPatternCompat;
import net.edwin.mmcecomplement.compat.CompatMods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Four-capacity variant of MMCE's pattern provider.
 *
 * <p>The parent type is retained so AE2 interface terminals, pattern tools,
 * MMCE machine component sharing and the original action/sync packets continue
 * to recognize this provider. All capacity-dependent state is deliberately
 * replaced here because the original implementation stores its 36-slot arrays
 * in final fields.</p>
 */
public class TileMEPatternProviderII extends MEPatternProvider {

    public static final int PATTERN_COLUMNS = 18;
    public static final int PATTERN_ROWS = 8;
    public static final int PATTERN_SLOTS = PATTERN_COLUMNS * PATTERN_ROWS;
    public static final int SUB_ITEM_SLOTS = 9;
    public static final int SUB_FLUID_SLOTS = 3;

    private final AppEngInternalInventory subItemsII =
        new AppEngInternalInventory(this, SUB_ITEM_SLOTS);
    private final AEFluidInventoryUpgradeable subFluidsII =
        new AEFluidInventoryUpgradeable(this, SUB_FLUID_SLOTS,
            Integer.MAX_VALUE);
    private final AppEngInternalInventory patternsII =
        new AppEngInternalInventory(this, PATTERN_SLOTS, 1,
            PatternItemFilter.INSTANCE);
    private final ICraftingPatternDetails[] detailsII =
        new ICraftingPatternDetails[PATTERN_SLOTS];
    private final InfItemFluidHandler handlerII =
        new InfItemFluidHandler(subItemsII, subFluidsII);
    private final List<MachineComponent<?>> combinationComponentsII =
        new ObjectArrayList<>();

    private String machineNameII;

    public TileMEPatternProviderII() {
        bindExpandedPatternInventory();
        configureHandler(handlerII);
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            combinationComponentsII.add(new MachineComponent<InfItemFluidHandler>(
                IOType.INPUT) {
                private final InfItemFluidHandler isolatedHandler =
                    new InfItemFluidHandler(subItemsII, subFluidsII);
                private final long groupId = getUniqueGroupID();

                {
                    configureHandler(isolatedHandler);
                }

                @Override
                public ComponentType getComponentType() {
                    return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
                }

                @Override
                public InfItemFluidHandler getContainerProvider() {
                    return isolatedHandler;
                }

                @Override
                public long getGroupID() {
                    return groupId;
                }
            });
        }
    }

    /**
     * MMCE replaces DualityInterface's native pattern inventory from a mixin
     * in its constructor. That constructor runs before subclass fields, so a
     * virtual call to {@link #getPatterns()} observes {@code null} here. Bind
     * the fully initialized expanded inventory once construction reaches us.
     */
    private void bindExpandedPatternInventory() {
        ObfuscationReflectionHelper.setPrivateValue(DualityInterface.class,
            getInterfaceDuality(), patternsII, "patterns");
    }

    private void configureHandler(InfItemFluidHandler target) {
        target.setOnItemChanged(slot -> {
            handlerDirty = true;
            markChunkDirty();
        });
        target.setOnFluidChanged(slot -> {
            handlerDirty = true;
            markChunkDirty();
        });
        if (CompatMods.isAeGasCompatLoaded()) {
            target.setOnGasChanged(slot -> {
                handlerDirty = true;
                markChunkDirty();
            });
        }
    }

    @Override
    public List<MachineComponent<?>> getCombinationComponents() {
        return combinationComponentsII;
    }

    @Nullable
    @Override
    public MachineComponent<InfItemFluidHandler> provideComponent() {
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            return null;
        }
        return new MachineComponent<InfItemFluidHandler>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                return handlerII;
            }

            @Override
            public long getGroupID() {
                return getGroupId();
            }
        };
    }

    @Nonnull
    @Override
    public Collection<MachineComponent<?>> provideComponents() {
        return workMode == WorkModeSetting.ISOLATION_INPUT
            ? combinationComponentsII : Collections.emptyList();
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        if (!proxy.isActive() || !proxy.isPowered()) {
            return;
        }
        Arrays.stream(detailsII)
            .filter(Objects::nonNull)
            .forEach(detail -> craftingTracker.addCraftingOption(this, detail));
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails,
                               InventoryCrafting table) {
        if (!acceptsPatternII(patternDetails)) {
            return false;
        }

        InfItemFluidHandler target = handlerII;
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            target = null;
            for (int i = 0; i < detailsII.length; i++) {
                if (patternDetails.equals(detailsII[i])) {
                    target = (InfItemFluidHandler) combinationComponentsII
                        .get(i).getContainerProvider();
                    break;
                }
            }
        }
        if (target == null) {
            return false;
        }

        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            ItemStack stack = table.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (CompatMods.isAe2FcrCompatLoaded()
                    && Ae2FcrPatternCompat.appendFakeFluid(stack, target)) {
                continue;
            }
            if (CompatMods.isAeGasCompatLoaded()
                    && MekEngPatternCompat.appendFakeGas(stack, target)) {
                continue;
            }
            target.appendItem(stack);
        }

        handleNewPatternII(patternDetails);
        machineCompleted = workMode != WorkModeSetting.CRAFTING_LOCK_MODE;
        return true;
    }

    private boolean acceptsPatternII(ICraftingPatternDetails patternDetails) {
        if (patternDetails.isCraftable() || !proxy.isActive()
            || !proxy.isPowered()) {
            return false;
        }
        return workMode != WorkModeSetting.ENHANCED_BLOCKING_MODE
            || handlerII.isEmpty() || currentPattern == null
            || currentPattern.equals(patternDetails);
    }

    private void handleNewPatternII(ICraftingPatternDetails patternDetails) {
        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            if (!patternDetails.equals(currentPattern)) {
                setCurrentPatternII(patternDetails);
            }
        } else {
            resetCurrentPatternII();
        }
    }

    @Override
    public boolean isBusy() {
        return (workMode == WorkModeSetting.CRAFTING_LOCK_MODE
            && !machineCompleted)
            || (workMode == WorkModeSetting.BLOCKING_MODE
                && !handlerII.isEmpty());
    }

    @Override
    protected void refreshPatterns() {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            refreshPattern(slot);
        }
        if (currentPatternIdx >= 0 && currentPatternIdx < detailsII.length) {
            setCurrentPatternII(detailsII[currentPatternIdx]);
        }
        try {
            proxy.getGrid().postEvent(new MENetworkCraftingPatternChange(
                this, proxy.getNode()));
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    protected void refreshPattern(int slot) {
        if (slot < 0 || slot >= detailsII.length) {
            return;
        }
        detailsII[slot] = null;
        ItemStack pattern = patternsII.getStackInSlot(slot);
        Item patternItem = pattern.getItem();
        if (!pattern.isEmpty()
            && patternItem instanceof ICraftingPatternItem) {
            ICraftingPatternDetails detail =
                ((ICraftingPatternItem) patternItem)
                    .getPatternForItem(pattern, getWorld());
            if (detail != null && !detail.isCraftable()) {
                detailsII[slot] = detail;
            }
        }

        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE
            && slot == currentPatternIdx) {
            ICraftingPatternDetails detail = detailsII[slot];
            if (currentPattern == null) {
                currentPattern = detail;
            } else if (!currentPattern.equals(detail)) {
                resetCurrentPatternII();
            }
        }
    }

    @Override
    public void returnItemsScheduled() {
        if (!shouldReturnItems) {
            shouldReturnItems = true;
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::returnItemsII);
        }
    }

    private void returnItemsII() {
        if (!shouldReturnItems || !proxy.isActive() || !proxy.isPowered()) {
            return;
        }
        shouldReturnItems = false;
        machineCompleted = true;
        synchronized (handlerII) {
            returnHandlerToNetwork(handlerII);
        }
        for (MachineComponent<?> component : combinationComponentsII) {
            InfItemFluidHandler isolated = (InfItemFluidHandler)
                component.getContainerProvider();
            synchronized (isolated) {
                returnHandlerToNetwork(isolated);
            }
        }
        handlerDirty = true;
        markChunkDirty();
    }

    private void returnHandlerToNetwork(InfItemFluidHandler target) {
        try {
            IItemStorageChannel itemChannel = AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> itemInventory =
                proxy.getStorage().getInventory(itemChannel);
            List<ItemStack> items = target.getItemStackList();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                IAEItemStack remainder = insertToNetwork(itemInventory,
                    itemChannel.createStack(stack));
                items.set(i, remainder == null
                    ? ItemStack.EMPTY : remainder.createItemStack());
            }

            IFluidStorageChannel fluidChannel = AEApi.instance().storage()
                .getStorageChannel(IFluidStorageChannel.class);
            IMEMonitor<IAEFluidStack> fluidInventory =
                proxy.getStorage().getInventory(fluidChannel);
            List<FluidStack> fluids = target.getFluidStackList();
            for (int i = 0; i < fluids.size(); i++) {
                FluidStack stack = fluids.get(i);
                if (stack == null) {
                    continue;
                }
                IAEFluidStack remainder = insertToNetwork(fluidInventory,
                    fluidChannel.createStack(stack));
                fluids.set(i, remainder == null
                    ? null : remainder.getFluidStack());
            }
            if (CompatMods.isAeGasCompatLoaded()) {
                MekEngPatternCompat.returnGasesToNetwork(
                    target, proxy, source);
            }
        } catch (GridAccessException ignored) {
        }
    }

    private <T extends IAEStack<T>> T insertToNetwork(IMEMonitor<T> inventory,
                                                       T stack)
        throws GridAccessException {
        return stack == null ? null : Platform.poweredInsert(proxy.getEnergy(),
            inventory, stack.copy(), source);
    }

    private void resetCurrentPatternII() {
        currentPatternIdx = -1;
        currentPattern = null;
    }

    private void setCurrentPatternII(ICraftingPatternDetails pattern) {
        if (pattern == null) {
            resetCurrentPatternII();
            return;
        }
        for (int i = 0; i < detailsII.length; i++) {
            if (pattern.equals(detailsII[i])) {
                currentPatternIdx = i;
                break;
            }
        }
        currentPattern = pattern;
    }

    @Override
    public AppEngInternalInventory getSubItemHandler() {
        return subItemsII;
    }

    @Override
    public AEFluidInventoryUpgradeable getSubFluidHandler() {
        return subFluidsII;
    }

    @Override
    public InfItemFluidHandler getInfHandler() {
        return handlerII;
    }

    @Override
    public AppEngInternalInventory getPatterns() {
        return patternsII;
    }

    @Override
    public void setWorkMode(WorkModeSetting nextMode) {
        if (workMode == WorkModeSetting.ISOLATION_INPUT
            || nextMode == WorkModeSetting.ISOLATION_INPUT) {
            returnItemsScheduled();
        }
        workMode = nextMode;
        if (nextMode != WorkModeSetting.CRAFTING_LOCK_MODE) {
            machineCompleted = true;
        }
        if (nextMode != WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            resetCurrentPatternII();
        }
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability,
                                 @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability,
                               @Nullable EnumFacing facing) {
        Capability<IItemHandler> itemCapability =
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        if (capability == itemCapability) {
            return itemCapability.cast(patternsII);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readProviderNBT(NBTTagCompound compound) {
        subItemsII.readFromNBT(compound, "subItemHandler");
        subFluidsII.readFromNBT(compound, "subFluidHandler");
        patternsII.readFromNBT(compound, "patterns");
        readProviderHandlerNBT(compound, false);
        if (compound.hasKey("currentPatternIdx", 99)
            && workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            currentPatternIdx = compound.getInteger("currentPatternIdx");
            if (currentPatternIdx < 0
                || currentPatternIdx >= PATTERN_SLOTS) {
                resetCurrentPatternII();
            }
        } else {
            resetCurrentPatternII();
        }
    }

    @Override
    public void readProviderHandlerNBT(NBTTagCompound compound,
                                       boolean allHandlers) {
        int ordinal = compound.getByte("workMode");
        workMode = ordinal >= 0 && ordinal < WorkModeSetting.values().length
            ? WorkModeSetting.values()[ordinal] : WorkModeSetting.DEFAULT;
        if (allHandlers) {
            handlerII.readFromNBT(compound, "handler");
            NBTTagCompound components = compound.getCompoundTag("components");
            for (int i = 0; i < combinationComponentsII.size(); i++) {
                ((InfItemFluidHandler) combinationComponentsII.get(i)
                    .getContainerProvider()).readFromNBT(components,
                        "handler#" + i);
            }
        } else if (workMode == WorkModeSetting.ISOLATION_INPUT
            && compound.hasKey("components", 10)) {
            NBTTagCompound components = compound.getCompoundTag("components");
            for (int i = 0; i < combinationComponentsII.size(); i++) {
                String key = "handler#" + i;
                if (components.hasKey(key, 10)) {
                    ((InfItemFluidHandler) combinationComponentsII.get(i)
                        .getContainerProvider()).readFromNBT(components, key);
                }
            }
        } else {
            handlerII.readFromNBT(compound, "handler");
        }
    }

    @Override
    public NBTTagCompound writeProviderNBT(NBTTagCompound compound) {
        patternsII.writeToNBT(compound, "patterns");
        subItemsII.writeToNBT(compound, "subItemHandler");
        subFluidsII.writeToNBT(compound, "subFluidHandler");
        if (!handlerII.isEmpty() && currentPatternIdx >= 0) {
            compound.setInteger("currentPatternIdx", currentPatternIdx);
        }
        return writeProviderHandlerNBT(compound);
    }

    @Override
    public NBTTagCompound writeProviderHandlerNBT(NBTTagCompound compound) {
        if (workMode != WorkModeSetting.DEFAULT) {
            compound.setByte("workMode", (byte) workMode.ordinal());
        }
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            NBTTagCompound components = new NBTTagCompound();
            for (int i = 0; i < combinationComponentsII.size(); i++) {
                InfItemFluidHandler isolated = (InfItemFluidHandler)
                    combinationComponentsII.get(i).getContainerProvider();
                if (!isolated.isEmpty()) {
                    isolated.writeToNBT(components, "handler#" + i);
                }
            }
            if (!components.isEmpty()) {
                compound.setTag("components", components);
            }
        } else {
            handlerII.writeToNBT(compound, "handler");
        }
        return compound;
    }

    @Override
    public boolean isAllDefault() {
        if (IntStream.range(0, subItemsII.getSlots())
            .mapToObj(subItemsII::getStackInSlot)
            .anyMatch(stack -> !stack.isEmpty())) {
            return false;
        }
        for (int slot = 0; slot < subFluidsII.getSlots(); slot++) {
            if (subFluidsII.getFluidInSlot(slot) != null) {
                return false;
            }
        }
        if (IntStream.range(0, patternsII.getSlots())
            .mapToObj(patternsII::getStackInSlot)
            .anyMatch(stack -> !stack.isEmpty())) {
            return false;
        }
        if (!handlerII.isEmpty()) {
            return false;
        }
        for (MachineComponent<?> component : combinationComponentsII) {
            if (!((InfItemFluidHandler) component.getContainerProvider())
                .isEmpty()) {
                return false;
            }
        }
        return workMode == WorkModeSetting.DEFAULT;
    }

    @Override
    public void onChangeInventory(IItemHandler inventory, int slot,
                                  InvOperation operation,
                                  ItemStack removedStack,
                                  ItemStack newStack) {
        refreshPattern(slot);
        try {
            proxy.getGrid().postEvent(new MENetworkCraftingPatternChange(
                this, proxy.getNode()));
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_PATTERN_PROVIDER_II == null
            ? ItemStack.EMPTY
            : new ItemStack(ModBlocks.ME_PATTERN_PROVIDER_II);
    }

    @Override
    public String getMachineName() {
        return machineNameII == null
            ? "tile.mmce_complement.me_pattern_provider_ii"
            : machineNameII;
    }

    @Override
    public void setMachineName(String name) {
        machineNameII = name;
    }

    @Override
    public String getCustomInventoryName() {
        return hasCustomInventoryName() ? super.getCustomInventoryName()
            : "tile.mmce_complement.me_pattern_provider_ii";
    }

    @Override
    public void invalidate() {
        super.invalidate();
        GROUP_ACQUIRER.addAndGet(-PATTERN_SLOTS);
    }
}
