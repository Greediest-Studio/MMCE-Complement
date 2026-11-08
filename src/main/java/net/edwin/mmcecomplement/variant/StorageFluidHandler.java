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
    private FluidTank[] tanks;
    private int capacity;

    public StorageFluidHandler(TileEntity owner, int slots, int capacity) {
        this.owner = owner;
        this.capacity = Math.max(1, capacity);
        this.tanks = new FluidTank[Math.max(0, slots)];
        for (int i = 0; i < tanks.length; i++) tanks[i] = createTank(this.capacity);
    }

    private FluidTank createTank(int amount) {
        return new FluidTank(amount) {
            @Override protected void onContentsChanged() { changed(); }
        };
    }

    public synchronized boolean resize(int slots) {
        if (slots < 1 || slots > StorageCapacitySettings.MAX_SLOTS) return false;
        if (slots == tanks.length) return true;
        for (int i = slots; i < tanks.length; i++) if (tanks[i].getFluidAmount() > 0) return false;
        int old = tanks.length;
        tanks = java.util.Arrays.copyOf(tanks, slots);
        for (int i = old; i < slots; i++) tanks[i] = createTank(capacity);
        changed();
        return true;
    }

    public synchronized boolean setCapacityInSlot(int slot, int amount) {
        if (slot < 0 || slot >= tanks.length || amount <= 0 || amount < tanks[slot].getFluidAmount()) return false;
        if (tanks[slot].getCapacity() != amount) { tanks[slot].setCapacity(amount); changed(); }
        return true;
    }

    public synchronized boolean setCapacity(int amount) {
        if (amount <= 0) return false;
        for (FluidTank tank : tanks) if (tank.getFluidAmount() > amount) return false;
        boolean modified = capacity != amount;
        capacity = amount;
        for (FluidTank tank : tanks) {
            modified |= tank.getCapacity() != amount;
            tank.setCapacity(amount);
        }
        if (modified) changed();
        return true;
    }

    private void changed() {
        owner.markDirty();
        if (owner instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized)
            ((hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized) owner).markForUpdateSync();
        if (owner instanceof StorageController)
            ((StorageController) owner).mmceComplement$markStorageChanged();
    }

    public synchronized int getSlots() { return tanks.length; }
    public synchronized int getCapacity() { return capacity; }
    public synchronized FluidTank getTank(int index) { return index >= 0 && index < tanks.length ? tanks[index] : null; }
    /** Merge identical fluid+NBT stacks and sort across the entire storage. */
    public synchronized void sort() {
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
        FluidStack[] sorted = new FluidStack[tanks.length];
        int index = 0;
        for (FluidGroup group : groups) {
            long remaining = group.amount;
            while (remaining > 0) {
                // Variable-capacity tanks may not fit the requested sort order.
                // Build the entire plan before touching any live contents.
                if (index >= tanks.length) return;
                FluidStack stack = group.prototype.copy();
                stack.amount = (int) Math.min(tanks[index].getCapacity(), remaining);
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
    public synchronized IFluidHandler getTankInteractionHandler(final int index) {
        if (getTank(index) == null) return null;
        final StorageFluidHandler owner = this;
        return new IFluidHandler() {
            @Override public IFluidTankProperties[] getTankProperties() {
                synchronized (owner) {
                FluidTank tank = getTank(index);
                if (tank == null) return new IFluidTankProperties[0];
                FluidStack stack = tank.getFluid();
                return new IFluidTankProperties[] { new FluidTankProperties(stack == null ? null : stack.copy(), tank.getCapacity(), true, true) };
                }
            }
            @Override public int fill(FluidStack resource, boolean doFill) {
                synchronized (owner) { FluidTank tank = getTank(index); return tank == null ? 0 : tank.fill(resource, doFill); }
            }
            @Override public FluidStack drain(FluidStack resource, boolean doDrain) {
                synchronized (owner) { FluidTank tank = getTank(index); return tank == null ? null : tank.drain(resource, doDrain); }
            }
            @Override public FluidStack drain(int maxDrain, boolean doDrain) {
                synchronized (owner) { FluidTank tank = getTank(index); return tank == null ? null : tank.drain(maxDrain, doDrain); }
            }
        };
    }
    @Override public synchronized IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] result = new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            FluidStack stack = tanks[i].getFluid();
            result[i] = new FluidTankProperties(stack == null ? null : stack.copy(), tanks[i].getCapacity(), true, true);
        }
        return result;
    }
    @Override public synchronized int fill(FluidStack resource, boolean doFill) {
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
    @Override public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
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
    @Override public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
        for (FluidTank t : tanks) if (t.getFluid() != null && t.getFluid().amount > 0) return t.drain(maxDrain, doDrain);
        return null;
    }
    public synchronized NBTTagCompound serializeNBT() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (FluidTank tank : tanks) {
            NBTTagCompound tag = tank.writeToNBT(new NBTTagCompound());
            tag.setInteger("SlotCapacity", tank.getCapacity());
            list.appendTag(tag);
        }
        root.setInteger("Slots", tanks.length);
        root.setInteger("Capacity", capacity);
        root.setTag("Tanks", list);
        return root;
    }
    public synchronized void deserializeNBT(NBTTagCompound root) {
        if (root == null || !root.hasKey("Tanks", 9)) return;
        NBTTagList list = root.getTagList("Tanks", 10);
        int slots = root.hasKey("Slots", 3) ? root.getInteger("Slots") : tanks.length;
        if (slots > 0 && slots <= StorageCapacitySettings.MAX_SLOTS && slots != tanks.length) {
            tanks = new FluidTank[slots];
            for (int i = 0; i < slots; i++) tanks[i] = createTank(capacity);
        }
        if (root.getInteger("Capacity") > 0) capacity = root.getInteger("Capacity");
        for (int i = 0; i < tanks.length; i++) {
            NBTTagCompound tag = i < list.tagCount() ? list.getCompoundTagAt(i) : new NBTTagCompound();
            tanks[i].readFromNBT(tag);
            int saved = tag.getInteger("SlotCapacity");
            tanks[i].setCapacity(Math.max(tanks[i].getFluidAmount(), saved > 0 ? saved : capacity));
        }
    }
}
