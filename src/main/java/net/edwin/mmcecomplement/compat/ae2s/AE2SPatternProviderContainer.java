// Adapted from AppliedCompatibility 1.0.1 (5247000), LGPL-3.0-or-later.
// Complement owns the extended layout and the per-slot fluid actions.
package net.edwin.mmcecomplement.compat.ae2s;

import github.formlessdragon.appcompat.common.container.mmce.SyncedFluidList;
import github.formlessdragon.appcompat.common.container.mmce.ContainerMEPatternProvider.PatternStackList;
import appeng.api.storage.data.IAEFluidStack;
import appeng.helpers.InventoryAction;
import net.minecraft.entity.player.EntityPlayerMP;

import github.formlessdragon.appcompat.bridge.mmce.MMCEEnums;
import ae2.api.inventories.PlatformInventoryWrapper;
import ae2.api.stacks.GenericStack;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.container.guisync.PacketWritable;
import ae2.container.slot.AppEngSlot;
import ae2.container.slot.RestrictedInputSlot;
import appeng.fluids.util.AEFluidStack;
import github.formlessdragon.appcompat.bridge.mmce.PatternProviderCacheExtensions;
import github.kasuminova.mmce.common.tile.MEPatternProvider;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.Collections;
import java.util.List;

public class AE2SPatternProviderContainer extends AEBaseContainer {

    private final MEPatternProvider owner;

    @GuiSync(40)
    public int workMode;

    @GuiSync(41)
    public SyncedFluidList subFluid = new SyncedFluidList(Collections.emptyList());

    @GuiSync(42)
    public PatternStackList stackList = new PatternStackList(Collections.emptyList());

