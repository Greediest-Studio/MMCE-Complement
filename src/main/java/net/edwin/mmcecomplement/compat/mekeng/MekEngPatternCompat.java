package net.edwin.mmcecomplement.compat.mekeng;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.integration.mek.FakeGases;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import mekanism.api.gas.GasStack;
import net.minecraft.item.ItemStack;
import appeng.api.AEApi;

import java.util.List;

/** Mekanism Energistics fake-gas and storage-channel operations. */
public final class MekEngPatternCompat {

    private MekEngPatternCompat() {}

    public static boolean appendFakeGas(ItemStack stack,
                                        InfItemFluidHandler target) {
        if (!FakeGases.isGasFakeItem(stack)) return false;
        GasStack gas = FakeItemRegister.getStack(stack);
        if (gas != null) target.receiveGas(null, gas, true);
        return gas != null;
    }

    @SuppressWarnings("unchecked")
    public static void returnGasesToNetwork(InfItemFluidHandler target,
                                            AENetworkProxy proxy,
                                            IActionSource source)
            throws GridAccessException {
        IGasStorageChannel channel = AEApi.instance().storage()
            .getStorageChannel(IGasStorageChannel.class);
        IMEMonitor<IAEGasStack> inventory =
            proxy.getStorage().getInventory(channel);
        List<GasStack> gases = (List<GasStack>) target.getGasStackList();
        for (int i = 0; i < gases.size(); i++) {
            GasStack stack = gases.get(i);
            if (stack == null) continue;
            IAEGasStack aeStack = channel.createStack(stack);
            IAEGasStack remainder = aeStack == null ? null
                : Platform.poweredInsert(proxy.getEnergy(), inventory,
                    aeStack.copy(), source);
            gases.set(i, remainder == null ? null : remainder.getGasStack());
        }
    }
}
