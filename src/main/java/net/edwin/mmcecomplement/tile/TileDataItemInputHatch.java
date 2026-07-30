package net.edwin.mmcecomplement.tile;

import github.kasuminova.mmce.common.event.machine.SmartInterfaceUpdateEvent;
import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.TileItemInputBus;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.config.ModConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A normal-sized item input bus with two normal-capacity hybrid input tanks
 * that also behaves as an MMCE smart data interface. The item component is
 * exposed through {@link #provideComponent()}; the fluid/gas and data
 * components are registered separately by the controller/context mixins so
 * every input belongs to the same recipe group.
 */
public class TileDataItemInputHatch extends TileItemInputBus
    implements IFluidHandler {

    private static final String TAG_BOUND_DATA = "boundData";
    private static final String TAG_TANK_PREFIX = "fluidTank";
    private static final String TAG_TIER = "assemblyTier";

    private final List<SmartInterfaceData> boundData = new ArrayList<>();
    private final DataItemInterfaceProvider dataProvider =
        new DataItemInterfaceProvider(this);
    protected HybridTank[] tanks;
    private DataInputAssemblyTier tier;
    private int perTankCapacity;
    private final MachineComponent.FluidHatch fluidProvider;

    public TileDataItemInputHatch() {
        this(DataInputAssemblyTier.NORMAL);
    }

    public TileDataItemInputHatch(DataInputAssemblyTier tier) {
        super(ItemBusSize.NORMAL);
        configureTier(tier, true);
        this.fluidProvider = new MachineComponent.FluidHatch(IOType.INPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return TileDataItemInputHatch.this;
            }

            @Override
            public long getGroupID() {
                return TileDataItemInputHatch.this.getGroupId();
            }
        };
    }

    private void configureTier(DataInputAssemblyTier newTier,
                               boolean rebuildInventory) {
        this.tier = newTier == null
            ? DataInputAssemblyTier.NORMAL : newTier;
        this.perTankCapacity = getConfiguredPerTankCapacity(this.tier);
        this.tanks = new HybridTank[this.tier.getFluidTanks()];
        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = createTank(perTankCapacity);
            tanks[i].setCanFill(true);
            tanks[i].setCanDrain(true);
        }
        if (rebuildInventory) {
            this.inventory = buildInventory(this, this.tier.getItemSlots());
        }
    }

    /**
     * Selects the appropriate configuration group.  The non-data input
     * assembly subclasses share this implementation, but must not inherit
     * the smart-interface assembly's capacity settings.
     */
    protected int getConfiguredPerTankCapacity(DataInputAssemblyTier tier) {
        return this instanceof TileItemInputAssemblyHatch
            ? ModConfig.getInputAssemblyCapacity(tier)
            : ModConfig.getDataInputAssemblyCapacity(tier);
    }

    @Nonnull
    public DataInputAssemblyTier getTier() {
        return tier;
    }

    @Nonnull
    public DataItemInterfaceProvider getDataProvider() {
        return dataProvider;
    }

    @Nonnull
    public MachineComponent.FluidHatch getFluidProvider() {
        return fluidProvider;
    }

    protected HybridTank createTank(int capacity) {
        return new HybridTank(capacity) {
            @Override
            protected void onContentsChanged() {
                TileDataItemInputHatch.this.onTankContentsChanged();
            }
        };
    }

    protected void onTankContentsChanged() {
        markForUpdateSync();
    }

    public int getTankCount() {
        return tanks.length;
    }

    public int getPerTankCapacity() {
        return perTankCapacity;
    }

    public HybridTank getTank(int index) {
        if (index < 0 || index >= tanks.length) {
            throw new IndexOutOfBoundsException("Tank index: " + index);
        }
        return tanks[index];
    }

    @Override
    public void doRestrictedTick() {
        super.doRestrictedTick();
        if (getWorld().isRemote || ticksExisted % 20 != 0 || boundData.isEmpty()) {
            return;
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
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        DataInputAssemblyTier savedTier = compound.hasKey(
            TAG_TIER, Constants.NBT.TAG_INT)
            ? DataInputAssemblyTier.fromMeta(compound.getInteger(TAG_TIER))
            : DataInputAssemblyTier.NORMAL;
        configureTier(savedTier, false);
        super.readCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(compound.getCompoundTag(TAG_TANK_PREFIX + i));
        }
        boundData.clear();
        NBTTagList list = compound.getTagList(
            TAG_BOUND_DATA, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            boundData.add(SmartInterfaceData.deserialize(
                list.getCompoundTagAt(i)));
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger(TAG_TIER, tier.getMetadata());
        for (int i = 0; i < tanks.length; i++) {
            NBTTagCompound tankTag = new NBTTagCompound();
            tanks[i].writeToNBT(tankTag);
            writeExtraTankNBT(i, tankTag);
            compound.setTag(TAG_TANK_PREFIX + i, tankTag);
        }
        NBTTagList list = new NBTTagList();
        for (SmartInterfaceData data : boundData) {
            list.appendTag(data.serialize());
        }
        compound.setTag(TAG_BOUND_DATA, list);
    }

    protected void writeExtraTankNBT(int index, NBTTagCompound tankTag) {
        // The optional Mekanism subclass appends gas data here.
    }

    protected boolean isTankOccupied(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid != null && fluid.amount > 0;
    }

    /** Hook for the Mekanism subclass's cross fluid/gas isolation. */
    protected boolean canInsertFluid(@Nonnull FluidStack resource) {
        return true;
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canInsertFluid(resource)) {
            return 0;
        }
        for (int i = 0; i < tanks.length; i++) {
            FluidStack stored = tanks[i].getFluid();
            if (stored != null && stored.amount > 0
                && stored.isFluidEqual(resource)) {
                return tanks[i].fill(resource, doFill);
            }
        }
        for (int i = 0; i < tanks.length; i++) {
            if (!isTankOccupied(i)) {
                return tanks[i].fill(resource, doFill);
            }
        }
        return 0;
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }
        for (HybridTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored != null && stored.amount > 0
                && stored.isFluidEqual(resource)) {
                return tank.drain(resource, doDrain);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        for (HybridTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored != null && stored.amount > 0) {
                return tank.drain(maxDrain, doDrain);
            }
        }
        return null;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] properties =
            new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            properties[i] = new FluidTankProperties(
                tanks[i].getFluid(), perTankCapacity, true, true);
        }
        return properties;
    }

    public IFluidHandler getTankInteractionHandler(final int index) {
        getTank(index);
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                return new IFluidTankProperties[] {
                    new FluidTankProperties(tanks[index].getFluid(),
                        perTankCapacity, true, true)
                };
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                if (resource == null || resource.amount <= 0
                    || !canInsertFluid(resource)) {
                    return 0;
                }
                for (int i = 0; i < tanks.length; i++) {
                    FluidStack stored = tanks[i].getFluid();
                    if (stored != null && stored.amount > 0
                        && stored.isFluidEqual(resource)) {
                        return i == index
                            ? tanks[index].fill(resource, doFill) : 0;
                    }
                }
                return isTankOccupied(index)
                    ? 0 : tanks[index].fill(resource, doFill);
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                FluidStack stored = tanks[index].getFluid();
                return resource != null && resource.amount > 0
                    && stored != null && stored.amount > 0
                    && stored.isFluidEqual(resource)
                    ? tanks[index].drain(resource, doDrain) : null;
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                return maxDrain <= 0 ? null
                    : tanks[index].drain(maxDrain, doDrain);
            }
        };
    }

    @Override
    public boolean hasCapability(Capability<?> capability,
                                 @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability,
                               @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    @Nullable
    public String getStoredDisplayName(int index) {
        FluidStack fluid = getTank(index).getFluid();
        return fluid == null || fluid.amount <= 0
            ? null : fluid.getLocalizedName();
    }

    public int getStoredAmount(int index) {
        FluidStack fluid = getTank(index).getFluid();
        return fluid == null ? 0 : Math.max(0, fluid.amount);
    }

    public int getStoredTint(int index) {
        FluidStack fluid = getTank(index).getFluid();
        return fluid == null ? 0xFFFFFF : fluid.getFluid().getColor(fluid);
    }

    @Nullable
    public ResourceLocation getStoredTexture(int index) {
        FluidStack fluid = getTank(index).getFluid();
        return fluid == null ? null : fluid.getFluid().getStill(fluid);
    }

    public boolean isGas(int index) {
        return false;
    }

    private void notifyDataUpdate(SmartInterfaceData data) {
        if (getWorld() != null && !getWorld().isRemote) {
            TileEntity tile = getWorld().getTileEntity(data.getPos());
            if (tile instanceof TileMultiblockMachineController) {
                new SmartInterfaceUpdateEvent(
                    (TileMultiblockMachineController) tile, getPos(), data)
                    .postEvent();
            }
        }
        markForUpdateSync();
    }

    /** Smart-interface provider backed by this combined hatch's own NBT. */
    public static final class DataItemInterfaceProvider
        extends TileSmartInterface.SmartInterfaceProvider {

        private final TileDataItemInputHatch owner;

        private DataItemInterfaceProvider(TileDataItemInputHatch owner) {
            // MMCE's provider requires a TileSmartInterface owner. Every
            // stateful method is overridden below, so this inert instance is
            // never attached to a world or used for persistence/sync.
            super(new TileSmartInterface());
            this.owner = owner;
        }

        public TileDataItemInputHatch getOwner() {
            return owner;
        }

        @Nullable
        @Override
        public SmartInterfaceData getMachineData(String type) {
            for (SmartInterfaceData data : owner.boundData) {
                if (data.getType().equals(type)) {
                    return data;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public SmartInterfaceData getMachineData(BlockPos pos) {
            for (SmartInterfaceData data : owner.boundData) {
                if (data.getPos().equals(pos)) {
                    return data;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public SmartInterfaceData getMachineData(int index) {
            return index >= 0 && index < owner.boundData.size()
                ? owner.boundData.get(index)
                : null;
        }

        @Override
        public void addMachineData(BlockPos pos, ResourceLocation parent,
                                   String type, float defaultValue,
                                   boolean override) {
            SmartInterfaceData current = getMachineData(pos);
            if (current != null) {
                if (!override) {
                    return;
                }
                owner.boundData.remove(current);
            }

            SmartInterfaceData data = new SmartInterfaceData(
                pos, parent, type, defaultValue);
            owner.boundData.add(data);
            owner.notifyDataUpdate(data);
        }

        @Override
        public void removeMachineData(BlockPos pos) {
            SmartInterfaceData data = getMachineData(pos);
            if (data != null && owner.boundData.remove(data)) {
                owner.markForUpdateSync();
            }
        }

        public boolean setMachineValue(BlockPos controllerPos, float value) {
            SmartInterfaceData data = getMachineData(controllerPos);
            if (data == null) {
                return false;
            }
            data.setValue(value);
            owner.notifyDataUpdate(data);
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
        public DataItemInterfaceProvider getContainerProvider() {
            return this;
        }

        @Override
        public long getGroupID() {
            return owner.getGroupId();
        }
    }
}
