package net.edwin.mmcecomplement.compat.mekanism;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import hellfirepvp.modularmachinery.common.util.HybridGasTank;
import hellfirepvp.modularmachinery.common.util.HybridTank;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import net.edwin.mmcecomplement.cycle.CycleComponentHandler;
import net.edwin.mmcecomplement.tile.TileSelfCycleAssemblyHatch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Mekanism gas-capable self-cycle assembly. */
public class TileSelfCycleAssemblyHatchMekanism
    extends TileSelfCycleAssemblyHatch
    implements IExtendedGasHandler, ITubeConnection {

    @Override
    protected HybridTank createTank(int capacity) {
        return new HybridGasTank(capacity) {
            @Override protected void onContentsChanged() {
                TileSelfCycleAssemblyHatchMekanism.this.onTankContentsChanged();
            }
        };
    }

    private HybridGasTank gasTank(int slot) {
        return (HybridGasTank) tanks[slot];
    }

    @Override protected boolean isTankOccupied(int slot) {
        if (super.isTankOccupied(slot)) return true;
        GasStack gas = gasTank(slot).getGas();
        return gas != null && gas.amount > 0;
    }

    @Override protected boolean canInsertFluid(@Nonnull FluidStack resource) {
        for (int i = 0; i < tanks.length; i++) {
            GasStack gas = gasTank(i).getGas();
            if (gas != null && gas.amount > 0 && gas.getGas().hasFluid()
                && gas.getGas().getFluid() == resource.getFluid()) return false;
        }
        return true;
    }

    @Override protected IFluidHandler createCycleFluidInputHandler() {
        return new CycleGasHandler(super.createCycleFluidInputHandler(), true);
    }

    @Override protected IFluidHandler createCycleFluidOutputHandler() {
        return new CycleGasHandler(super.createCycleFluidOutputHandler(), false);
    }

    @Override public synchronized int receiveGas(EnumFacing side,
                                                  GasStack stack,
                                                  boolean doTransfer) {
        if (stack == null || stack.amount <= 0
            || conflictsWithFluid(stack.getGas())) return 0;
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0
                && stored.isGasEqual(stack))
                return gasTank(i).receiveGas(side, stack, doTransfer);
        }
        for (int i = 0; i < tanks.length; i++) if (!isTankOccupied(i))
            return gasTank(i).receiveGas(side, stack, doTransfer);
        return 0;
    }

    @Nullable @Override public synchronized GasStack drawGas(
        EnumFacing side, int amount, boolean doTransfer) {
        if (amount <= 0) return null;
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0)
                return gasTank(i).drawGas(side, amount, doTransfer);
        }
        return null;
    }

    @Nullable @Override public synchronized GasStack drawGas(
        GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0) return null;
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0
                && stored.isGasEqual(stack))
                return gasTank(i).drawGas(stack, doTransfer);
        }
        return null;
    }

    @Override public synchronized boolean canReceiveGas(EnumFacing side,
                                                         Gas gas) {
        if (gas == null || conflictsWithFluid(gas)) return false;
        return receiveGas(side, new GasStack(gas, 1), false) > 0;
    }

    @Override public synchronized boolean canDrawGas(EnumFacing side, Gas gas) {
        for (int i = 0; i < tanks.length; i++) {
            GasStack stored = gasTank(i).getGas();
            if (stored != null && stored.amount > 0
                && (gas == null || stored.isGasEqual(gas))) return true;
        }
        return false;
    }

    @Override public GasTankInfo[] getTankInfo() {
        GasTankInfo[] result = new GasTankInfo[tanks.length];
        for (int i = 0; i < result.length; i++) {
            GasTankInfo[] info = gasTank(i).getTankInfo();
            result[i] = info[0];
        }
        return result;
    }

    @Override public boolean canTubeConnect(EnumFacing side) { return true; }

    private boolean conflictsWithFluid(Gas gas) {
        if (gas == null || !gas.hasFluid()) return false;
        for (HybridTank tank : tanks) {
            FluidStack fluid = tank.getFluid();
            if (fluid != null && fluid.amount > 0
                && fluid.getFluid() == gas.getFluid()) return true;
        }
        return false;
    }

    private static boolean isGasCapability(Capability<?> capability) {
        if (capability == null) return false;
        String name = capability.getName();
        return IGasHandler.class.getName().equals(name)
            || ITubeConnection.class.getName().equals(name);
    }

    @Override public boolean hasCapability(Capability<?> capability,
                                           @Nullable EnumFacing facing) {
        return isGasCapability(capability)
            || super.hasCapability(capability, facing);
    }

    @Nullable @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability,
                               @Nullable EnumFacing facing) {
        return isGasCapability(capability) ? (T) this
            : super.getCapability(capability, facing);
    }

    @Override public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        for (int i = 0; i < tanks.length; i++) gasTank(i).readGasFromNBT(
            compound.getCompoundTag("fluidTank" + i));
    }

    @Override protected void writeExtraTankNBT(int index, NBTTagCompound tag) {
        gasTank(index).writeGasToNBT(tag);
    }

    @Nullable @Override public String getStoredDisplayName(int index) {
        String fluid = super.getStoredDisplayName(index);
        if (fluid != null) return fluid;
        GasStack gas = gasTank(index).getGas();
        return gas == null || gas.amount <= 0 ? null
            : gas.getGas().getLocalizedName();
    }

    @Override public int getStoredAmount(int index) {
        int fluid = super.getStoredAmount(index);
        if (fluid > 0) return fluid;
        GasStack gas = gasTank(index).getGas();
        return gas == null ? 0 : Math.max(0, gas.amount);
    }

    @Override public int getStoredTint(int index) {
        if (super.getStoredAmount(index) > 0)
            return super.getStoredTint(index);
        GasStack gas = gasTank(index).getGas();
        return gas == null ? 0xFFFFFF : gas.getGas().getTint();
    }

    @Nullable @Override public ResourceLocation getStoredTexture(int index) {
        ResourceLocation fluid = super.getStoredTexture(index);
        if (fluid != null) return fluid;
        GasStack gas = gasTank(index).getGas();
        return gas == null ? null : gas.getGas().getIcon();
    }

    @Override public boolean isGas(int index) {
        GasStack gas = gasTank(index).getGas();
        return super.getStoredAmount(index) <= 0
            && gas != null && gas.amount > 0;
    }

    private final class CycleGasHandler
        implements IFluidHandler, IExtendedGasHandler, CycleComponentHandler {
        private final IFluidHandler fluidDelegate;
        private final boolean input;
        private CycleGasHandler(IFluidHandler fluidDelegate, boolean input) {
            this.fluidDelegate = fluidDelegate;
            this.input = input;
        }
        @Override public Mode mmceComplement$getCycleMode() {
            return input ? Mode.INPUT : Mode.OUTPUT;
        }
        @Override public IFluidTankProperties[] getTankProperties() {
            return fluidDelegate.getTankProperties();
        }
        @Override public int fill(FluidStack resource, boolean doFill) {
            return fluidDelegate.fill(resource, doFill);
        }
        @Nullable @Override public FluidStack drain(FluidStack resource,
                                                     boolean doDrain) {
            return fluidDelegate.drain(resource, doDrain);
        }
        @Nullable @Override public FluidStack drain(int maxDrain,
                                                     boolean doDrain) {
            return fluidDelegate.drain(maxDrain, doDrain);
        }
        @Override public int receiveGas(EnumFacing side, GasStack offered,
                                        boolean doTransfer) {
            if (input) return TileSelfCycleAssemblyHatchMekanism.this
                .receiveGas(side, offered, doTransfer);
            if (offered == null || offered.amount <= 0) return 0;
            for (int slot = 0; slot < getTankCount(); slot++) {
                int allowance = CycleGasRuntime.allowance(
                    TileSelfCycleAssemblyHatchMekanism.this, slot, offered);
                if (allowance <= 0) continue;
                GasStack limited = offered.copy();
                limited.amount = Math.min(allowance, offered.amount);
                int inserted = gasTank(slot).receiveGas(side, limited,
                    doTransfer);
                if (doTransfer && inserted > 0) CycleGasRuntime.consume(
                    TileSelfCycleAssemblyHatchMekanism.this, slot, inserted);
                return inserted;
            }
            return 0;
        }
        @Nullable @Override public GasStack drawGas(EnumFacing side, int amount,
                                                     boolean doTransfer) {
            if (!input) return null;
            int[] before = gasAmounts();
            GasStack result = TileSelfCycleAssemblyHatchMekanism.this
                .drawGas(side, amount, doTransfer);
            if (doTransfer && result != null) recordGasChanges(before, result);
            return result;
        }
        @Nullable @Override public GasStack drawGas(GasStack stack,
                                                     boolean doTransfer) {
            if (!input) return null;
            int[] before = gasAmounts();
            GasStack result = TileSelfCycleAssemblyHatchMekanism.this
                .drawGas(stack, doTransfer);
            if (doTransfer && result != null) recordGasChanges(before, result);
            return result;
        }
        @Override public boolean canReceiveGas(EnumFacing side, Gas gas) {
            if (input) return TileSelfCycleAssemblyHatchMekanism.this
                .canReceiveGas(side, gas);
            return gas != null && receiveGas(side,
                new GasStack(gas, 1), false) > 0;
        }
        @Override public boolean canDrawGas(EnumFacing side, Gas gas) {
            return input && TileSelfCycleAssemblyHatchMekanism.this
                .canDrawGas(side, gas);
        }
        @Override public GasTankInfo[] getTankInfo() {
            if (input) return TileSelfCycleAssemblyHatchMekanism.this
                .getTankInfo();
            List<Gas> gases = GasRegistry.getRegisteredGasses();
            if (gases.isEmpty()) return TileSelfCycleAssemblyHatchMekanism.this
                .getTankInfo();
            final GasStack blocker = new GasStack(gases.get(0),
                getPerTankCapacity());
            GasTankInfo[] blocked = new GasTankInfo[getTankCount()];
            for (int i = 0; i < blocked.length; i++) blocked[i] =
                new GasTankInfo() {
                    @Override public GasStack getGas() { return blocker; }
                    @Override public int getStored() { return blocker.amount; }
                    @Override public int getMaxGas() { return blocker.amount; }
                };
            return blocked;
        }
    }

    private int[] gasAmounts() {
        int[] result = new int[getTankCount()];
        for (int i = 0; i < result.length; i++) {
            GasStack gas = gasTank(i).getGas();
            result[i] = gas == null ? 0 : gas.amount;
        }
        return result;
    }

    private void recordGasChanges(int[] before, GasStack resource) {
        for (int i = 0; i < before.length; i++) {
            GasStack stored = gasTank(i).getGas();
            int after = stored == null ? 0 : stored.amount;
            int extracted = before[i] - after;
            if (extracted <= 0) continue;
            GasStack perSlot = resource.copy();
            perSlot.amount = extracted;
            CycleGasRuntime.record(this, i, perSlot);
        }
    }
}
