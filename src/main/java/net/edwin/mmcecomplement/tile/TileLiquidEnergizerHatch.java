package net.edwin.mmcecomplement.tile;

import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import net.edwin.mmcecomplement.config.ModConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Long-backed one-tank fluid-to-energy input hatch.
 *
 * <p>Forge 1.12 fluid transactions are int-sized, but the stored amount and
 * configured capacity are longs. Pipes can therefore fill the tank in normal
 * int-sized transactions until a capacity as large as {@link Long#MAX_VALUE}
 * is reached.</p>
 */
public class TileLiquidEnergizerHatch extends TileColorableMachineComponent
    implements MachineComponentTile, IEnergyHandlerAsync, IFluidHandler,
    ITickable {

    private static final String TAG_TIER = "tier";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_FLUID_AMOUNT = "fluidAmount";
    private static final String TAG_FLUID = "fluid";

    private final AtomicLong energy = new AtomicLong();
    private FluidHatchSize tier;
    @Nullable private FluidStack fluidTemplate;
    private long fluidAmount;
    private long clientFluidCapacity = -1L;
    private long clientEnergyCapacity = -1L;
    private long clientConversionRatio = -1L;

    public TileLiquidEnergizerHatch() {
        this(FluidHatchSize.TINY);
    }

    public TileLiquidEnergizerHatch(FluidHatchSize tier) {
        this.tier = tier == null ? FluidHatchSize.TINY : tier;
    }

    public FluidHatchSize getTier() {
        return tier;
    }

    public long getFluidCapacityLong() {
        if (world != null && world.isRemote && clientFluidCapacity > 0L) {
            return clientFluidCapacity;
        }
        return ModConfig.getLiquidEnergizerFluidCapacity(tier);
    }

    public long getFluidAmountLong() {
        return Math.max(0L, fluidAmount);
    }

    public long getConversionRatio() {
        if (world != null && world.isRemote && clientConversionRatio >= 0L) {
            return clientConversionRatio;
        }
        return fluidTemplate == null ? 0L
            : ModConfig.getLiquidEnergizerRatio(fluidTemplate.getFluid());
    }

    @Nullable
    public FluidStack getFluidView() {
        if (fluidTemplate == null || fluidAmount <= 0L) return null;
        FluidStack view = fluidTemplate.copy();
        view.amount = (int) Math.min(Integer.MAX_VALUE, fluidAmount);
        return view;
    }

    @Nullable
    public String getStoredDisplayName() {
        FluidStack view = getFluidView();
        return view == null ? null : view.getLocalizedName();
    }

    public int getStoredTint() {
        FluidStack view = getFluidView();
        return view == null ? 0xFFFFFF : view.getFluid().getColor(view);
    }

    @Nullable
    public ResourceLocation getStoredTexture() {
        FluidStack view = getFluidView();
        return view == null ? null : view.getFluid().getStill(view);
    }

    @Override
    public synchronized int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0
            || ModConfig.getLiquidEnergizerRatio(resource.getFluid()) <= 0L) {
            return 0;
        }
        if (fluidTemplate != null && fluidAmount > 0L
            && !fluidTemplate.isFluidEqual(resource)) {
            return 0;
        }
        long capacity = getFluidCapacityLong();
        long room = fluidAmount >= capacity ? 0L : capacity - fluidAmount;
        int accepted = (int) Math.min((long) resource.amount, room);
        if (accepted <= 0 || !doFill) return accepted;

        boolean typeChanged = fluidTemplate == null || fluidAmount <= 0L;
        if (typeChanged) {
            fluidTemplate = resource.copy();
            fluidTemplate.amount = 1;
        }
        fluidAmount += accepted;
        markNoUpdateSync();
        if (typeChanged) markForUpdateSync();
        return accepted;
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(FluidStack resource,
                                         boolean doDrain) {
        if (resource == null || resource.amount <= 0 || fluidTemplate == null
            || fluidAmount <= 0L || !fluidTemplate.isFluidEqual(resource)) {
            return null;
        }
        return drain(resource.amount, doDrain);
    }

    @Nullable
    @Override
    public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0 || fluidTemplate == null || fluidAmount <= 0L) {
            return null;
        }
        int drainedAmount = (int) Math.min((long) maxDrain, fluidAmount);
        FluidStack drained = fluidTemplate.copy();
        drained.amount = drainedAmount;
        if (doDrain) {
            fluidAmount -= drainedAmount;
            boolean emptied = fluidAmount <= 0L;
            if (emptied) {
                fluidAmount = 0L;
                fluidTemplate = null;
            }
            markNoUpdateSync();
            if (emptied) markForUpdateSync();
        }
        return drained;
    }

    @Override
    public synchronized IFluidTankProperties[] getTankProperties() {
        final FluidStack contents = getFluidView();
        final int visibleCapacity = (int) Math.min(Integer.MAX_VALUE,
            getFluidCapacityLong());
        return new IFluidTankProperties[] { new IFluidTankProperties() {
            @Nullable @Override public FluidStack getContents() {
                return contents == null ? null : contents.copy();
            }
            @Override public int getCapacity() { return visibleCapacity; }
            @Override public boolean canFill() { return true; }
            @Override public boolean canDrain() { return true; }
            @Override public boolean canFillFluidType(FluidStack stack) {
                return stack != null
                    && ModConfig.getLiquidEnergizerRatio(stack.getFluid()) > 0L;
            }
            @Override public boolean canDrainFluidType(FluidStack stack) {
                return stack != null && contents != null
                    && contents.isFluidEqual(stack);
            }
        } };
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

    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        clampEnergyToConfiguredCapacity();
        convertStoredFluid();
    }

    private synchronized void convertStoredFluid() {
        if (fluidTemplate == null || fluidAmount <= 0L) return;
        long ratio = getConversionRatio();
        if (ratio <= 0L) return;

        long capacity = getMaxEnergy();
        while (true) {
            long current = energy.get();
            if (current >= capacity) return;
            long room = capacity - current;
            long convertedMb = Math.min(fluidAmount, room / ratio);
            if (convertedMb <= 0L) return;
            long generated = convertedMb * ratio;
            if (!energy.compareAndSet(current, current + generated)) continue;

            fluidAmount -= convertedMb;
            boolean emptied = fluidAmount <= 0L;
            if (emptied) {
                fluidAmount = 0L;
                fluidTemplate = null;
            }
            markNoUpdateSync();
            if (emptied) markForUpdateSync();
            return;
        }
    }

    private void clampEnergyToConfiguredCapacity() {
        long capacity = getMaxEnergy();
        while (true) {
            long current = energy.get();
            if (current <= capacity) return;
            if (energy.compareAndSet(current, capacity)) {
                markNoUpdateSync();
                return;
            }
        }
    }

    @Override
    public long getCurrentEnergy() {
        return energy.get();
    }

    @Override
    public synchronized void setCurrentEnergy(long amount) {
        energy.set(clamp(amount, 0L, getMaxEnergy()));
        markNoUpdateSync();
    }

    @Override
    public long getMaxEnergy() {
        if (world != null && world.isRemote && clientEnergyCapacity > 0L) {
            return clientEnergyCapacity;
        }
        return ModConfig.getLiquidEnergizerEnergyCapacity(tier);
    }

    @Override
    public synchronized boolean extractEnergy(long amount) {
        if (amount < 0L || energy.get() < amount) return false;
        energy.addAndGet(-amount);
        markNoUpdateSync();
        return true;
    }

    @Override
    public synchronized boolean receiveEnergy(long amount) {
        if (amount < 0L || getRemainingCapacity() < amount) return false;
        energy.addAndGet(amount);
        markNoUpdateSync();
        return true;
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    @Override
    public MachineComponent.EnergyHatch provideComponent() {
        return new MachineComponent.EnergyHatch(IOType.INPUT) {
            @Override
            public IEnergyHandlerAsync getContainerProvider() {
                return TileLiquidEnergizerHatch.this;
            }
        };
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        FluidHatchSize[] tiers = FluidHatchSize.values();
        int savedTier = compound.getInteger(TAG_TIER);
        tier = tiers[Math.max(0, Math.min(tiers.length - 1, savedTier))];
        energy.set(clamp(compound.getLong(TAG_ENERGY), 0L, getMaxEnergy()));
        fluidAmount = Math.max(0L, compound.getLong(TAG_FLUID_AMOUNT));
        fluidTemplate = compound.hasKey(TAG_FLUID)
            ? FluidStack.loadFluidStackFromNBT(compound.getCompoundTag(TAG_FLUID))
            : null;
        if (fluidTemplate == null || fluidAmount <= 0L) {
            fluidTemplate = null;
            fluidAmount = 0L;
        } else {
            fluidTemplate.amount = 1;
        }
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setInteger(TAG_TIER, tier.ordinal());
        compound.setLong(TAG_ENERGY, energy.get());
        compound.setLong(TAG_FLUID_AMOUNT, fluidAmount);
        if (fluidTemplate != null && fluidAmount > 0L) {
            NBTTagCompound fluidTag = new NBTTagCompound();
            FluidStack saved = fluidTemplate.copy();
            saved.amount = 1;
            saved.writeToNBT(fluidTag);
            compound.setTag(TAG_FLUID, fluidTag);
        }
    }

    public void setClientFluidAmount(long amount) {
        if (world != null && world.isRemote) fluidAmount = Math.max(0L, amount);
    }

    public void setClientEnergy(long amount) {
        if (world != null && world.isRemote) energy.set(Math.max(0L, amount));
    }

    public void setClientFluidCapacity(long amount) {
        if (world != null && world.isRemote) {
            clientFluidCapacity = Math.max(1L, amount);
        }
    }

    public void setClientEnergyCapacity(long amount) {
        if (world != null && world.isRemote) {
            clientEnergyCapacity = Math.max(1L, amount);
        }
    }

    public void setClientConversionRatio(long amount) {
        if (world != null && world.isRemote) {
            clientConversionRatio = Math.max(0L, amount);
        }
    }

    private static long clamp(long value, long min, long max) {
        return value < min ? min : Math.min(value, max);
    }
}
