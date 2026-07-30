package net.edwin.mmcecomplement.compat.mekanism;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.HybridGasTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import net.edwin.mmcecomplement.tile.TileQuadFluidOutputHatch;
import net.edwin.mmcecomplement.fluid.QuadTankRouting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Mekanism gas-capable implementation of the four-slot output hatch. */
public class TileQuadFluidOutputHatchMekanism extends TileQuadFluidOutputHatch
    implements IExtendedGasHandler, ITubeConnection {

    public TileQuadFluidOutputHatchMekanism() {
        super();
    }

    public TileQuadFluidOutputHatchMekanism(FluidHatchSize hatchSize) {
        super(hatchSize);
    }

    protected TileQuadFluidOutputHatchMekanism(FluidHatchSize hatchSize, int tankCount) {
        super(hatchSize, tankCount);
    }

    @Override
    protected HybridTank createTank(int capacity) {
        return new HybridGasTank(capacity) {
            @Override
            protected void onContentsChanged() {
                TileQuadFluidOutputHatchMekanism.this.onTankContentsChanged();
            }
        };
    }

    private HybridGasTank gasTank(int index) {
        return (HybridGasTank) tanks[index];
    }

    @Override
    protected boolean isTankOccupied(int index) {
        if (super.isTankOccupied(index)) {
            return true;
        }
        GasStack gas = gasTank(index).getGas();
        return gas != null && gas.amount > 0;
    }

    @Override
    protected boolean canInsertFluid(@Nonnull FluidStack resource) {
        return true;
    }

    @Override
    public synchronized int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0) {
            return 0;
        }
        boolean[] occupied = new boolean[tanks.length];
        boolean[] matching = new boolean[tanks.length];
        boolean[] hasRoom = new boolean[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            occupied[i] = isTankOccupied(i);
            matching[i] = stored != null && stored.amount > 0
                && stored.isGasEqual(stack);
            hasRoom[i] = stored == null || stored.amount < getPerTankCapacity();
        }
        int target = QuadTankRouting.findOutputFillTarget(occupied, matching, hasRoom);
        return target < 0 ? 0 : gasTank(target).receiveGas(side, stack, doTransfer);
    }

    @Nullable
    @Override
    public synchronized GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (amount <= 0) {
            return null;
        }
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0) {
                return gasTank(i).drawGas(side, amount, doTransfer);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized GasStack drawGas(GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0) {
            return null;
        }
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && stored.isGasEqual(stack)) {
                return gasTank(i).drawGas(stack, doTransfer);
            }
        }
        return null;
    }

    @Override
    public synchronized boolean canReceiveGas(EnumFacing side, Gas gas) {
        if (gas == null) {
            return false;
        }
        GasStack probe = new GasStack(gas, 1);
        boolean[] occupied = new boolean[tanks.length];
        boolean[] matching = new boolean[tanks.length];
        boolean[] hasRoom = new boolean[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            occupied[i] = isTankOccupied(i);
            matching[i] = stored != null && stored.amount > 0
                && stored.isGasEqual(gas);
            hasRoom[i] = stored == null || stored.amount < getPerTankCapacity();
        }
        int target = QuadTankRouting.findOutputFillTarget(occupied, matching, hasRoom);
        return target >= 0 && gasTank(target).canReceiveGas(side, gas);
    }

    @Override
    public synchronized boolean canDrawGas(EnumFacing side, Gas gas) {
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0
                    && (gas == null || stored.isGasEqual(gas))) {
                return gasTank(i).canDrawGas(side, gas);
            }
        }
        return false;
    }

    @Override
    public GasTankInfo[] getTankInfo() {
        GasTankInfo[] info = new GasTankInfo[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            GasTankInfo[] slotInfo = gasTank(i).getTankInfo();
            info[i] = slotInfo.length == 0 ? null : slotInfo[0];
        }
        return info;
    }

    @Override
    public boolean canTubeConnect(EnumFacing side) {
        return true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return isGasCapability(capability) || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (isGasCapability(capability)) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    private static boolean isGasCapability(Capability<?> capability) {
        if (capability == null) {
            return false;
        }
        String name = capability.getName();
        return IGasHandler.class.getName().equals(name)
            || ITubeConnection.class.getName().equals(name);
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) {
            gasTank(i).readGasFromNBT(compound.getCompoundTag("tank" + i));
        }
    }

    @Override
    protected void writeExtraTankNBT(int index, NBTTagCompound tankTag) {
        gasTank(index).writeGasToNBT(tankTag);
    }

    @Nullable
    @Override
    public String getStoredDisplayName(int index) {
        String fluidName = super.getStoredDisplayName(index);
        if (fluidName != null) {
            return fluidName;
        }
        GasStack gas = gasTank(index).getGas();
        return gas == null || gas.amount <= 0 ? null : gas.getGas().getLocalizedName();
    }

    @Override
    public int getStoredAmount(int index) {
        int fluidAmount = super.getStoredAmount(index);
        if (fluidAmount > 0) {
            return fluidAmount;
        }
        GasStack gas = gasTank(index).getGas();
        return gas == null ? 0 : Math.max(0, gas.amount);
    }

    @Override
    public int getStoredTint(int index) {
        if (super.getStoredAmount(index) > 0) {
            return super.getStoredTint(index);
        }
        GasStack gas = gasTank(index).getGas();
        return gas == null ? 0xFFFFFF : gas.getGas().getTint();
    }

    @Nullable
    @Override
    public ResourceLocation getStoredTexture(int index) {
        ResourceLocation fluidTexture = super.getStoredTexture(index);
        if (fluidTexture != null) {
            return fluidTexture;
        }
        GasStack gas = gasTank(index).getGas();
        return gas == null ? null : gas.getGas().getIcon();
    }

    @Override
    public boolean isGas(int index) {
        GasStack gas = gasTank(index).getGas();
        return super.getStoredAmount(index) <= 0 && gas != null && gas.amount > 0;
    }

    @Override
    public MachineComponent<?> provideComponent() {
        return new MachineComponent.FluidHatch(IOType.OUTPUT) {
            @Override
            public IFluidHandler getContainerProvider() {
                return TileQuadFluidOutputHatchMekanism.this;
            }
        };
    }
}
