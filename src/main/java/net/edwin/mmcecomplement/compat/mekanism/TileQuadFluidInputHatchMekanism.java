package net.edwin.mmcecomplement.compat.mekanism;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.util.HybridGasTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import hellfirepvp.modularmachinery.common.block.prop.FluidHatchSize;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import net.edwin.mmcecomplement.tile.TileQuadFluidInputHatch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Optional gas-capable implementation, loaded only when Mekanism is present. */
public class TileQuadFluidInputHatchMekanism extends TileQuadFluidInputHatch
    implements IExtendedGasHandler, ITubeConnection {

    public TileQuadFluidInputHatchMekanism() {
        super();
    }

    public TileQuadFluidInputHatchMekanism(FluidHatchSize hatchSize) {
        super(hatchSize);
    }

    protected TileQuadFluidInputHatchMekanism(FluidHatchSize hatchSize, int tankCount) {
        super(hatchSize, tankCount);
    }

    @Override
    protected HybridTank createTank(int capacity) {
        return new HybridGasTank(capacity) {
            @Override
            protected void onContentsChanged() {
                TileQuadFluidInputHatchMekanism.this.onTankContentsChanged();
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
        for (int i = 0; i < tanks.length; i++) {
            GasStack gas = gasTank(i).getGas();
            if (gas != null && gas.amount > 0 && gas.getGas().hasFluid()
                && gas.getGas().getFluid() == resource.getFluid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0 || conflictsWithFluid(stack.getGas())) {
            return 0;
        }
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && stored.isGasEqual(stack)) {
                return gasTank(i).receiveGas(side, stack, doTransfer);
            }
        }
        for (int i = 0; i < tanks.length; i++) {
            if (!isTankOccupied(i)) {
                return gasTank(i).receiveGas(side, stack, doTransfer);
            }
        }
        return 0;
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
        if (gas == null || conflictsWithFluid(gas)) {
            return false;
        }
        GasStack probe = new GasStack(gas, 1);
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && stored.isGasEqual(gas)) {
                return gasTank(i).receiveGas(side, probe, false) > 0;
            }
        }
        for (int i = 0; i < tanks.length; i++) {
            if (!isTankOccupied(i)) {
                return gasTank(i).canReceiveGas(side, gas);
            }
        }
        return false;
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

    private boolean conflictsWithFluid(Gas gas) {
        if (!gas.hasFluid()) {
            return false;
        }
        for (HybridTank tank : tanks) {
            FluidStack fluid = tank.getFluid();
            if (fluid != null && fluid.amount > 0 && fluid.getFluid() == gas.getFluid()) {
                return true;
            }
        }
        return false;
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
}
