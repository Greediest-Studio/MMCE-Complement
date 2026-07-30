package net.edwin.mmcecomplement.compat.mekanism;

import net.edwin.mmcecomplement.block.prop.DataInputAssemblyTier;
import net.edwin.mmcecomplement.tile.TileItemInputAssemblyHatch;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.util.HybridGasTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Mekanism gas-capable item/fluid-only input assembly. */
public class TileItemInputAssemblyHatchMekanism extends TileItemInputAssemblyHatch
    implements IExtendedGasHandler, ITubeConnection {

    public TileItemInputAssemblyHatchMekanism() { super(); }
    public TileItemInputAssemblyHatchMekanism(DataInputAssemblyTier tier) { super(tier); }

    @Override
    protected HybridTank createTank(int capacity) {
        return new HybridGasTank(capacity) {
            @Override protected void onContentsChanged() {
                TileItemInputAssemblyHatchMekanism.this.onTankContentsChanged();
            }
        };
    }
    private HybridGasTank gasTank(int i) { return (HybridGasTank) tanks[i]; }

    @Override protected boolean isTankOccupied(int i) {
        if (super.isTankOccupied(i)) return true;
        GasStack gas = gasTank(i).getGas();
        return gas != null && gas.amount > 0;
    }
    @Override protected boolean canInsertFluid(@Nonnull FluidStack resource) {
        for (HybridTank tank : tanks) {
            GasStack gas = ((HybridGasTank) tank).getGas();
            if (gas != null && gas.amount > 0 && gas.getGas().hasFluid()
                && gas.getGas().getFluid() == resource.getFluid()) return false;
        }
        return true;
    }
    @Override public synchronized int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0 || conflictsWithFluid(stack.getGas())) return 0;
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && stored.isGasEqual(stack))
                return gasTank(i).receiveGas(side, stack, doTransfer);
        }
        for (int i = 0; i < tanks.length; i++) if (!isTankOccupied(i))
            return gasTank(i).receiveGas(side, stack, doTransfer);
        return 0;
    }
    @Nullable @Override public synchronized GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (amount <= 0) return null;
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0)
                return gasTank(i).drawGas(side, amount, doTransfer);
        }
        return null;
    }
    @Nullable @Override public synchronized GasStack drawGas(GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0) return null;
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && stored.isGasEqual(stack))
                return gasTank(i).drawGas(stack, doTransfer);
        }
        return null;
    }
    @Override public synchronized boolean canReceiveGas(EnumFacing side, Gas gas) {
        if (gas == null || conflictsWithFluid(gas)) return false;
        GasStack probe = new GasStack(gas, 1);
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && stored.isGasEqual(gas))
                return gasTank(i).receiveGas(side, probe, false) > 0;
        }
        for (int i = 0; i < tanks.length; i++) if (!isTankOccupied(i))
            return gasTank(i).canReceiveGas(side, gas);
        return false;
    }
    @Override public synchronized boolean canDrawGas(EnumFacing side, Gas gas) {
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0 && (gas == null || stored.isGasEqual(gas)))
                return gasTank(i).canDrawGas(side, gas);
        }
        return false;
    }
    @Override public GasTankInfo[] getTankInfo() {
        GasTankInfo[] result = new GasTankInfo[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            GasTankInfo[] info = gasTank(i).getTankInfo(); result[i] = info.length == 0 ? null : info[0];
        }
        return result;
    }
    @Override public boolean canTubeConnect(EnumFacing side) { return true; }
    private boolean conflictsWithFluid(Gas gas) {
        if (gas == null || !gas.hasFluid()) return false;
        for (HybridTank tank : tanks) {
            FluidStack fluid = tank.getFluid();
            if (fluid != null && fluid.amount > 0 && fluid.getFluid() == gas.getFluid()) return true;
        }
        return false;
    }
    private static boolean isGasCapability(Capability<?> c) {
        if (c == null) return false; String n = c.getName();
        return IGasHandler.class.getName().equals(n) || ITubeConnection.class.getName().equals(n);
    }
    @Override public boolean hasCapability(Capability<?> c, @Nullable EnumFacing f) {
        return isGasCapability(c) || super.hasCapability(c, f);
    }
    @Nullable @Override @SuppressWarnings("unchecked") public <T> T getCapability(Capability<T> c, @Nullable EnumFacing f) {
        return isGasCapability(c) ? (T) this : super.getCapability(c, f);
    }
    @Override public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) gasTank(i).readGasFromNBT(compound.getCompoundTag("fluidTank" + i));
    }
    @Override protected void writeExtraTankNBT(int index, NBTTagCompound tag) { gasTank(index).writeGasToNBT(tag); }
    @Nullable @Override public String getStoredDisplayName(int i) {
        String f = super.getStoredDisplayName(i); if (f != null) return f;
        GasStack gas = gasTank(i).getGas(); return gas == null || gas.amount <= 0 ? null : gas.getGas().getLocalizedName();
    }
    @Override public int getStoredAmount(int i) {
        int f = super.getStoredAmount(i); if (f > 0) return f;
        GasStack gas = gasTank(i).getGas(); return gas == null ? 0 : Math.max(0, gas.amount);
    }
    @Override public int getStoredTint(int i) {
        if (super.getStoredAmount(i) > 0) return super.getStoredTint(i);
        GasStack gas = gasTank(i).getGas(); return gas == null ? 0xFFFFFF : gas.getGas().getTint();
    }
    @Nullable @Override public ResourceLocation getStoredTexture(int i) {
        ResourceLocation f = super.getStoredTexture(i); if (f != null) return f;
        GasStack gas = gasTank(i).getGas(); return gas == null ? null : gas.getGas().getIcon();
    }
    @Override public boolean isGas(int i) { GasStack gas = gasTank(i).getGas(); return super.getStoredAmount(i) <= 0 && gas != null && gas.amount > 0; }
}
