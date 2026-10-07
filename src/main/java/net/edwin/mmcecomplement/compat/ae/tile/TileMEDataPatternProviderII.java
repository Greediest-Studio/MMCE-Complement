package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeFinishEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeFinishEvent;
import github.kasuminova.mmce.common.event.machine.SmartInterfaceUpdateEvent;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.helper.RequirementComponents;
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

/** 144-slot data-enabled ME pattern provider. */
public class TileMEDataPatternProviderII extends TileMEPatternProviderII
    implements ITickable, IMEDataPatternProvider {

    public static final int PATTERN_SLOTS = TileMEPatternProviderII.PATTERN_SLOTS;
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(
        Tags.MOD_ID, "me_data_pattern_provider_ii");
    public static final String TRANSLATION_KEY =
        "tile.mmce_complement.me_data_pattern_provider_ii";

    private static final String TAG_SELECTED_TYPE = "dataInterfaceType";
    private static final String TAG_PATTERN_VALUES = "dataPatternValues";
    private static final String TAG_BOUND_DATA = "dataPatternBindings";
    private static final int MAX_INTERFACE_TYPE_LENGTH = 128;

    private final float[] patternValues = new float[PATTERN_SLOTS];
    private final boolean[] activePatternSlots = new boolean[PATTERN_SLOTS];
    private final List<SmartInterfaceData> boundData = new ArrayList<>();
    private final DataPatternInterfaceProvider primaryDataProvider;
    private final DataPatternInterfaceProvider sharedDataProvider;
    private final List<DataPatternInterfaceProvider> slotDataProviders;
    private final List<DataPatternInterfaceProvider> allDataProviders;
    private final Map<Long, DataPatternInterfaceProvider> dataProvidersByGroup;

    @Nullable
    private String selectedInterfaceType;
    @Nullable
    private String machineNameIIData;
    private volatile boolean activeSharedPattern;
    private int bindingCleanupTicker;

    public TileMEDataPatternProviderII() {
        primaryDataProvider = new DataPatternInterfaceProvider(
            this, -1L, -1, false);
        sharedDataProvider = new DataPatternInterfaceProvider(
            this, -1L, -1, true);
        List<DataPatternInterfaceProvider> slots = new ArrayList<>(
            PATTERN_SLOTS);
        Map<Long, DataPatternInterfaceProvider> byGroup = new HashMap<>();
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            long groupId = getCombinationComponents().get(slot).getGroupID();
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
    public TileSmartInterface.SmartInterfaceProvider getPrimaryDataProvider() {
        return primaryDataProvider;
    }

    @Nonnull
    @Override
    public List<DataPatternInterfaceProvider> getAllDataProviders() {
        return allDataProviders;
    }

    @Nullable
    @Override
    public DataPatternInterfaceProvider getDataProviderForGroup(long groupId) {
        return getWorkMode() == WorkModeSetting.ISOLATION_INPUT
            ? dataProvidersByGroup.get(groupId)
            : groupId == getGroupId() ? sharedDataProvider : null;
    }

    @Override
    public boolean isDataProviderActiveForGroup(long groupId) {
        if (getWorkMode() != WorkModeSetting.ISOLATION_INPUT) {
            return groupId == getGroupId()
                && (activeSharedPattern || !getInfHandler().isEmpty());
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
        Object container = getCombinationComponents().get(slot)
            .getContainerProvider();
        return container instanceof InfItemFluidHandler
            && !((InfItemFluidHandler) container).isEmpty();
    }

    @Nullable
    public DataPatternInterfaceProvider getDataProviderForSlot(int slot) {
        return slot >= 0 && slot < slotDataProviders.size()
            ? slotDataProviders.get(slot) : null;
    }

    @Nonnull
    public List<DataPatternInterfaceProvider> getDataProvidersForCurrentMode() {
        return getWorkMode() == WorkModeSetting.ISOLATION_INPUT
            ? slotDataProviders : Collections.singletonList(sharedDataProvider);
    }

    public float getPatternValue(int slot) {
        checkPatternSlot(slot);
        return patternValues[slot];
    }

    @Nullable
    @Override
    public String getSelectedInterfaceType() {
        return selectedInterfaceType;
    }

    @Nonnull
    public List<SmartInterfaceData> getBoundData() {
        List<SmartInterfaceData> result = new ArrayList<>(boundData.size());
        for (SmartInterfaceData data : boundData) {
            result.add(copyData(data));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean resetUnsupportedSelectionForController(BlockPos pos) {
        for (SmartInterfaceData data : boundData) {
            if (!data.getPos().equals(pos)) {
                return false;
            }
        }
        boundData.clear();
        selectedInterfaceType = null;
        markForUpdateSync();
        return true;
    }

    public boolean configurePattern(int slot, String interfaceType,
                                    float value) {
        if (!isPatternSlot(slot) || !Float.isFinite(value)
            || getPatterns().getStackInSlot(slot).isEmpty()
            || hasPendingCraftingInputs()) {
            return false;
        }
        String trimmed = interfaceType == null ? "" : interfaceType.trim();
        if (trimmed.length() > MAX_INTERFACE_TYPE_LENGTH) {
            return false;
        }
        String normalized = normalizeType(interfaceType);
        if (normalized != null && !isTypeValidForBindings(normalized)) {
            return false;
        }
        patternValues[slot] = value;
        if (normalized != null && !normalized.equals(selectedInterfaceType)) {
            selectedInterfaceType = normalized;
            replaceBoundTypes(normalized);
        }
        synchronizeControllerBindings();
        markForUpdateSync();
        return true;
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails,
                               InventoryCrafting table) {
        int slot = findPatternSlot(patternDetails);
        if (!isPatternSlot(slot) || table == null || isBusy()) {
            return false;
        }
        if (getWorkMode() == WorkModeSetting.ISOLATION_INPUT) {
            synchronized (activePatternSlots) {
                activePatternSlots[slot] = true;
            }
        } else {
            activeSharedPattern = true;
        }
        applyPatternValue(slot);
        boolean pushed = super.pushPattern(patternDetails, table);
        if (!pushed) {
            if (getWorkMode() == WorkModeSetting.ISOLATION_INPUT) {
                synchronized (activePatternSlots) {
                    activePatternSlots[slot] = false;
                }
            } else {
                activeSharedPattern = false;
            }
        }
        return pushed;
    }

    @Override
    public boolean isBusy() {
        return super.isBusy()
            || getWorkMode() != WorkModeSetting.ISOLATION_INPUT
                && (activeSharedPattern || !getInfHandler().isEmpty());
    }

    @Override
    public void setWorkMode(WorkModeSetting nextMode) {
        super.setWorkMode(nextMode);
        activeSharedPattern = false;
        synchronized (activePatternSlots) {
            Arrays.fill(activePatternSlots, false);
        }
    }

    @Override
    public void onMachineEvent(MachineEvent event) {
        super.onMachineEvent(event);
        if (event instanceof RecipeFinishEvent
            || event instanceof FactoryRecipeFinishEvent) {
            clearCompletedPatternSlots(event);
        }
    }

    @Override
    public void update() {
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        if (++bindingCleanupTicker >= 20) {
            bindingCleanupTicker = 0;
            pruneInvalidBindings();
            synchronizeControllerBindings();
        }
    }

    @Override
    public void readProviderNBT(NBTTagCompound compound) {
        super.readProviderNBT(compound);
        Arrays.fill(patternValues, 0F);
        int[] encoded = compound.getIntArray(TAG_PATTERN_VALUES);
        for (int slot = 0; slot < Math.min(encoded.length, PATTERN_SLOTS); slot++) {
            float value = Float.intBitsToFloat(encoded[slot]);
            patternValues[slot] = Float.isFinite(value) ? value : 0F;
        }
        selectedInterfaceType = normalizeType(
            compound.getString(TAG_SELECTED_TYPE));
        activeSharedPattern = false;
        synchronized (activePatternSlots) {
            Arrays.fill(activePatternSlots, false);
        }
    }

    @Override
    public NBTTagCompound writeProviderNBT(NBTTagCompound compound) {
        super.writeProviderNBT(compound);
        if (selectedInterfaceType != null) {
            compound.setString(TAG_SELECTED_TYPE, selectedInterfaceType);
        }
        int[] encoded = new int[PATTERN_SLOTS];
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            encoded[slot] = Float.floatToIntBits(patternValues[slot]);
        }
        compound.setIntArray(TAG_PATTERN_VALUES, encoded);
        return compound;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        readBindings(compound);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        writeBindings(compound);
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
        return boundData.isEmpty();
    }

    @Override
    public ItemStack getVisualItemStack() {
        return ModBlocks.ME_DATA_PATTERN_PROVIDER_II == null
            ? ItemStack.EMPTY : new ItemStack(ModBlocks.ME_DATA_PATTERN_PROVIDER_II);
    }

    @Override
    public String getMachineName() {
        return machineNameIIData == null ? TRANSLATION_KEY : machineNameIIData;
    }

    @Override
    public void setMachineName(String name) {
        machineNameIIData = name;
    }

    @Override
    public String getCustomInventoryName() {
        return hasCustomInventoryName() ? super.getCustomInventoryName()
            : TRANSLATION_KEY;
    }

    @Override
    public void invalidate() {
        activeSharedPattern = false;
        synchronized (activePatternSlots) {
            Arrays.fill(activePatternSlots, false);
        }
        super.invalidate();
    }

    private void applyPatternValue(int slot) {
        for (SmartInterfaceData data : boundData) {
            data.setValue(getPatternValue(slot));
            notifyDataUpdate(data);
        }
        if (!boundData.isEmpty()) {
            markForUpdateSync();
        }
    }

    private boolean hasPendingCraftingInputs() {
        if (activeSharedPattern || !getInfHandler().isEmpty()) {
            return true;
        }
        for (MachineComponent<?> component : getCombinationComponents()) {
            Object container = component.getContainerProvider();
            if (container instanceof InfItemFluidHandler
                && !((InfItemFluidHandler) container).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void clearCompletedPatternSlots(MachineEvent event) {
        boolean[] completed = new boolean[PATTERN_SLOTS];
        boolean sharedCompleted = false;
        if (event instanceof RecipeEvent) {
            RecipeCraftingContext context = ((RecipeEvent) event).getContext();
            if (context != null && context.getCurrentComponents() != null) {
                for (RequirementComponents requirement :
                    context.getCurrentComponents()) {
                    for (ProcessingComponent<?> component :
                        requirement.components()) {
                        Object provided = component.getProvidedComponent();
                        if (provided instanceof DataPatternInterfaceProvider) {
                            DataPatternInterfaceProvider data =
                                (DataPatternInterfaceProvider) provided;
                            if (data.owner == this) {
                                if (data.patternSlot < 0) {
                                    sharedCompleted = true;
                                } else {
                                    completed[data.patternSlot] = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        synchronized (activePatternSlots) {
            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                if (!activePatternSlots[slot]) {
                    continue;
                }
                Object container = getCombinationComponents().get(slot)
                    .getContainerProvider();
                boolean empty = container instanceof InfItemFluidHandler
                    && ((InfItemFluidHandler) container).isEmpty();
                if (completed[slot] || empty) {
                    activePatternSlots[slot] = false;
                }
            }
        }
        if (activeSharedPattern && sharedCompleted) {
            activeSharedPattern = false;
        }
    }

    public boolean pruneInvalidBindings() {
        if (getWorld() == null || getWorld().isRemote) {
            return false;
        }
        boolean changed = false;
        Iterator<SmartInterfaceData> iterator = boundData.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next().getPos();
            if (getWorld().isBlockLoaded(pos)
                && !(getWorld().getTileEntity(pos)
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

    public void synchronizeControllerBindings() {
        if (selectedInterfaceType == null || getWorld() == null
            || getWorld().isRemote) {
            return;
        }
        for (SmartInterfaceData data : boundData) {
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
            for (TileSmartInterface.SmartInterfaceProvider provider
                : allDataProviders) {
                if (found.containsKey(provider)) {
                    found.put(provider, selectedInterfaceType);
                }
            }
        }
    }

    private boolean isTypeValidForBindings(String type) {
        for (SmartInterfaceData data : boundData) {
            DynamicMachine machine = MachineRegistry.getRegistry()
                .getMachine(data.getParent());
            if (machine == null || !machine.hasSmartInterfaceType(type)) {
                return false;
            }
        }
        return true;
    }

    private void addMachineData(BlockPos pos, ResourceLocation parent,
                                String type, float defaultValue,
                                boolean override) {
        SmartInterfaceData current = findMachineData(pos);
        if (current != null && !override) {
            return;
        }
        String effective = selectedInterfaceType == null
            ? normalizeType(type) : selectedInterfaceType;
        if (effective == null) {
            return;
        }
        if (current != null) {
            boundData.remove(current);
        }
        SmartInterfaceData data = new SmartInterfaceData(pos, parent,
            effective, defaultValue);
        boundData.add(data);
        notifyDataUpdate(data);
        markForUpdateSync();
    }

    private void removeMachineData(BlockPos pos) {
        SmartInterfaceData data = findMachineData(pos);
        if (data != null) {
            boundData.remove(data);
            markForUpdateSync();
        }
    }

    @Nullable
    private SmartInterfaceData findMachineData(BlockPos pos) {
        for (SmartInterfaceData data : boundData) {
            if (data.getPos().equals(pos)) {
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

    private void replaceBoundTypes(String type) {
        for (int i = 0; i < boundData.size(); i++) {
            SmartInterfaceData data = boundData.get(i);
            if (!data.getType().equals(type)) {
                boundData.set(i, new SmartInterfaceData(data.getPos(),
                    data.getParent(), type, data.getValue()));
            }
        }
        for (SmartInterfaceData data : boundData) {
            notifyDataUpdate(data);
        }
    }

    private void readBindings(NBTTagCompound compound) {
        boundData.clear();
        NBTTagList list = compound.getTagList(TAG_BOUND_DATA,
            Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            boundData.add(SmartInterfaceData.deserialize(
                list.getCompoundTagAt(i)));
        }
        if (selectedInterfaceType == null && !boundData.isEmpty()) {
            selectedInterfaceType = normalizeType(boundData.get(0).getType());
        }
        if (selectedInterfaceType != null) {
            replaceBoundTypes(selectedInterfaceType);
        }
    }

    private void writeBindings(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (SmartInterfaceData data : boundData) {
            list.appendTag(data.serialize());
        }
        compound.setTag(TAG_BOUND_DATA, list);
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

    private static SmartInterfaceData copyData(SmartInterfaceData data) {
        return new SmartInterfaceData(data.getPos(), data.getParent(),
            data.getType(), data.getValue());
    }

    @Nullable
    private static String normalizeType(@Nullable String type) {
        if (type == null) {
            return null;
        }
        String result = type.trim();
        return result.isEmpty() || result.length() > MAX_INTERFACE_TYPE_LENGTH
            ? null : result;
    }

    private static boolean isPatternSlot(int slot) {
        return slot >= 0 && slot < PATTERN_SLOTS;
    }

    private static void checkPatternSlot(int slot) {
        if (!isPatternSlot(slot)) {
            throw new IndexOutOfBoundsException("Pattern slot: " + slot);
        }
    }

    public static final class DataPatternInterfaceProvider
        extends TileSmartInterface.SmartInterfaceProvider
        implements PrioritySmartInterfaceProvider {

        private final TileMEDataPatternProviderII owner;
        private final long groupId;
        private final int patternSlot;
        private final boolean activeOnly;

        private DataPatternInterfaceProvider(TileMEDataPatternProviderII owner,
                                             long groupId, int patternSlot,
                                             boolean activeOnly) {
            super(new TileSmartInterface());
            this.owner = owner;
            this.groupId = groupId;
            this.patternSlot = patternSlot;
            this.activeOnly = activeOnly;
        }

        @Override
        public SmartInterfaceData getMachineData(String type) {
            return view(owner.findMachineData(type));
        }

        @Override
        public SmartInterfaceData getMachineData(BlockPos pos) {
            return view(owner.findMachineData(pos));
        }

        @Override
        public SmartInterfaceData getMachineData(int index) {
            return view(index >= 0 && index < owner.boundData.size()
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
        private SmartInterfaceData view(@Nullable SmartInterfaceData data) {
            if (data == null || activeOnly
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
