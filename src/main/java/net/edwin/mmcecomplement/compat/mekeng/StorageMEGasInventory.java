package net.edwin.mmcecomplement.compat.mekeng;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IItemList;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import mekanism.api.gas.GasStack;
import net.edwin.mmcecomplement.compat.ae.storage.StorageMEInventory;
import net.edwin.mmcecomplement.compat.mekanism.StorageGasHandler;
import net.edwin.mmcecomplement.variant.StorageController;
import java.util.function.Supplier;

/** Loaded only when Mekanism and Mekanism Energistics are both available. */
public final class StorageMEGasInventory extends StorageMEInventory<IAEGasStack> {
    private final Supplier<StorageGasHandler> inventory;
    public StorageMEGasInventory(IStorageChannel<IAEGasStack> channel, Supplier<StorageGasHandler> inventory) {
        super(channel);
        this.inventory = inventory;
    }
    public static StorageMEInventory<?> create(Supplier<StorageController> storage) {
        return new StorageMEGasInventory(AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class), () -> {
            StorageController controller = storage.get();
            return controller != null && controller.mmceComplement$hasGasStorage()
                ? (StorageGasHandler) controller.mmceComplement$getGasStorage() : null;
        });
    }
    @Override public IAEGasStack injectItems(IAEGasStack input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) return input;
        StorageGasHandler handler = inventory.get();
        if (handler == null) return input;
        GasStack offered = input.getGasStack().copy();
        offered.amount = (int) Math.min(input.getStackSize(), Integer.MAX_VALUE);
        int accepted = handler.receiveGas(null, offered, mode == Actionable.MODULATE);
        if (mode == Actionable.MODULATE) networkChange(input, accepted);
        long left = input.getStackSize() - accepted;
        return left == 0 ? null : input.copy().setStackSize(left);
    }
    @Override public IAEGasStack extractItems(IAEGasStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) return null;
        StorageGasHandler handler = inventory.get();
        if (handler == null) return null;
        GasStack wanted = request.getGasStack().copy();
        wanted.amount = (int) Math.min(request.getStackSize(), Integer.MAX_VALUE);
        GasStack drawn = handler.drawGas(wanted, mode == Actionable.MODULATE);
        if (drawn == null || drawn.amount <= 0) return null;
        if (mode == Actionable.MODULATE) networkChange(request, -drawn.amount);
        return request.copy().setStackSize(drawn.amount);
    }
    @Override public IItemList<IAEGasStack> getAvailableItems(IItemList<IAEGasStack> out) {
        StorageGasHandler handler = inventory.get();
        if (handler != null) for (int slot = 0; slot < handler.getSlots(); slot++) {
            GasStack stack = handler.getStack(slot);
            if (stack != null && stack.amount > 0) out.add(getChannel().createStack(stack));
        }
        return out;
    }
}
