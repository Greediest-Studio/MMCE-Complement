package net.edwin.mmcecomplement.compat.mekanism;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.GasTankInfo;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import mekanism.common.util.GasUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.edwin.mmcecomplement.variant.StorageController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Optional Mekanism gas backend. This class is only loaded when Mekanism is present. */
public final class StorageGasHandler implements IExtendedGasHandler {
    private final net.minecraft.tileentity.TileEntity owner;
    private GasTank[] tanks;
    private int capacity;
    public StorageGasHandler(net.minecraft.tileentity.TileEntity owner, int slots, int capacity) {
        this.owner = owner; this.capacity = Math.max(1, capacity);
        this.tanks = new GasTank[Math.max(0, slots)];
        for (int i = 0; i < tanks.length; i++) tanks[i] = new GasTank(this.capacity);
    }
    public synchronized int getSlots() { return tanks.length; }
    public synchronized int getCapacity() { return capacity; }
    public synchronized int getCapacityInSlot(int slot) { return slot >= 0 && slot < tanks.length ? tanks[slot].getMaxGas() : 0; }
    public synchronized boolean resize(int slots) {
        if (slots < 1 || slots > net.edwin.mmcecomplement.variant.StorageCapacitySettings.MAX_SLOTS) return false;
        if (slots == tanks.length) return true;
        for (int i = slots; i < tanks.length; i++) if (tanks[i].getStored() > 0) return false;
        int old = tanks.length;
        tanks = java.util.Arrays.copyOf(tanks, slots);
        for (int i = old; i < slots; i++) tanks[i] = new GasTank(capacity);
        changed();
        return true;
    }
    public synchronized boolean setCapacityInSlot(int slot, int amount) {
        if (slot < 0 || slot >= tanks.length || amount <= 0 || amount < tanks[slot].getStored()) return false;
        if (tanks[slot].getMaxGas() != amount) { tanks[slot].setMaxGas(amount); changed(); }
        return true;
    }
    public synchronized boolean setCapacity(int amount) {
        if (amount <= 0) return false;
        for (GasTank tank : tanks) if (tank.getStored() > amount) return false;
        boolean modified = capacity != amount;
        capacity = amount;
        for (GasTank tank : tanks) { modified |= tank.getMaxGas() != amount; tank.setMaxGas(amount); }
        if (modified) changed();
        return true;
    }
    public synchronized GasStack getStack(int i) { return i >= 0 && i < tanks.length ? tanks[i].getGas() : null; }
    public synchronized int getAmount(int i) { GasStack s = getStack(i); return s == null ? 0 : s.amount; }
    public synchronized net.minecraft.util.ResourceLocation getTexture(int i) { GasStack stack = getStack(i); return stack == null ? null : stack.getGas().getIcon(); }
    public synchronized int getTint(int i) { GasStack stack = getStack(i); return stack == null ? 0xFFFFFF : stack.getGas().getTint(); }
    public synchronized String getDisplayName(int i) { GasStack stack = getStack(i); return stack == null ? null : stack.getGas().getLocalizedName(); }
    /** Merge same-gas tanks, sort by registry name, and leave empty tanks last. */
    public synchronized void sort() {
        List<GasGroup> groups = new ArrayList<>();
        for (GasTank tank : tanks) {
            GasStack stack = tank.getGas();
            if (stack == null || stack.amount <= 0) continue;
            GasGroup matching = null;
            for (GasGroup group : groups) {
                if (group.prototype.isGasEqual(stack)) { matching = group; break; }
            }
            if (matching == null) {
                matching = new GasGroup(stack.copy());
                groups.add(matching);
            }
            matching.amount += stack.amount;
        }
        groups.sort(Comparator.comparing(group -> group.prototype.getGas().getName()));
        GasStack[] sorted = new GasStack[tanks.length];
        int index = 0;
        for (GasGroup group : groups) {
            long remaining = group.amount;
            while (remaining > 0) {
                if (index >= tanks.length) return;
                GasStack stack = group.prototype.copy();
                stack.amount = (int) Math.min(tanks[index].getMaxGas(), remaining);
                remaining -= stack.amount;
                sorted[index++] = stack;
            }
        }
        boolean modified = false;
        for (int i = 0; i < tanks.length; i++) {
            GasStack previous = tanks[i].getGas(), next = sorted[i];
            if (previous == null ? next != null
                : next == null || previous.amount != next.amount || !previous.isGasEqual(next)) {
                tanks[i].setGas(next);
                modified = true;
            }
        }
        if (modified) changed();
    }

