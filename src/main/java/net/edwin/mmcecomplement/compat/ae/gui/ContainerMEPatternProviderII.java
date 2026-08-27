package net.edwin.mmcecomplement.compat.ae.gui;

import appeng.api.storage.data.IAEFluidStack;
import appeng.container.slot.SlotNormal;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.InventoryAction;
import appeng.tile.inventory.AppEngInternalInventory;
import github.kasuminova.mmce.common.container.ContainerMEPatternProvider;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import net.edwin.mmcecomplement.compat.ae.tile.TileMEPatternProviderII;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/** Large slot layout while retaining MMCE's action packet contract. */
public class ContainerMEPatternProviderII extends ContainerMEPatternProvider {

    public static final int PLAYER_INVENTORY_Y = 186;
    public static final int SUB_STORAGE_X = 343;
    public static final int SUB_STORAGE_Y = 208;

    private final TileMEPatternProviderII ownerII;

    public ContainerMEPatternProviderII(TileMEPatternProviderII owner,
                                        EntityPlayer player) {
        super(owner, player);
        this.ownerII = owner;

        inventorySlots.clear();
        inventoryItemStacks.clear();
        bindPlayerInventory(getInventoryPlayer(), 0, PLAYER_INVENTORY_Y);

        AppEngInternalInventory patterns = owner.getPatterns();
        for (int row = 0; row < TileMEPatternProviderII.PATTERN_ROWS; row++) {
            for (int column = 0;
                 column < TileMEPatternProviderII.PATTERN_COLUMNS; column++) {
                addSlotToContainer(new SlotRestrictedInput(
                    SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
                    patterns,
                    row * TileMEPatternProviderII.PATTERN_COLUMNS + column,
                    8 + column * 18, 28 + row * 18,
                    getInventoryPlayer()));
            }
        }

        AppEngInternalInventory subItems = owner.getSubItemHandler();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = row * 3 + column;
                addSlotToContainer(new SlotNormal(subItems, slot,
                    SUB_STORAGE_X + column * 18,
                    SUB_STORAGE_Y + row * 18));
            }
        }
    }

    @Override
    public void doAction(EntityPlayerMP player, InventoryAction action,
                         int slot, long id) {
        if (action != InventoryAction.FILL_ITEM
            && action != InventoryAction.EMPTY_ITEM) {
            super.doAction(player, action, slot, id);
            return;
        }
        if (slot < 0 || slot >= TileMEPatternProviderII.SUB_FLUID_SLOTS) {
            return;
        }

        ItemStack held = player.inventory.getItemStack();
        ItemStack single = held.copy();
        single.setCount(1);
        IFluidHandlerItem itemHandler = FluidUtil.getFluidHandler(single);
        if (itemHandler == null) {
            return;
        }

        AEFluidInventoryUpgradeable tanks = ownerII.getSubFluidHandler();
        if (action == InventoryAction.FILL_ITEM) {
            IAEFluidStack stored = tanks.getFluidInSlot(slot);
            if (stored == null) {
                return;
            }
            IAEFluidStack unlimited = stored.copy();
            unlimited.setStackSize(Integer.MAX_VALUE);
            int allowed = itemHandler.fill(unlimited.getFluidStack(), false);
            for (int i = 0; i < held.getCount() && allowed > 0; i++) {
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
        updateHeld(player);
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

    @Override
    public TileMEPatternProviderII getOwner() {
        return ownerII;
    }
}
