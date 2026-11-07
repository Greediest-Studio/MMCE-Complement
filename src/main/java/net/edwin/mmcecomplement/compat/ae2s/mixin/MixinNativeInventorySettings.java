package net.edwin.mmcecomplement.compat.ae2s.mixin;

import ae2.container.AEBaseContainer;
import ae2.container.guisync.GuiSync;
import io.netty.buffer.ByteBuf;
import net.edwin.mmcecomplement.compat.ae.tile.MEInventoryInputBus;
import net.minecraft.inventory.IContainerListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Synchronize Complement settings through the native container, including its first update. */
@Mixin(value = AEBaseContainer.class, remap = false)
public abstract class MixinNativeInventorySettings {
    @Unique @GuiSync(30000) public boolean mmceComplement$activePull;
    @Unique @GuiSync(30001) public long mmceComplement$permanentReserve;
    @Shadow public abstract Object getTarget();
    @Shadow protected abstract boolean isServerSide();

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void mmceComplement$updateSettings(CallbackInfo ci) {
        mmceComplement$readSettings();
    }

    @Inject(method = {"addListener", "func_75132_a"}, at = @At("HEAD"), require = 1)
    private void mmceComplement$initialSettings(IContainerListener listener, CallbackInfo ci) {
        mmceComplement$readSettings();
    }

    @Unique
    private void mmceComplement$readSettings() {
        Object target = getTarget();
        if (isServerSide() && target instanceof MEInventoryInputBus) {
            MEInventoryInputBus bus = (MEInventoryInputBus) target;
            mmceComplement$activePull = bus.isActivePull();
            mmceComplement$permanentReserve = bus.getPermanentReserve();
        }
    }

    @Inject(method = "receiveServerSyncData", at = @At("RETURN"))
    private void mmceComplement$applySettings(ByteBuf data, CallbackInfo ci) {
        Object target = getTarget();
        if (!isServerSide() && target instanceof MEInventoryInputBus) {
            MEInventoryInputBus bus = (MEInventoryInputBus) target;
            bus.setClientActivePull(mmceComplement$activePull);
            bus.setClientPermanentReserve(mmceComplement$permanentReserve);
        }
    }
}
