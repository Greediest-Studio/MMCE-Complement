package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.ItemBusSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.TileItemOutputBus;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.config.ModConfig;
import net.edwin.mmcecomplement.fluid.QuadTankRouting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Item/fluid output assembly with five tiered capacities. */
public class TileItemOutputAssemblyHatch extends TileItemOutputBus
    implements IFluidHandler {

    private static final String TAG_TIER = "assemblyTier";
    private static final String TAG_TANK_PREFIX = "fluidTank";

    protected HybridTank[] tanks;
    private DataInputAssemblyTier tier;
    private int perTankCapacity;
    private final MachineComponent.FluidHatch fluidProvider;

    public TileItemOutputAssemblyHatch() {
        this(DataInputAssemblyTier.NORMAL);
    }

    public TileItemOutputAssemblyHatch(DataInputAssemblyTier tier) {
        super(ItemBusSize.NORMAL);
        configureTier(tier, true);
        this.fluidProvider = new MachineComponent.FluidHatch(IOType.OUTPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return TileItemOutputAssemblyHatch.this;
            }
        };
    }

    private void configureTier(DataInputAssemblyTier newTier,
                                boolean rebuildInventory) {
        this.tier = newTier == null ? DataInputAssemblyTier.NORMAL : newTier;
        this.perTankCapacity = ModConfig.getInputAssemblyCapacity(this.tier);
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

    protected HybridTank createTank(int capacity) {
        return new HybridTank(capacity) {
            @Override
            protected void onContentsChanged() {
                TileItemOutputAssemblyHatch.this.markForUpdateSync();
            }
        };
    }

    public DataInputAssemblyTier getTier() { return tier; }
    public int getTankCount() { return tanks.length; }
    public int getPerTankCapacity() { return perTankCapacity; }
    public HybridTank getTank(int index) {
        if (index < 0 || index >= tanks.length) {
            throw new IndexOutOfBoundsException("Tank index: " + index);
        }
        return tanks[index];
    }
    public MachineComponent.FluidHatch getFluidProvider() { return fluidProvider; }

    protected boolean isTankOccupied(int index) {
        FluidStack fluid = tanks[index].getFluid();
        return fluid != null && fluid.amount > 0;
    }

    protected boolean canInsertFluid(@Nonnull FluidStack resource) {
        return true;
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canInsertFluid(resource)) {
            return 0;
        }
        boolean[] occupied = new boolean[tanks.length];
        boolean[] matching = new boolean[tanks.length];
        boolean[] hasRoom = new boolean[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            FluidStack stored = tanks[i].getFluid();
            occupied[i] = isTankOccupied(i);
            matching[i] = stored != null && stored.amount > 0
                && stored.isFluidEqual(resource);
            hasRoom[i] = tanks[i].getFluidAmount() < perTankCapacity;
        }
        int target = QuadTankRouting.findOutputFillTarget(
            occupied, matching, hasRoom);
        return target < 0 ? 0 : tanks[target].fill(resource, doFill);
    }

    public IFluidHandler getTankInteractionHandler(final int index) {
        getTank(index);
        return new IFluidHandler() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                return new IFluidTankProperties[] { new FluidTankProperties(
                    tanks[index].getFluid(), perTankCapacity, true, true) };
            }
            @Override
            public int fill(FluidStack resource, boolean doFill) {
                if (resource == null || resource.amount <= 0
                    || !canInsertFluid(resource)) return 0;
                FluidStack stored = tanks[index].getFluid();
                if (stored != null && stored.amount > 0
                    && !stored.isFluidEqual(resource)) return 0;
                return tanks[index].fill(resource, doFill);
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
                return maxDrain <= 0 || !isTankOccupied(index)
                    ? null : tanks[index].drain(maxDrain, doDrain);
            }
        };
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;
        for (HybridTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored != null && stored.amount > 0
                && stored.isFluidEqual(resource)) return tank.drain(resource, doDrain);
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;
        for (HybridTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored != null && stored.amount > 0) return tank.drain(maxDrain, doDrain);
        }
        return null;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] properties = new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            properties[i] = new FluidTankProperties(
                tanks[i].getFluid(), perTankCapacity, true, true);
        }
        return properties;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            ? (T) this : super.getCapability(capability, facing);
    }

    @Nullable
    public String getStoredDisplayName(int index) {
        FluidStack fluid = getTank(index).getFluid();
        return fluid == null || fluid.amount <= 0 ? null : fluid.getLocalizedName();
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
    public boolean isGas(int index) { return false; }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        DataInputAssemblyTier saved = compound.hasKey(TAG_TIER)
            ? DataInputAssemblyTier.fromMeta(compound.getInteger(TAG_TIER))
            : DataInputAssemblyTier.NORMAL;
        // Rebuild the tier-sized inventory before the superclass restores its
        // item contents; no-arg tile construction starts at the normal tier.
        configureTier(saved, true);
        super.readCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(compound.getCompoundTag(TAG_TANK_PREFIX + i));
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger(TAG_TIER, tier.getMetadata());
        for (int i = 0; i < tanks.length; i++) {
            NBTTagCompound tag = new NBTTagCompound();
            tanks[i].writeToNBT(tag);
            writeExtraTankNBT(i, tag);
            compound.setTag(TAG_TANK_PREFIX + i, tag);
        }
    }

    protected void writeExtraTankNBT(int index, NBTTagCompound tag) { }
}
