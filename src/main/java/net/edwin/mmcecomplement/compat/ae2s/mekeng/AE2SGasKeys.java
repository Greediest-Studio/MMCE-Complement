package net.edwin.mmcecomplement.compat.ae2s.mekeng;

import ae2.api.stacks.AEKey;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import me.ramidzkh.mekae2.ae2.AEGasKey;

/** Kept separate so installing MekEng remains optional. */
public final class AE2SGasKeys {
    private AE2SGasKeys() {}
    public static boolean isGasStack(IAEStack<?> stack) { return stack instanceof IAEGasStack; }
    public static boolean isGasKey(AEKey key) { return key instanceof AEGasKey; }
    public static AEKey toKey(IAEStack<?> stack) { return AEGasKey.of(((IAEGasStack) stack).getGasStack()); }
    public static IAEStack<?> toOldStack(AEKey key, long amount) {
        // The native key's GasStack is int-sized; the legacy AE quantity is long.
        return AEGasStack.of(((AEGasKey) key).toStack(1)).setStackSize(amount);
    }
    public static boolean matchesChannel(IStorageChannel<?> channel, AEKey key) {
        return channel instanceof IGasStorageChannel && key instanceof AEGasKey;
    }
}
