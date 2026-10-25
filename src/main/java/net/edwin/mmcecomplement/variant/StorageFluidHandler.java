package net.edwin.mmcecomplement.variant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Independent fluid tanks used by a storage controller. */
public class StorageFluidHandler implements IFluidHandler {
    private final TileEntity owner;
    private final FluidTank[] tanks;
    private final int capacity;

    public StorageFluidHandler(TileEntity owner, int slots, int capacity) {
        this.owner = owner;
        this.capacity = Math.max(1, capacity);
        this.tanks = new FluidTank[Math.max(0, slots)];
        for (int i = 0; i < tanks.length; i++) {
            final StorageFluidHandler self = this;
            tanks[i] = new FluidTank(this.capacity) {
                @Override protected void onContentsChanged() { self.changed(); }
            };
        }
    }

    private void changed() {
        owner.markDirty();
        if (owner instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized)
            ((hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized) owner).markForUpdateSync();
    }

    public int getSlots() { return tanks.length; }
    public int getCapacity() { return capacity; }
    public FluidTank getTank(int index) { return index >= 0 && index < tanks.length ? tanks[index] : null; }
    /** Merge identical fluid+NBT stacks and sort across the entire storage. */
    public void sort() {
        List<FluidGroup> groups = new ArrayList<>();
        for (FluidTank tank : tanks) {
            FluidStack stack = tank.getFluid();
            if (stack == null || stack.amount <= 0) continue;
            FluidGroup matching = null;
            for (FluidGroup group : groups) {
                if (group.prototype.isFluidEqual(stack)) { matching = group; break; }
            }
            if (matching == null) {
                matching = new FluidGroup(stack.copy());
                groups.add(matching);
            }
            matching.amount += stack.amount;
        }
        groups.sort(Comparator.comparing((FluidGroup group) -> group.prototype.getFluid().getName())
            .thenComparing(group -> group.prototype.tag == null ? "" : group.prototype.tag.toString()));
        long required = 0;
        for (FluidGroup group : groups) required += (group.amount + capacity - 1L) / capacity;
        // Do not discard malformed/over-capacity stock from old NBT.
        if (required > tanks.length) return;
        FluidStack[] sorted = new FluidStack[tanks.length];
        int index = 0;
        for (FluidGroup group : groups) {
            long remaining = group.amount;
            while (remaining > 0) {
                FluidStack stack = group.prototype.copy();
                stack.amount = (int) Math.min(capacity, remaining);
                remaining -= stack.amount;
                sorted[index++] = stack;
            }
        }
        boolean modified = false;
        for (int i = 0; i < tanks.length; i++) {
            FluidStack previous = tanks[i].getFluid(), next = sorted[i];
            if (previous == null ? next != null
                : next == null || previous.amount != next.amount || !previous.isFluidEqual(next)) {
                tanks[i].setFluid(next);
                modified = true;
            }
        }
        if (modified) changed();
    }

    private static final class FluidGroup {
        private final FluidStack prototype;
        private long amount;
        private FluidGroup(FluidStack prototype) { this.prototype = prototype; }
    }
    public IFluidHandler getTankInteractionHandler(final int index) {
        final FluidTank tank = getTank(index);
        if (tank == null) return null;
        return new IFluidHandler() {
            @Override public IFluidTankProperties[] getTankProperties() {
                return new IFluidTankProperties[] { new FluidTankProperties(tank.getFluid(), capacity, true, true) };
            }
            @Override public int fill(FluidStack resource, boolean doFill) { return tank.fill(resource, doFill); }
            @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return tank.drain(resource, doDrain); }
            @Override public FluidStack drain(int maxDrain, boolean doDrain) { return tank.drain(maxDrain, doDrain); }
        };
    }
    @Override public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] result = new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) result[i] = new FluidTankProperties(tanks[i].getFluid(), capacity, true, true);
        return result;
    }
    @Override public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        FluidStack remaining = resource.copy();
        // Fill existing stacks first, then empty tanks, including in simulation.
        for (int pass = 0; pass < 2; pass++) {
            for (FluidTank tank : tanks) {
                FluidStack stored = tank.getFluid();
                boolean empty = stored == null || stored.amount <= 0;
                if (pass == 0 ? empty || !stored.isFluidEqual(resource) : !empty) continue;
                remaining.amount -= tank.fill(remaining, doFill);
                if (remaining.amount <= 0) return resource.amount;
            }
        }
        return resource.amount - remaining.amount;
    }
    @Override public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;
        FluidStack remaining = resource.copy();
        for (FluidTank tank : tanks) {
            FluidStack drained = tank.drain(remaining, doDrain);
            if (drained != null) remaining.amount -= drained.amount;
            if (remaining.amount <= 0) break;
        }
        int amount = resource.amount - remaining.amount;
        if (amount <= 0) return null;
        FluidStack result = resource.copy();
        result.amount = amount;
        return result;
    }
    @Override public FluidStack drain(int maxDrain, boolean doDrain) {
        for (FluidTank t : tanks) if (t.getFluid() != null && t.getFluid().amount > 0) return t.drain(maxDrain, doDrain);
        return null;
    }
    public NBTTagCompound serializeNBT() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (FluidTank tank : tanks) list.appendTag(tank.writeToNBT(new NBTTagCompound()));
        root.setTag("Tanks", list);
        return root;
    }
    public void deserializeNBT(NBTTagCompound root) {
        if (root == null || !root.hasKey("Tanks", 9)) return;
        NBTTagList list = root.getTagList("Tanks", 10);
        for (int i = 0; i < tanks.length && i < list.tagCount(); i++) tanks[i].readFromNBT(list.getCompoundTagAt(i));
    }
}
