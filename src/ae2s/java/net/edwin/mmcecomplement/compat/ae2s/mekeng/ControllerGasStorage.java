package net.edwin.mmcecomplement.compat.ae2s.mekeng;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import me.ramidzkh.mekae2.ae2.AEGasKey;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import java.util.function.Supplier;

/** Native gas keys access the same tanks as pipes, controller GUI and scripts. */
public final class ControllerGasStorage implements MEStorage {
    private final Supplier<StorageController> storage;

    public ControllerGasStorage(Supplier<StorageController> storage) { this.storage = storage; }

    private StorageGasHandler handler() {
        StorageController controller = storage.get();
        if (controller == null || !controller.mmceComplement$hasGasStorage()) return null;
        Object handler = controller.mmceComplement$getGasStorage();
        return handler instanceof StorageGasHandler ? (StorageGasHandler) handler : null;
    }

    @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (!(key instanceof AEGasKey) || amount <= 0) return 0;
        StorageGasHandler handler = handler();
        if (handler == null) return 0;
        synchronized (handler) {
            long remaining = amount;
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    GasStack stored = handler.getStack(slot);
                    boolean empty = stored == null || stored.amount <= 0;
                    if (pass == 0 ? empty || stored.getGas() != ((AEGasKey) key).getGas() : !empty) continue;
                    // Requests and totals are long-sized in AE; individual tanks
                    // are int-sized in Mekanism. Visit each tank only once per pass,
                    // including simulations, rather than refilling an unchanged tank.
                    GasStack offered = new GasStack(((AEGasKey) key).getGas(),
                        (int) Math.min(remaining, Integer.MAX_VALUE));
                    remaining -= handler.receiveGasInSlot(slot, offered, mode == Actionable.MODULATE);
                }
            }
            return amount - remaining;
        }
    }

    @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (!(key instanceof AEGasKey) || amount <= 0) return 0;
        StorageGasHandler handler = handler();
        if (handler == null) return 0;
        synchronized (handler) {
            long remaining = amount;
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                GasStack requested = new GasStack(((AEGasKey) key).getGas(),
                    (int) Math.min(remaining, Integer.MAX_VALUE));
                GasStack drawn = handler.drawGasInSlot(slot, requested, mode == Actionable.MODULATE);
                if (drawn != null) remaining -= drawn.amount;
            }
            return amount - remaining;
        }
    }

    @Override public void getAvailableStacks(KeyCounter out) {
        StorageGasHandler handler = handler();
        if (handler == null) return;
        synchronized (handler) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                GasStack stack = handler.getStack(slot);
                if (stack != null && stack.amount > 0) out.add(AEGasKey.of(stack), stack.amount);
            }
        }
    }

    @Override public ITextComponent getDescription() {
        return new TextComponentTranslation("tile.mmce_complement.ae_storage_access_port.name");
    }
}