    public AE2SPatternProviderContainer(final MEPatternProvider owner, final EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;

        final PlatformInventoryWrapper patterns = new PlatformInventoryWrapper(owner.getPatterns());
        int columns = isExpanded() ? 18 : 9;
        for (int row = 0; row < owner.getPatterns().getSlots() / columns; row++) {
            for (int col = 0; col < columns; col++) {
                this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, patterns, (row * columns) + col, 8 + (col * 18), 28 + (row * 18)), SlotSemantics.ENCODED_PATTERN);
            }
        }

        final PlatformInventoryWrapper subItemHandler = new PlatformInventoryWrapper(owner.getSubItemHandler());
        for (int i = 0; i < owner.getSubItemHandler().getSlots(); i++) {
            int x = isExpanded() ? 343 + (i % 3) * 18 : 181 + i * 18;
            int y = isExpanded() ? 208 + (i / 3) * 18 : 172;
            this.addSlot(new AppEngSlot(subItemHandler, i, x, y), SlotSemantics.STORAGE);
        }

        this.addPlayerInventorySlots(8, isExpanded() ? 186 : 114);

        this.registerClientAction("cycleWorkMode", this::cycleWorkMode);
        this.registerClientAction("returnItems", this::returnItems);
        this.registerClientAction("fluidAction", String.class, this::fluidAction);

        refreshSyncedState();
    }

    private static void appendHandlerStacks(final List<GenericStack> stacks, final InfItemFluidHandler handler) {
        for (final ItemStack stack : handler.getItemStackList()) {
            if (!stack.isEmpty()) {
                stacks.add(GenericStack.fromItemStack(stack));
            }
        }
        for (final FluidStack stack : handler.getFluidStackList()) {
            if (stack != null) {
                stacks.add(GenericStack.fromFluidStack(stack));
            }
        }
        PatternProviderCacheExtensions.appendStacks(stacks, handler);
    }

    private void cycleWorkMode() {
        this.owner.setWorkMode(MMCEEnums.nextWorkMode(this.owner.getWorkMode()));
    }

    private void returnItems() {
        this.owner.returnItemsScheduled();
    }

    private void fluidAction(String encoded) {
        String[] parts = encoded.split(":", -1);
        if (parts.length != 2 || !(getPlayerInventory().player instanceof EntityPlayerMP)) return;
        int slot;
        try { slot = Integer.parseInt(parts[0]); } catch (NumberFormatException ignored) { return; }
        if (slot < 0 || slot >= owner.getSubFluidHandler().getSlots()) return;
        InventoryAction action = "fill".equals(parts[1]) ? InventoryAction.FILL_ITEM : InventoryAction.EMPTY_ITEM;
        EntityPlayerMP player = (EntityPlayerMP) getPlayerInventory().player;
        ItemStack held = player.inventory.getItemStack();
        ItemStack single = held.copy();
        single.setCount(1);
        IFluidHandlerItem itemHandler = FluidUtil.getFluidHandler(single);
        if (itemHandler == null) {
            return;
        }

        AEFluidInventoryUpgradeable tanks = owner.getSubFluidHandler();
        if (action == InventoryAction.FILL_ITEM) {
            IAEFluidStack stored = tanks.getFluidInSlot(slot);
            if (stored == null) {
                return;
            }
            IAEFluidStack unlimited = stored.copy();
            unlimited.setStackSize(Integer.MAX_VALUE);
            int allowed = itemHandler.fill(unlimited.getFluidStack(), false);
            int originalCount = held.getCount();
            for (int i = 0; i < originalCount && allowed > 0; i++) {
                ItemStack container = held.copy();
                container.setCount(1);
                itemHandler = FluidUtil.getFluidHandler(container);
                if (itemHandler == null) {
                    continue;
                }
                FluidStack available = tanks.drain(slot, allowed, false);
                if (available == null || available.amount <= 0) {
                    break;
                }
                int accepted = itemHandler.fill(available, false);
                if (accepted > 0) {
                    FluidStack extracted = tanks.drain(slot, accepted, true);
                    itemHandler.fill(extracted, true);
                }
                replaceOrStoreContainer(player, held, itemHandler.getContainer());
            }
        } else {
            int originalCount = held.getCount();
            for (int i = 0; i < originalCount; i++) {
                ItemStack container = held.copy();
                container.setCount(1);
                itemHandler = FluidUtil.getFluidHandler(container);
                if (itemHandler == null) {
                    continue;
                }
                FluidStack drainable = itemHandler.drain(Integer.MAX_VALUE,
                    false);
                if (drainable == null) {
                    continue;
                }
                int accepted = tanks.fill(slot, drainable, false);
                if (accepted > 0) {
                    FluidStack drained = itemHandler.drain(accepted, true);
                    tanks.fill(slot, drained, true);
                }
                replaceOrStoreContainer(player, held, itemHandler.getContainer());
            }
        }
        player.inventory.markDirty();
        player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
        broadcastChanges();
    }

    private static void replaceOrStoreContainer(EntityPlayerMP player,
                                                ItemStack held,
                                                ItemStack result) {
        if (held.getCount() == 1) {
            player.inventory.setItemStack(result);
            return;
        }
        player.inventory.getItemStack().shrink(1);
        if (!player.inventory.addItemStackToInventory(result)) {
            player.dropItem(result, false);
        }
    }

    public void sendCycleWorkMode() {
        sendClientAction("cycleWorkMode");
    }

    public void sendReturnItems() {
        sendClientAction("returnItems");
    }

    public void sendFluidAction(final int slot, final boolean fill) {
        sendClientAction("fluidAction", slot + ":" + (fill ? "fill" : "empty"));
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            refreshSyncedState();
        }
        super.broadcastChanges();
    }

    public MEPatternProvider getOwner() {
        return this.owner;
    }

    private void refreshSyncedState() {
        this.workMode = this.owner.getWorkMode().ordinal();
        this.subFluid = SyncedFluidList.from(this.owner.getSubFluidHandler());
        this.stackList = new PatternStackList(snapshotStacks());
    }

    private List<GenericStack> snapshotStacks() {
        final List<GenericStack> stacks = new ObjectArrayList<>();
        appendHandlerStacks(stacks, this.owner.getInfHandler());
        for (final MachineComponent<?> component : this.owner.getCombinationComponents()) {
            final Object provider = component.getContainerProvider();
            if (!(provider instanceof InfItemFluidHandler)) {
                throw new IllegalStateException("Unexpected pattern provider cache type: " + provider);
            }
            appendHandlerStacks(stacks, (InfItemFluidHandler) provider);
        }
        return stacks;
    }

    public boolean isExpanded() { return owner.getPatterns().getSlots() > 36; }
}
