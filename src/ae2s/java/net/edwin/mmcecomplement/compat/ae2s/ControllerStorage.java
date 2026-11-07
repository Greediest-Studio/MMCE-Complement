package net.edwin.mmcecomplement.compat.ae2s;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.*;
import ae2.api.storage.MEStorage;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.items.IItemHandler;
import java.util.function.Supplier;

/** No cached contents: both AE and pipes mutate the controller's real handlers. */
public final class ControllerStorage implements MEStorage {
    private final Supplier<StorageController> storage;
    private final MEStorage gasStorage;

    public ControllerStorage(Supplier<StorageController> storage) {
        this.storage = storage;
        gasStorage = NativeGasStorageBridge.create(storage);
    }

    @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (key == null || amount <= 0) return 0;
        StorageController controller = storage.get();
        if (controller == null) return 0;
        if (key instanceof AEItemKey) {
            AEItemKey item = (AEItemKey) key;
            IItemHandler handler = controller.mmceComplement$getStorageInventory();
            if (handler == null) return 0;
            long remaining = amount;
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    ItemStack stored = handler.getStackInSlot(slot);
                    if (pass == 0 ? stored.isEmpty() || !item.matches(stored) : !stored.isEmpty()) continue;
                    ItemStack offered = item.toStack((int) Math.min(remaining, Integer.MAX_VALUE));
                    ItemStack rest = handler.insertItem(slot, offered, mode == Actionable.SIMULATE);
                    remaining -= offered.getCount() - rest.getCount();
                }
            }
            return amount - remaining;
        }
        if (key instanceof AEFluidKey) {
            IFluidHandler handler = controller.mmceComplement$getFluidStorage();
            return handler == null ? 0 : handler.fill(((AEFluidKey) key).toStack((int) Math.min(amount, Integer.MAX_VALUE)),
                mode == Actionable.MODULATE);
        }
        return gasStorage == null ? 0 : gasStorage.insert(key, amount, mode, source);
    }

    @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        if (key == null || amount <= 0) return 0;
        StorageController controller = storage.get();
        if (controller == null) return 0;
        if (key instanceof AEItemKey) {
            IItemHandler handler = controller.mmceComplement$getStorageInventory();
            if (handler == null) return 0;
            long remaining = amount;
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                if (((AEItemKey) key).matches(handler.getStackInSlot(slot)))
                    remaining -= handler.extractItem(slot, (int) Math.min(remaining, Integer.MAX_VALUE),
                        mode == Actionable.SIMULATE).getCount();
            }
            return amount - remaining;
        }
        if (key instanceof AEFluidKey) {
            IFluidHandler handler = controller.mmceComplement$getFluidStorage();
            if (handler == null) return 0;
            FluidStack drawn = handler.drain(((AEFluidKey) key).toStack((int) Math.min(amount, Integer.MAX_VALUE)),
                mode == Actionable.MODULATE);
            return drawn == null ? 0 : drawn.amount;
        }
        return gasStorage == null ? 0 : gasStorage.extract(key, amount, mode, source);
    }

    @Override public void getAvailableStacks(KeyCounter out) {
        StorageController controller = storage.get();
        if (controller == null) return;
        IItemHandler items = controller.mmceComplement$getStorageInventory();
        if (items != null) for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) out.add(AEItemKey.of(stack), stack.getCount());
        }
        IFluidHandler fluids = controller.mmceComplement$getFluidStorage();
        if (fluids != null) for (IFluidTankProperties tank : fluids.getTankProperties()) {
            FluidStack stack = tank.getContents();
            if (stack != null && stack.amount > 0) out.add(AEFluidKey.of(stack), stack.amount);
        }
        if (gasStorage != null) gasStorage.getAvailableStacks(out);
    }

    @Override public ITextComponent getDescription() {
        return new TextComponentTranslation("tile.mmce_complement.ae_storage_access_port.name");
    }
}
