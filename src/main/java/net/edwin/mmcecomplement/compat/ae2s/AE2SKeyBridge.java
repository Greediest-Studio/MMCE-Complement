package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.stacks.AEKey;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import github.formlessdragon.appcompat.bridge.ae.LegacyAeStorageBridge;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.compat.ae2s.mekeng.AE2SGasKeys;

/** Extends the official item/fluid bridge with optional native MekEng gas keys. */
public final class AE2SKeyBridge {
    private AE2SKeyBridge() {}
    public static AEKey toKey(IAEStack<?> stack) {
        if (CompatMods.isAeGasCompatLoaded() && AE2SGasKeys.isGasStack(stack)) return AE2SGasKeys.toKey(stack);
        return LegacyAeStorageBridge.toKey(stack);
    }
    public static IAEStack<?> toOldStack(AEKey key, long amount) {
        if (CompatMods.isAeGasCompatLoaded() && AE2SGasKeys.isGasKey(key)) return AE2SGasKeys.toOldStack(key, amount);
        return LegacyAeStorageBridge.toOldStack(key, amount);
    }
    public static boolean matchesChannel(IStorageChannel<?> channel, AEKey key) {
        return LegacyAeStorageBridge.matchesChannel(channel, key)
            || CompatMods.isAeGasCompatLoaded() && AE2SGasKeys.matchesChannel(channel, key);
    }
}
