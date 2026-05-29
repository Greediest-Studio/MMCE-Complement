package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.edwin.mmcecomplement.init.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class TileMEEnergyOutputBus extends TileMEEnergyBusBase {

    @Override
    public ItemStack getVisualItemStack() {
        Item item = Item.getItemFromBlock(ModBlocks.ME_ENERGY_OUTPUT_BUS);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    protected IOType getIOType() {
        return IOType.OUTPUT;
    }

    @Override
    protected boolean canExtractToMachine() {
        return false;
    }

    @Override
    protected boolean canReceiveFromMachine() {
        return true;
    }

    @Override
    protected boolean shouldRequestWork() {
        return getCurrentEnergy() > 0L;
    }

    @Override
    protected long doTransfer(IMEMonitor<IAEItemStack> inventory) {
        return exportToME(inventory, getCurrentEnergy());
    }
}