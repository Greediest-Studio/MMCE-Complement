package net.edwin.mmcecomplement.compat.mekanism;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import net.minecraft.util.EnumFacing;

/** Loaded through the bridge only when gas storage is enabled. */
public final class StorageGasTransfer {
    private StorageGasTransfer() { }

    public static boolean transfer(Object from, Object to, Runnable changed) {
        if (from == to || !(from instanceof IGasHandler) || !(to instanceof IGasHandler)) return false;
        IGasHandler source = (IGasHandler) from, destination = (IGasHandler) to;
        boolean blocked = false;
        for (GasTankInfo tank : source.getTankInfo()) {
            if (tank == null || tank.getGas() == null || tank.getGas().amount <= 0) continue;
            GasStack offered = draw(source, tank.getGas().copy(), false);
            if (offered == null || offered.amount <= 0) continue;
            int accepted = Math.min(offered.amount, destination.receiveGas(null, offered.copy(), false));
            if (accepted > 0) {
                GasStack request = offered.copy();
                request.amount = accepted;
                GasStack extracted = draw(source, request, true);
                if (extracted != null && extracted.amount > 0) {
                    int inserted = destination.receiveGas(null, extracted.copy(), true);
                    if (inserted > 0) changed.run();
                    if (inserted < extracted.amount) {
                        blocked = true;
                        extracted.amount -= inserted;
                        if (source.receiveGas(null, extracted, true) != extracted.amount) {
                            throw new IllegalStateException("Storage gas source refused its own remainder");
                        }
                    }
                }
            }
            blocked |= accepted < offered.amount;
        }
        return blocked;
    }

    private static GasStack draw(IGasHandler handler, GasStack request, boolean transfer) {
        if (handler instanceof IExtendedGasHandler) {
            return ((IExtendedGasHandler) handler).drawGas(request, transfer);
        }
        // Legacy single-tank handlers expose only an untyped draw. Check its
        // simulated gas before committing so a different gas is never removed.
        GasStack probe = handler.drawGas((EnumFacing) null, request.amount, false);
        if (probe == null || !probe.isGasEqual(request)) return null;
        return handler.drawGas((EnumFacing) null, Math.min(request.amount, probe.amount), transfer);
    }
}