    private static final class GasGroup {
        private final GasStack prototype;
        private long amount;
        private GasGroup(GasStack prototype) { this.prototype = prototype; }
    }
    private void changed() {
        owner.markDirty();
        if (owner instanceof hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized)
            ((hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized)owner).markForUpdateSync();
        if (owner instanceof StorageController)
            ((StorageController) owner).mmceComplement$markStorageChanged();
    }
    @Override public synchronized int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        if (stack == null || stack.amount <= 0) return 0;
        GasStack remaining = stack.copy();
        for (int pass = 0; pass < 2; pass++) {
            for (GasTank tank : tanks) {
                GasStack stored = tank.getGas();
                boolean empty = stored == null || stored.amount <= 0;
                if (pass == 0 ? empty || !stored.isGasEqual(stack) : !empty) continue;
                int room = tank.getMaxGas() - tank.getStored();
                if (room <= 0) continue;
                // Mekanism's GasTank adds the full request before clamping.
                // Bound it first so large requests cannot overflow that sum.
                GasStack portion = remaining.copy();
                portion.amount = Math.min(portion.amount, room);
                remaining.amount -= tank.receive(portion, doTransfer);
                if (remaining.amount <= 0) break;
            }
            if (remaining.amount <= 0) break;
        }
        int received = stack.amount - remaining.amount;
        if (doTransfer && received > 0) changed();
        return received;
    }
    @Override public synchronized GasStack drawGas(GasStack request, boolean doTransfer) {
        if (request == null || request.amount <= 0) return null;
        int remaining = request.amount;
        for (GasTank tank : tanks) {
            if (tank.getGas() == null || !tank.getGas().isGasEqual(request)) continue;
            GasStack drawn = tank.draw(remaining, doTransfer);
            if (drawn != null) remaining -= drawn.amount;
            if (remaining <= 0) break;
        }
        int drawn = request.amount - remaining;
        if (drawn <= 0) return null;
        if (doTransfer) changed();
        GasStack result = request.copy();
        result.amount = drawn;
        return result;
    }
    /** Slot-specific script transfers retain the same lock and change notifications. */
    public synchronized int receiveGasInSlot(int slot, GasStack request, boolean doTransfer) {
        if (slot < 0 || slot >= tanks.length || request == null || request.amount <= 0) return 0;
        GasStack portion = request.copy();
        portion.amount = Math.min(portion.amount, tanks[slot].getMaxGas() - tanks[slot].getStored());
        if (portion.amount <= 0) return 0;
        int accepted = tanks[slot].receive(portion, doTransfer);
        if (doTransfer && accepted > 0) changed();
        return accepted;
    }
    public synchronized GasStack drawGasInSlot(int slot, GasStack request, boolean doTransfer) {
        if (slot < 0 || slot >= tanks.length || request == null || request.amount <= 0) return null;
        GasStack stored = tanks[slot].getGas();
        if (stored == null || !stored.isGasEqual(request)) return null;
        GasStack drawn = tanks[slot].draw(request.amount, doTransfer);
        if (doTransfer && drawn != null && drawn.amount > 0) changed();
        return drawn;
    }
    @Override public synchronized GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        if (amount <= 0) return null;
        for (GasTank t : tanks) if (t.getGas() != null && t.getGas().amount > 0) { GasStack s=t.draw(amount, doTransfer); if(doTransfer&&s!=null)changed(); return s; }
        return null;
    }
    @Override public synchronized boolean canReceiveGas(EnumFacing side, mekanism.api.gas.Gas gas) { return gas != null && receiveGas(side, new GasStack(gas, 1), false) > 0; }
    @Override public synchronized boolean canDrawGas(EnumFacing side, mekanism.api.gas.Gas gas) { for(GasTank t:tanks) if(t.getGas()!=null&&t.getGas().amount>0&&(gas==null||t.getGas().getGas()==gas)) return true; return false; }
    @Override public synchronized GasTankInfo[] getTankInfo() { GasTankInfo[] a=new GasTankInfo[tanks.length]; for(int i=0;i<a.length;i++)a[i]=tanks[i].getInfo(); return a; }
    public synchronized NBTTagCompound serializeNBT() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (GasTank tank : tanks) {
            NBTTagCompound tag = tank.write(new NBTTagCompound());
            tag.setInteger("SlotCapacity", tank.getMaxGas());
            list.appendTag(tag);
        }
        root.setTag("Tanks", list);
        root.setInteger("Slots", tanks.length);
        root.setInteger("Capacity", capacity);
        return root;
    }
    public synchronized void deserializeNBT(NBTTagCompound root) {
        if (root == null || !root.hasKey("Tanks", 9)) return;
        NBTTagList list = root.getTagList("Tanks", 10);
        int slots = root.hasKey("Slots", 3) ? root.getInteger("Slots") : tanks.length;
        if (root.getInteger("Capacity") > 0) capacity = root.getInteger("Capacity");
        if (slots > 0 && slots <= net.edwin.mmcecomplement.variant.StorageCapacitySettings.MAX_SLOTS && slots != tanks.length) {
            tanks = new GasTank[slots];
            for (int i = 0; i < slots; i++) tanks[i] = new GasTank(capacity);
        }
        for (int i = 0; i < tanks.length; i++) {
            NBTTagCompound tag = i < list.tagCount() ? list.getCompoundTagAt(i) : new NBTTagCompound();
            tanks[i].setGas(null);
            tanks[i].read(tag);
            int saved = tag.getInteger("SlotCapacity");
            tanks[i].setMaxGas(Math.max(tanks[i].getStored(), saved > 0 ? saved : capacity));
        }
    }
    /** Handles the Mekanism gas container carried by the GUI cursor. */
    public synchronized boolean interact(EntityPlayer player, int index) {
        if (index < 0 || index >= tanks.length) return false;
        ItemStack held = player.inventory.getItemStack();
        if (held.isEmpty() || !GasUtils.isGasItem(held)) return false;
        GasStack contained = GasUtils.getGasContained(held);
        if (contained != null && contained.amount > 0) {
            int accepted = tanks[index].receive(contained.copy(), false);
            if (accepted <= 0) return false;
            GasStack portion = contained.copy();
            portion.amount = accepted;
            accepted = tanks[index].receive(portion, true);
            if (accepted > 0) { GasUtils.useGas(held, contained.getGas(), accepted); changed(); return true; }
            return false;
        }
        GasStack stored = tanks[index].getGas();
        if (stored == null || stored.amount <= 0) return false;
        int accepted = GasUtils.simulateAddGas(held, stored.copy());
        if (accepted <= 0) return false;
        GasStack drained = tanks[index].draw(accepted, true);
        if (drained != null) { GasUtils.addGas(held, drained); changed(); return true; }
        return false;
    }
}
