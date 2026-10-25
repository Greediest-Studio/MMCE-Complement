package net.edwin.mmcecomplement.compat.ae.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import java.util.function.Supplier;

public final class StorageMEFluidInventory extends StorageMEInventory<IAEFluidStack> {
    private final Supplier<IFluidHandler> inventory;
    public StorageMEFluidInventory(IStorageChannel<IAEFluidStack> channel, Supplier<IFluidHandler> inventory) {
        super(channel);
        this.inventory = inventory;
    }
    @Override public IAEFluidStack injectItems(IAEFluidStack input, Actionable mode, IActionSource source) {
        if (input == null || input.getStackSize() <= 0) return input;
        IFluidHandler handler = inventory.get();
        if (handler == null) return input;
        // Forge handlers accept ints. Return the remainder for any larger AE request.
        FluidStack offered = input.getFluidStack().copy();
        offered.amount = (int) Math.min(input.getStackSize(), Integer.MAX_VALUE);
        int accepted = handler.fill(offered, mode == Actionable.MODULATE);
        if (mode == Actionable.MODULATE) networkChange(input, accepted);
        long left = input.getStackSize() - accepted;
        return left == 0 ? null : input.copy().setStackSize(left);
    }
    @Override public IAEFluidStack extractItems(IAEFluidStack request, Actionable mode, IActionSource source) {
        if (request == null || request.getStackSize() <= 0) return null;
        IFluidHandler handler = inventory.get();
        if (handler == null) return null;
        FluidStack wanted = request.getFluidStack().copy();
        wanted.amount = (int) Math.min(request.getStackSize(), Integer.MAX_VALUE);
        FluidStack drawn = handler.drain(wanted, mode == Actionable.MODULATE);
        if (drawn == null || drawn.amount <= 0) return null;
        if (mode == Actionable.MODULATE) networkChange(request, -drawn.amount);
        return request.copy().setStackSize(drawn.amount);
    }
    @Override public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out) {
        IFluidHandler handler = inventory.get();
        if (handler != null) for (IFluidTankProperties tank : handler.getTankProperties()) {
            FluidStack stack = tank.getContents();
            if (stack != null && stack.amount > 0) out.add(getChannel().createStack(stack));
        }
        return out;
    }
}
