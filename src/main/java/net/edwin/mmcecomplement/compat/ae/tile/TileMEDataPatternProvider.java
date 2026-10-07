package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFinishEvent;
import github.kasuminova.mmce.common.event.machine.SmartInterfaceUpdateEvent;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RequirementComponents;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import net.edwin.mmcecomplement.Tags;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae2fc.Ae2FcrPatternCompat;
import net.edwin.mmcecomplement.compat.mekeng.MekEngPatternCompat;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.edwin.mmcecomplement.tile.PrioritySmartInterfaceProvider;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tier-one MMCE pattern provider with one shared smart-interface type and a
 * numeric marker for each encoded-pattern slot.
 *
 * <p>The primary data provider shares the normal input component's group. In
 * isolation mode, one lightweight provider per pattern slot shares the exact
 * group id of that slot's isolated item/fluid/gas component. All providers
 * delegate to the same binding list, so the block still represents one smart
 * interface even though recipe component grouping needs several wrappers.</p>
 */
public class TileMEDataPatternProvider extends MEPatternProvider
    implements ITickable, IMEDataPatternProvider {

    public static final int PATTERN_SLOTS = 36;
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(
        Tags.MOD_ID, "me_data_pattern_provider");
    public static final String TRANSLATION_KEY =
        "tile.mmce_complement.me_data_pattern_provider";

    private static final String TAG_SELECTED_TYPE = "dataInterfaceType";
    private static final String TAG_PATTERN_VALUES = "dataPatternValues";
    private static final String TAG_BOUND_DATA = "dataPatternBindings";
    private static final int BINDING_CLEANUP_INTERVAL = 20;
    private static final int MAX_INTERFACE_TYPE_LENGTH = 128;

    private final float[] patternValues = new float[PATTERN_SLOTS];
    /**
     * Isolation-mode slots that have dispatched a pattern whose data value is
     * still eligible for a recipe check.  The parent input handlers remain a
     * useful fallback for ordinary patterns with item/fluid inputs, while this
     * flag also covers valid zero-input/data-only patterns.
     */
    private final boolean[] activePatternSlots =
        new boolean[PATTERN_SLOTS];
    private final List<SmartInterfaceData> boundData = new ArrayList<>();
    private final DataPatternInterfaceProvider primaryDataProvider;
    private final DataPatternInterfaceProvider sharedDataProvider;
    private final List<DataPatternInterfaceProvider> slotDataProviders;
    private final List<DataPatternInterfaceProvider> allDataProviders;
    private final Map<Long, DataPatternInterfaceProvider> dataProvidersByGroup;

    @Nullable
    private String selectedInterfaceType;
    @Nullable
    private String machineName;
    private int bindingCleanupTicker;
    /**
     * A non-isolated provider has one shared smart-interface value.  Keep a
     * separate dispatch bit because a valid data-only pattern leaves the
     * normal item/fluid handler empty and would otherwise allow a second
     * pattern to overwrite the value before the first recipe starts.
     */
    private volatile boolean activeSharedPattern;

    public TileMEDataPatternProvider() {
        primaryDataProvider = new DataPatternInterfaceProvider(
            this, -1L, -1, false);
        /*
         * Keep controller registration separate from the recipe-facing
         * wrapper. The former must expose its binding while idle; the latter
         * must become invisible as soon as the dispatched pattern completes,
         * even if an existing recipe context still holds the wrapper.
         */
        sharedDataProvider = new DataPatternInterfaceProvider(
            this, -1L, -1, true);

        List<DataPatternInterfaceProvider> slots = new ArrayList<>(
            PATTERN_SLOTS);
        Map<Long, DataPatternInterfaceProvider> byGroup = new HashMap<>();
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            long groupId = combinationComponents.get(slot).getGroupID();
            DataPatternInterfaceProvider provider =
                new DataPatternInterfaceProvider(this, groupId, slot, true);
            slots.add(provider);
            byGroup.put(groupId, provider);
        }
        slotDataProviders = Collections.unmodifiableList(slots);

        List<DataPatternInterfaceProvider> all = new ArrayList<>(
            PATTERN_SLOTS + 2);
        all.add(primaryDataProvider);
        all.add(sharedDataProvider);
        all.addAll(slots);
        allDataProviders = Collections.unmodifiableList(all);
        dataProvidersByGroup = Collections.unmodifiableMap(byGroup);
    }

    @Override
    public List<MachineComponent<?>> getCombinationComponents() {
        // The current MMCE base already allocates one independent handler and
        // group id per pattern slot. Reuse that list so its NBT and lifecycle
        // behavior stay identical to the ordinary provider.
        return combinationComponents;
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
                return handler;
            }

            @Override
            public long getGroupID() {
                return getGroupId();
            }
        };
    }

    @Nonnull
    @Override
    public java.util.Collection<MachineComponent<?>> provideComponents() {
        return workMode == WorkModeSetting.ISOLATION_INPUT
            ? combinationComponents : Collections.emptyList();
    }

    public float getPatternValue(int slot) {
        checkPatternSlot(slot);
        return patternValues[slot];
    }

    @Nullable
    public String getSelectedInterfaceType() {
        return selectedInterfaceType;
    }

    /**
     * Drops an unsupported persisted selection only when doing so cannot
     * change the class used by another controller sharing this block.
     */
    public boolean resetUnsupportedSelectionForController(
        BlockPos controllerPos) {
        for (SmartInterfaceData data : boundData) {
            if (!data.getPos().equals(controllerPos)) {
                return false;
            }
        }
        boundData.clear();
        selectedInterfaceType = null;
        markForUpdateSync();
        return true;
    }

    /**
     * Returns an immutable snapshot. The returned data objects are copies, so
     * callers cannot bypass server-side validation by mutating their values.
     */
    @Nonnull
    public List<SmartInterfaceData> getBoundData() {
        List<SmartInterfaceData> snapshot = new ArrayList<>(boundData.size());
        for (SmartInterfaceData data : boundData) {
            snapshot.add(copyData(data));
        }
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Atomically applies the hatch-wide interface type and one slot's marker.
     * This is intended to be the sole server-side entry point used by the
     * secondary GUI packet.
     */
    public boolean configurePattern(int slot, String interfaceType,
                                    float value) {
        if (!isPatternSlot(slot) || !Float.isFinite(value)
            || getPatterns().getStackInSlot(slot).isEmpty()
            || hasPendingCraftingInputs()) {
            return false;
        }

        String trimmedType = interfaceType == null
            ? "" : interfaceType.trim();
        if (trimmedType.length() > MAX_INTERFACE_TYPE_LENGTH) {
            return false;
        }
        String normalizedType = normalizeType(interfaceType);
        if (normalizedType != null
            && !isTypeValidForBindings(normalizedType)) {
            return false;
        }

        patternValues[slot] = value;
        if (normalizedType != null
            && !normalizedType.equals(selectedInterfaceType)) {
            selectedInterfaceType = normalizedType;
            replaceBoundTypes(normalizedType);
        }
        synchronizeControllerBindings();
        markForUpdateSync();
        return true;
    }

    /**
     * Data components that correspond to the item/fluid/gas components
     * exposed by the provider's current work mode.
     */
    @Nonnull
    public List<DataPatternInterfaceProvider> getDataProvidersForCurrentMode() {
        return workMode == WorkModeSetting.ISOLATION_INPUT
            ? slotDataProviders
            : Collections.singletonList(sharedDataProvider);
    }

    @Nullable
    public DataPatternInterfaceProvider getDataProviderForGroup(long groupId) {
        if (workMode != WorkModeSetting.ISOLATION_INPUT) {
            return groupId == getGroupId() ? sharedDataProvider : null;
        }
        return dataProvidersByGroup.get(groupId);
    }

    @Nonnull
    public DataPatternInterfaceProvider getPrimaryDataProvider() {
        return primaryDataProvider;
    }

    /** All wrappers, used when controller bookkeeping needs to be repaired. */
    @Nonnull
    public List<DataPatternInterfaceProvider> getAllDataProviders() {
        return allDataProviders;
    }

    /**
     * Returns whether an isolation group currently belongs to a dispatched
     * pattern.  The group wrappers are registered statically by MMCE, so the
     * crafting-context mixin uses this gate to keep unsent slots invisible.
     */
    public boolean isDataProviderActiveForGroup(long groupId) {
        if (workMode != WorkModeSetting.ISOLATION_INPUT) {
            return groupId == getGroupId()
                && (activeSharedPattern || !handler.isEmpty());
        }
        DataPatternInterfaceProvider provider = dataProvidersByGroup.get(groupId);
        if (provider == null) {
            return false;
        }
        int slot = provider.patternSlot;
        synchronized (activePatternSlots) {
            if (activePatternSlots[slot]) {
                return true;
            }
        }
        Object container = combinationComponents.get(slot)
            .getContainerProvider();
        return container instanceof InfItemFluidHandler
            && !((InfItemFluidHandler) container).isEmpty();
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails,
                               InventoryCrafting table) {
        int patternSlot = findPatternSlot(patternDetails);
        if (patternSlot < 0 || isBusy() || table == null) {
            return false;
        }

        InfItemFluidHandler target = handler;
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            Object container = combinationComponents.get(patternSlot)
                .getContainerProvider();
            if (!(container instanceof InfItemFluidHandler)) {
                return false;
            }
            target = (InfItemFluidHandler) container;
        }
        if (!canAcceptPatternBeforeDispatch(patternDetails, target)) {
            return false;
        }

        /*
         * Publish the marker before exposing any input. MMCE may inspect
         * async-supported components concurrently, so doing this after the
         * append loop would leave a window in which the new input is visible
         * with the previous pattern's data value.
         */
        if (workMode == WorkModeSetting.ISOLATION_INPUT) {
            synchronized (activePatternSlots) {
                activePatternSlots[patternSlot] = true;
            }
        } else {
            // The shared data interface cannot represent two different
            // marker values at once, including patterns with no item/fluid
            // inputs. Keep the dispatch occupied until its recipe finishes
            // or the parent returns its buffered inputs.
            activeSharedPattern = true;
        }
        applyPatternValue(patternSlot);

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
        handleNewPattern(patternDetails);
        machineCompleted = workMode != WorkModeSetting.CRAFTING_LOCK_MODE;
        return true;
    }

    @Override
    public void returnItemsScheduled() {
        synchronized (activePatternSlots) {
            Arrays.fill(activePatternSlots, false);
        }
        activeSharedPattern = false;
        /*
         * MEPatternProvider's returnItems() is private and only drains its
         * grouped handler.  In isolation mode each pattern has a separate
         * InfItemFluidHandler, so delegating to the parent would strand every
         * per-slot buffer when a structure changes or a mode is switched.
         * Keep the same deferred-return contract, but drain all handlers.
         */
        if (!shouldReturnItems) {
            shouldReturnItems = true;
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(
                this::returnItemsWithIsolation);
        }
    }

    private void returnItemsWithIsolation() {
        if (!shouldReturnItems || !proxy.isActive() || !proxy.isPowered()) {
            return;
        }
        shouldReturnItems = false;
        machineCompleted = true;
        synchronized (handler) {
            returnHandlerToNetwork(handler);
        }
        for (MachineComponent<?> component : combinationComponents) {
            Object container = component.getContainerProvider();
            if (!(container instanceof InfItemFluidHandler)) {
                continue;
            }
            InfItemFluidHandler isolated = (InfItemFluidHandler) container;
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
            for (int index = 0; index < items.size(); index++) {
                ItemStack stack = items.get(index);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                IAEItemStack remainder = insertToNetwork(itemInventory,
                    itemChannel.createStack(stack));
                items.set(index, remainder == null
                    ? ItemStack.EMPTY : remainder.createItemStack());
            }

            IFluidStorageChannel fluidChannel = AEApi.instance().storage()
                .getStorageChannel(IFluidStorageChannel.class);
            IMEMonitor<IAEFluidStack> fluidInventory =
                proxy.getStorage().getInventory(fluidChannel);
            List<net.minecraftforge.fluids.FluidStack> fluids =
                target.getFluidStackList();
            for (int index = 0; index < fluids.size(); index++) {
                net.minecraftforge.fluids.FluidStack stack = fluids.get(index);
                if (stack == null) {
                    continue;
                }
                IAEFluidStack remainder = insertToNetwork(fluidInventory,
                    fluidChannel.createStack(stack));
                fluids.set(index, remainder == null
                    ? null : remainder.getFluidStack());
            }
            if (CompatMods.isAeGasCompatLoaded()) {
                MekEngPatternCompat.returnGasesToNetwork(
                    target, proxy, source);
            }
        } catch (GridAccessException ignored) {
            // Match MMCE's parent behavior when the AE grid disappears.
        }
    }

    private <T extends IAEStack<T>> T insertToNetwork(
        IMEMonitor<T> inventory, T stack) throws GridAccessException {
        return stack == null ? null : Platform.poweredInsert(
            proxy.getEnergy(), inventory, stack.copy(), source);
    }

    @Override
    public void onMachineEvent(MachineEvent event) {
        super.onMachineEvent(event);
        if (event instanceof RecipeFinishEvent
            || event instanceof FactoryRecipeFinishEvent) {
            clearCompletedPatternSlots(event);
        }
    }

    /**
     * The ordinary provider permits mixed patterns in DEFAULT mode. A data
     * provider has only one live interface value, so accepting a second
     * marker before the first buffer is consumed would overwrite its value.
     */
    @Override
    public boolean isBusy() {
        // Crafting-lock state remains global, matching the parent provider.
        // In isolation mode each slot owns its input buffer, so one occupied
        // slot must not make the other 35 slots globally busy.
        return super.isBusy()
            || workMode != WorkModeSetting.ISOLATION_INPUT
                && (activeSharedPattern || !handler.isEmpty());
    }

    @Override
    public void setWorkMode(WorkModeSetting nextMode) {
        if (nextMode == null) {
            return;
        }
        if (workMode == WorkModeSetting.ISOLATION_INPUT
            || nextMode == WorkModeSetting.ISOLATION_INPUT) {
            returnItemsScheduled();
        }
        workMode = nextMode;
        if (nextMode != WorkModeSetting.CRAFTING_LOCK_MODE) {
            machineCompleted = true;
        }
        if (nextMode != WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            currentPattern = null;
            currentPatternIdx = -1;
        }
    }

    @Override
    public void update() {
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        if (++bindingCleanupTicker >= BINDING_CLEANUP_INTERVAL) {
            bindingCleanupTicker = 0;
            pruneInvalidBindings();
            synchronizeControllerBindings();
        }
    }

    /** Removes bindings whose loaded controller block no longer exists. */
    public boolean pruneInvalidBindings() {
        if (getWorld() == null || getWorld().isRemote || boundData.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Iterator<SmartInterfaceData> iterator = boundData.iterator();
        while (iterator.hasNext()) {
            BlockPos controllerPos = iterator.next().getPos();
            if (!getWorld().isBlockLoaded(controllerPos)) {
                continue;
            }
            if (!(getWorld().getTileEntity(controllerPos)
                instanceof TileMultiblockMachineController)) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            markForUpdateSync();
        }
        return changed;
    }

    /**
     * Keeps MMCE's controller-side provider-to-type cache in step with a type
     * chosen by the secondary GUI. Only wrappers already registered with that
     * controller are changed; this method never invents a structure binding.
     */
    public void synchronizeControllerBindings() {
        if (selectedInterfaceType == null || getWorld() == null
            || getWorld().isRemote) {
            return;
        }
        for (SmartInterfaceData data : boundData) {
            BlockPos controllerPos = data.getPos();
            if (!getWorld().isBlockLoaded(controllerPos)) {
                continue;
            }
            TileEntity tile = getWorld().getTileEntity(controllerPos);
            if (!(tile instanceof TileMultiblockMachineController)) {
                continue;
            }
            TileMultiblockMachineController controller =
                (TileMultiblockMachineController) tile;
            Map<TileSmartInterface.SmartInterfaceProvider, String> found =
                controller.getFoundSmartInterfaces();
            for (DataPatternInterfaceProvider provider : allDataProviders) {
                if (found.containsKey(provider)) {
                    found.put(provider, selectedInterfaceType);
                }
            }
        }
    }

    @Override
    public void readProviderNBT(NBTTagCompound compound) {
        /* Keep the parent inventory/pattern format, but restore isolated
         * buffers from their own names when the machine is in isolation. */
        super.readProviderNBT(compound);
        // The current MMCE base serializes its independent isolation handlers
        // through the inherited "components" tag. Clear omitted slots too,
        // because a client sync can change modes without constructing a new
        // tile entity.
        NBTTagCompound components = compound.getCompoundTag("components");
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            String key = "handler#" + slot;
            if (workMode != WorkModeSetting.ISOLATION_INPUT
                || !components.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
                InfItemFluidHandler isolated = (InfItemFluidHandler)
                    combinationComponents.get(slot).getContainerProvider();
                isolated.readFromNBT(new NBTTagCompound(), key);
            }
        }
        activeSharedPattern = false;
        synchronized (activePatternSlots) {
            Arrays.fill(activePatternSlots, false);
        }
        Arrays.fill(patternValues, 0F);
        int[] encodedValues = compound.getIntArray(TAG_PATTERN_VALUES);
        for (int slot = 0;
             slot < Math.min(encodedValues.length, PATTERN_SLOTS); slot++) {
            float value = Float.intBitsToFloat(encodedValues[slot]);
            patternValues[slot] = Float.isFinite(value) ? value : 0F;
        }
        selectedInterfaceType = normalizeType(
            compound.getString(TAG_SELECTED_TYPE));
    }

    @Override
    public NBTTagCompound writeProviderNBT(NBTTagCompound compound) {
        super.writeProviderNBT(compound);
        if (selectedInterfaceType != null) {
            compound.setString(TAG_SELECTED_TYPE, selectedInterfaceType);
        }
        int[] encodedValues = new int[PATTERN_SLOTS];
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            encodedValues[slot] = Float.floatToIntBits(patternValues[slot]);
        }
        compound.setIntArray(TAG_PATTERN_VALUES, encodedValues);
        return compound;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        boundData.clear();
        NBTTagList bindings = compound.getTagList(
            TAG_BOUND_DATA, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < bindings.tagCount(); index++) {
            boundData.add(SmartInterfaceData.deserialize(
                bindings.getCompoundTagAt(index)));
        }
        if (selectedInterfaceType == null && !boundData.isEmpty()) {
            selectedInterfaceType = normalizeType(boundData.get(0).getType());
        }
        if (selectedInterfaceType != null) {
            replaceBoundTypesWithoutNotification(selectedInterfaceType);
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        NBTTagList bindings = new NBTTagList();
        for (SmartInterfaceData data : boundData) {
            bindings.appendTag(data.serialize());
        }
        compound.setTag(TAG_BOUND_DATA, bindings);
    }

    @Override
    public boolean isAllDefault() {
        if (!super.isAllDefault() || selectedInterfaceType != null) {
            return false;
        }
        for (float value : patternValues) {
            if (Float.floatToIntBits(value) != Float.floatToIntBits(0F)) {
                return false;
            }
        }
        for (MachineComponent<?> component : combinationComponents) {
            Object container = component.getContainerProvider();
            if (container instanceof InfItemFluidHandler
                && !((InfItemFluidHandler) container).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void invalidate() {
        activeSharedPattern = false;
        super.invalidate();
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_DATA_PATTERN_PROVIDER == null
            ? ItemStack.EMPTY
            : new ItemStack(ModBlocks.ME_DATA_PATTERN_PROVIDER);
    }

    @Override
    public String getMachineName() {
        return machineName == null ? TRANSLATION_KEY : machineName;
    }

    @Override
    public void setMachineName(String name) {
        machineName = name;
    }

    @Override
    public String getCustomInventoryName() {
        return hasCustomInventoryName()
            ? super.getCustomInventoryName() : TRANSLATION_KEY;
    }

    private int findPatternSlot(ICraftingPatternDetails patternDetails) {
        if (patternDetails == null) {
            return -1;
        }
        // Match MEPatternProvider.pushPattern exactly: in isolation mode the
        // parent routes equal duplicate details to the first matching slot.
        for (int slot = 0; slot < details.length; slot++) {
            if (patternDetails.equals(details[slot])) {
                return slot;
            }
        }
        return -1;
    }

    private void applyPatternValue(int slot) {
        float value = getPatternValue(slot);
        if (selectedInterfaceType == null && !boundData.isEmpty()) {
            selectedInterfaceType = boundData.get(0).getType();
        }
        if (selectedInterfaceType != null) {
            replaceBoundTypesWithoutNotification(selectedInterfaceType);
        }
        for (SmartInterfaceData data : boundData) {
            data.setValue(value);
            notifyDataUpdate(data);
        }
        if (!boundData.isEmpty()) {
            markForUpdateSync();
        }
    }

    /** Mirrors the parent's private acceptance gate before publishing data. */
    private boolean canAcceptPatternBeforeDispatch(
        ICraftingPatternDetails patternDetails, InfItemFluidHandler target) {
        if (patternDetails.isCraftable()
            || !getProxy().isActive() || !getProxy().isPowered()) {
            return false;
        }
        return workMode != WorkModeSetting.ENHANCED_BLOCKING_MODE
            || target.isEmpty() || currentPattern == null
            || currentPattern.equals(patternDetails);
    }

    /** Mirrors MEPatternProvider's private current-pattern bookkeeping. */
    private void handleNewPattern(ICraftingPatternDetails patternDetails) {
        if (workMode == WorkModeSetting.ENHANCED_BLOCKING_MODE) {
            if (!patternDetails.equals(currentPattern)) {
                currentPattern = patternDetails;
                currentPatternIdx = findPatternSlot(patternDetails);
            }
        } else {
            currentPattern = null;
            currentPatternIdx = -1;
        }
    }

    private List<SmartInterfaceData> copyBoundData() {
        List<SmartInterfaceData> snapshot = new ArrayList<>(boundData.size());
        for (SmartInterfaceData data : boundData) {
            snapshot.add(copyData(data));
        }
        return snapshot;
    }

    private void restoreDataState(@Nullable String interfaceType,
                                  List<SmartInterfaceData> dataSnapshot) {
        selectedInterfaceType = interfaceType;
        boundData.clear();
        boundData.addAll(dataSnapshot);
        for (SmartInterfaceData data : boundData) {
            notifyDataUpdate(data);
        }
        markForUpdateSync();
    }

    private boolean isTypeValidForBindings(String type) {
        for (SmartInterfaceData data : boundData) {
            DynamicMachine machine = MachineRegistry.getRegistry()
                .getMachine(data.getParent());
            if (machine == null || !machine.hasSmartInterfaceType(type)) {
                return false;
            }
            if (getWorld() == null) {
                continue;
            }
            if (!getWorld().isBlockLoaded(data.getPos())) {
                continue;
            }
            TileEntity tile = getWorld().getTileEntity(data.getPos());
            if (!(tile instanceof TileMultiblockMachineController)) {
                continue;
            }
            Map<TileSmartInterface.SmartInterfaceProvider, String> found =
                ((TileMultiblockMachineController) tile)
                    .getFoundSmartInterfaces();
            for (Map.Entry<TileSmartInterface.SmartInterfaceProvider, String>
                entry : found.entrySet()) {
                if (type.equals(entry.getValue())
                    && !allDataProviders.contains(entry.getKey())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Configuration stays immutable until all dispatched inputs are consumed. */
    private boolean hasPendingCraftingInputs() {
        if (activeSharedPattern || !handler.isEmpty()) {
            return true;
        }
        for (MachineComponent<?> component : combinationComponents) {
            Object container = component.getContainerProvider();
            if (container instanceof InfItemFluidHandler
                && !((InfItemFluidHandler) container).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears isolation markers after the corresponding recipe has finished.
     * The event context identifies the exact wrappers used by data-only
     * recipes; handlers that carried ordinary inputs are also cleared once
     * their buffers are empty.  This keeps parallel slots independent without
     * making an empty, already-consumed handler look perpetually active.
     */
    private void clearCompletedPatternSlots(MachineEvent event) {
        boolean[] completed = new boolean[PATTERN_SLOTS];
        boolean sharedCompleted = false;
        if (event instanceof RecipeEvent) {
            RecipeCraftingContext context = ((RecipeEvent) event).getContext();
            if (context != null) {
                List<RequirementComponents> current =
                    context.getCurrentComponents();
                if (current != null) {
                    for (RequirementComponents requirement : current) {
                        for (ProcessingComponent<?> component
                            : requirement.components()) {
                            Object provided = component.getProvidedComponent();
                            if (provided instanceof DataPatternInterfaceProvider) {
                                DataPatternInterfaceProvider dataProvider =
                                    (DataPatternInterfaceProvider) provided;
                                int slot = dataProvider.patternSlot;
                                if (dataProvider.owner == this) {
                                    if (slot < 0) {
                                        sharedCompleted = true;
                                    } else if (isPatternSlot(slot)) {
                                        completed[slot] = true;
                                    }
                                }
                            }
                        }
                        if (!sharedCompleted && usesSharedProvider(
                            requirement.components())) {
                            sharedCompleted = true;
                        }
                    }
                }
            }
        }

        boolean changed = false;
        synchronized (activePatternSlots) {
            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                if (!activePatternSlots[slot]) {
                    continue;
                }
                Object container = combinationComponents.get(slot)
                    .getContainerProvider();
                boolean handlerEmpty = container instanceof InfItemFluidHandler
                    && ((InfItemFluidHandler) container).isEmpty();
                if (completed[slot] || handlerEmpty) {
                    activePatternSlots[slot] = false;
                    changed = true;
                }
            }
        }
        if (activeSharedPattern && sharedCompleted) {
            activeSharedPattern = false;
            changed = true;
        }
        if (changed) {
            markForUpdateSync();
        }
    }

    /**
     * Returns true when the selected components include this provider's
     * shared item/fluid buffer.  A finish event is broadcast to every
     * component in a controller, so checking the event context prevents an
     * unrelated parallel recipe from releasing this provider's marker early.
     */
    private boolean usesSharedProvider(
        List<ProcessingComponent<?>> components) {
        for (ProcessingComponent<?> processing : components) {
            if (processing.getProvidedComponent() == primaryDataProvider
                || processing.getProvidedComponent() == handler) {
                return true;
            }
            MachineComponent<?> component = processing.getComponent();
            if (component != null && component.getGroupID() == getGroupId()
                && component.getContainerProvider() == handler) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private String selectEffectiveType(ResourceLocation parent,
                                       String requestedType) {
        String selected = selectedInterfaceType;
        if (selected == null) {
            selectedInterfaceType = normalizeType(requestedType);
            return selectedInterfaceType;
        }
        DynamicMachine machine = MachineRegistry.getRegistry()
            .getMachine(parent);
        return machine != null && machine.hasSmartInterfaceType(selected)
            ? selected : null;
    }

    private void replaceBoundTypes(String type) {
        List<SmartInterfaceData> replacements = new ArrayList<>(
            boundData.size());
        for (SmartInterfaceData data : boundData) {
            SmartInterfaceData replacement = data.getType().equals(type)
                ? data : new SmartInterfaceData(data.getPos(),
                    data.getParent(), type, data.getValue());
            replacements.add(replacement);
        }
        boundData.clear();
        boundData.addAll(replacements);
        for (SmartInterfaceData data : boundData) {
            notifyDataUpdate(data);
        }
    }

    private void replaceBoundTypesWithoutNotification(String type) {
        for (int index = 0; index < boundData.size(); index++) {
            SmartInterfaceData data = boundData.get(index);
            if (!data.getType().equals(type)) {
                boundData.set(index, new SmartInterfaceData(data.getPos(),
                    data.getParent(), type, data.getValue()));
            }
        }
    }

    private void notifyDataUpdate(SmartInterfaceData data) {
        if (getWorld() != null && !getWorld().isRemote
            && getWorld().isBlockLoaded(data.getPos())) {
            TileEntity tile = getWorld().getTileEntity(data.getPos());
            if (tile instanceof TileMultiblockMachineController) {
                new SmartInterfaceUpdateEvent(
                    (TileMultiblockMachineController) tile, getPos(), data)
                    .postEvent();
            }
        }
    }

    private void addMachineData(BlockPos controllerPos,
                                ResourceLocation parent, String type,
                                float defaultValue, boolean override) {
        SmartInterfaceData current = findMachineData(controllerPos);
        if (current != null && !override) {
            return;
        }
        String effectiveType = selectEffectiveType(parent, type);
        if (effectiveType == null) {
            return;
        }
        if (current != null) {
            boundData.remove(current);
        }
        SmartInterfaceData data = new SmartInterfaceData(controllerPos,
            parent, effectiveType, defaultValue);
        boundData.add(data);
        notifyDataUpdate(data);
        markForUpdateSync();
    }

    private void removeMachineData(BlockPos controllerPos) {
        SmartInterfaceData data = findMachineData(controllerPos);
        if (data != null && boundData.remove(data)) {
            markForUpdateSync();
        }
    }

    @Nullable
    private SmartInterfaceData findMachineData(BlockPos controllerPos) {
        for (SmartInterfaceData data : boundData) {
            if (data.getPos().equals(controllerPos)) {
                return data;
            }
        }
        return null;
    }

    @Nullable
    private SmartInterfaceData findMachineData(String type) {
        for (SmartInterfaceData data : boundData) {
            if (data.getType().equals(type)) {
                return data;
            }
        }
        return null;
    }

    private static SmartInterfaceData copyData(SmartInterfaceData data) {
        return new SmartInterfaceData(data.getPos(), data.getParent(),
            data.getType(), data.getValue());
    }

    @Nullable
    private static String normalizeType(@Nullable String type) {
        if (type == null) {
            return null;
        }
        String normalized = type.trim();
        if (normalized.isEmpty()
            || normalized.length() > MAX_INTERFACE_TYPE_LENGTH) {
            return null;
        }
        return normalized;
    }

    private static boolean isPatternSlot(int slot) {
        return slot >= 0 && slot < PATTERN_SLOTS;
    }

    private static void checkPatternSlot(int slot) {
        if (!isPatternSlot(slot)) {
            throw new IndexOutOfBoundsException("Pattern slot: " + slot);
        }
    }

    /** Smart-interface component wrapper for one MMCE recipe group. */
    public static final class DataPatternInterfaceProvider
        extends TileSmartInterface.SmartInterfaceProvider
        implements PrioritySmartInterfaceProvider {

        private final TileMEDataPatternProvider owner;
        private final long groupId;
        private final int patternSlot;
        private final boolean activeOnly;

        private DataPatternInterfaceProvider(
            TileMEDataPatternProvider owner, long groupId, int patternSlot,
            boolean activeOnly) {
            // The MMCE base requires a TileSmartInterface owner. Every method
            // which touches that owner is overridden below.
            super(new TileSmartInterface());
            this.owner = owner;
            this.groupId = groupId;
            this.patternSlot = patternSlot;
            this.activeOnly = activeOnly;
        }

        @Nonnull
        public TileMEDataPatternProvider getOwner() {
            return owner;
        }

        @Nullable
        @Override
        public SmartInterfaceData getMachineData(String type) {
            return viewMachineData(owner.findMachineData(type));
        }

        @Nullable
        @Override
        public SmartInterfaceData getMachineData(BlockPos pos) {
            return viewMachineData(owner.findMachineData(pos));
        }

        @Nullable
        @Override
        public SmartInterfaceData getMachineData(int index) {
            return viewMachineData(
                index >= 0 && index < owner.boundData.size()
                    ? owner.boundData.get(index) : null);
        }

        @Override
        public void addMachineData(BlockPos pos, ResourceLocation parent,
                                   String type, float defaultValue,
                                   boolean override) {
            owner.addMachineData(pos, parent, type, defaultValue, override);
        }

        @Override
        public void removeMachineData(BlockPos pos) {
            owner.removeMachineData(pos);
        }

        public boolean setMachineValue(BlockPos controllerPos, float value) {
            if (!Float.isFinite(value)) {
                return false;
            }
            SmartInterfaceData data = owner.findMachineData(controllerPos);
            if (data == null) {
                return false;
            }
            data.setValue(value);
            owner.notifyDataUpdate(data);
            owner.markForUpdateSync();
            return true;
        }

        @Override
        public int getBoundSize() {
            return owner.boundData.size();
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentTypesMM.COMPONENT_SMART_INTERFACE;
        }

        @Nonnull
        @Override
        public DataPatternInterfaceProvider getContainerProvider() {
            return this;
        }

        @Override
        public long getGroupID() {
            return patternSlot < 0 ? owner.getGroupId() : groupId;
        }

        @Override
        public boolean isPriorityActive() {
            return !activeOnly
                || owner.isDataProviderActiveForGroup(getGroupID());
        }

        @Nullable
        private SmartInterfaceData viewMachineData(
            @Nullable SmartInterfaceData data) {
            if (data == null) {
                return data;
            }
            if (activeOnly
                && !owner.isDataProviderActiveForGroup(getGroupID())) {
                return null;
            }
            if (patternSlot < 0) {
                return data;
            }
            return new SmartInterfaceData(data.getPos(), data.getParent(),
                data.getType(), owner.getPatternValue(patternSlot));
        }
    }
}
