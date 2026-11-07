package net.edwin.mmcecomplement.compat.ae2s.mixin;

import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.me.helpers.IGridConnectedTile;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import net.edwin.mmcecomplement.compat.ae2s.AE2SNetwork;
import net.edwin.mmcecomplement.compat.ae2s.ComplementTickingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reuse MMCE's official node, including lifecycle hooks in SRG installation jars. */
@Mixin(value = MEMachineComponent.class, remap = false, priority = 900)
public abstract class MixinMEMachineComponent {
    @org.spongepowered.asm.mixin.Unique private boolean mmceComplement$tickingAttached;

    // State changes also fire while the world is tearing its networks down.
    // MMCE's markForUpdate dispatches to the old bus markNoUpdate override,
    // which asks a separate legacy proxy for a grid already destroyed by AE2S.
    // Persist and synchronize the tile directly; native ticking is registered
    // on the official main node and does not need that legacy wake-up.
    @Inject(method = "onMainNodeStateChanged", at = @At("HEAD"), cancellable = true, require = 1)
    private void mmceComplement$syncNativeState(IGridNodeListener.State reason, CallbackInfo ci) {
        MEMachineComponent tile = (MEMachineComponent) (Object) this;
        tile.markChunkDirty();
        tile.notifyUpdate();
        ci.cancel();
    }

    @Inject(method = {"validate", "func_145829_t"}, at = @At("HEAD"), require = 1)
    private void mmceComplement$attachTicking(CallbackInfo ci) {
        if (!mmceComplement$tickingAttached && getClass().getName().startsWith("net.edwin.mmcecomplement.")
            && this instanceof appeng.api.networking.ticking.IGridTickable) {
            MEMachineComponent tile = (MEMachineComponent) (Object) this;
            mmceComplement$tickingAttached = true;
            AE2SNetwork.mainNode(tile).addService(IGridTickable.class, new ComplementTickingService(tile));
        }
    }

    // AppliedCompatibility 1.0.1 leaves its MCP-only validate/invalidate
    // selectors unmapped. Use both names and guard against duplicate callbacks
    // when a future release fixes those selectors.
    @Inject(method = {"validate", "func_145829_t"}, at = @At("TAIL"), require = 1)
    private void mmceComplement$readyOfficialNode(CallbackInfo ci) {
        MEMachineComponent tile = (MEMachineComponent) (Object) this;
        GridHelper.onFirstTick(tile, component -> {
            IManagedGridNode node = AE2SNetwork.mainNode(component);
            if (node != null && !node.isReady()) {
                node.create(component.getWorld(), component.getPos());
                ((IGridConnectedTile) component).onMainNodeStateChanged(IGridNodeListener.State.GRID_BOOT);
            }
        });
    }

    @Inject(method = {"invalidate", "func_145843_s"}, at = @At("TAIL"), require = 1)
    private void mmceComplement$destroyOfficialNode(CallbackInfo ci) {
        IManagedGridNode node = AE2SNetwork.mainNode((MEMachineComponent) (Object) this);
        if (node != null) node.destroy();
    }
}
