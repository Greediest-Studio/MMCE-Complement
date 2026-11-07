package net.edwin.mmcecomplement.compat.ae2s.mekeng;

import ae2.api.inventories.PlatformInventoryWrapper;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.slot.FakeSlot;
import ae2.container.slot.OutputSlot;
import github.kasuminova.mmce.common.tile.base.MEMachineComponent;
import me.ramidzkh.mekae2.ae2.AEGasKey;
import net.edwin.mmcecomplement.compat.ae.tile.MixedMEInputMarker;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEFullExposureAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEInputAssembly;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEOutputAssembly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Native AE2S slot synchronization for mixed item/fluid/gas assemblies. */
public final class AE2SAssemblyContainer extends AEBaseContainer {
    private final MEMachineComponent owner;
    public AE2SAssemblyContainer(MEMachineComponent owner, EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;
        boolean input = owner instanceof TileMEInputAssembly;
        if (input) {
            TileMEInputAssembly tile = (TileMEInputAssembly) owner;
            PlatformInventoryWrapper config = new PlatformInventoryWrapper(tile.getConfigInventory());
            for (int i = 0; i < 16; i++) {
                if (tile instanceof TileMEFullExposureAssembly) addSlot(new DisplaySlot(config, i, 8 + i % 4 * 18, 35 + i / 4 * 18), SlotSemantics.CONFIG);
                else addSlot(new MarkerSlot(tile, config, i, 8 + i % 4 * 18, 35 + i / 4 * 18), SlotSemantics.CONFIG);
            }
        }
        IItemHandlerModifiable display = input ? ((TileMEInputAssembly) owner).getDisplayInventory()
            : ((TileMEOutputAssembly) owner).getDisplayInventory();
        PlatformInventoryWrapper wrapper = new PlatformInventoryWrapper(display);
        for (int i = 0; i < 16; i++) addSlot(new DisplaySlot(wrapper, i,
            (input ? 98 : 52) + i % 4 * 18, 35 + i / 4 * 18), SlotSemantics.STORAGE);
        addPlayerInventorySlots(8, 123);
    }
    public MEMachineComponent getOwner() { return owner; }

    public static ItemStack marker(ItemStack stack) {
        GenericStack generic = GenericStack.unwrapItemStack(stack);
        return generic == null ? MixedMEInputMarker.sanitize(stack) : marker(generic);
    }
    public static ItemStack marker(GenericStack stack) {
        if (stack == null || stack.amount() <= 0) return ItemStack.EMPTY;
        int amount = (int) Math.min(Integer.MAX_VALUE, stack.amount());
        if (stack.what() instanceof AEItemKey) return ((AEItemKey) stack.what()).toStack(amount);
        if (stack.what() instanceof AEFluidKey) return MixedMEInputMarker.fluid(((AEFluidKey) stack.what()).toStack(amount));
        if (stack.what() instanceof AEGasKey) return MixedMEInputMarker.gas(((AEGasKey) stack.what()).toStack(amount));
        return ItemStack.EMPTY;
    }
    public static final class MarkerSlot extends FakeSlot {
        private final TileMEInputAssembly owner;
        private MarkerSlot(TileMEInputAssembly owner, PlatformInventoryWrapper inventory, int index, int x, int y) {
            super(inventory, index, x, y); this.owner = owner; setNotDraggable();
        }
        @Override public int getSlotStackLimit() { return Integer.MAX_VALUE; }
        @Override public boolean canSetFilterTo(ItemStack stack) { return stack.isEmpty() || !marker(stack).isEmpty(); }
        @Override public void putStack(ItemStack stack) { owner.setMarker(getSlotIndex(), marker(stack)); }
        @Override public void increase(ItemStack stack) { putStack(stack); }
        @Override public void decrease(ItemStack stack) { putStack(stack); }
        @Override public void setGenericFilter(GenericStack stack) { putStack(marker(stack)); }
    }
    private static final class DisplaySlot extends OutputSlot {
        private DisplaySlot(PlatformInventoryWrapper inventory, int index, int x, int y) { super(inventory, index, x, y, null); }
        @Override public boolean canTakeStack(EntityPlayer player) { return false; }
    }
}
