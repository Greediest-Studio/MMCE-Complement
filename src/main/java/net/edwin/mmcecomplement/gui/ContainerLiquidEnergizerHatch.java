package net.edwin.mmcecomplement.gui;

import net.edwin.mmcecomplement.tile.TileLiquidEnergizerHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;

/** Synchronizes five long values as unsigned 16-bit window fields. */
public class ContainerLiquidEnergizerHatch extends Container {

    private static final int ENERGY = 0;
    private static final int FLUID = 4;
    private static final int ENERGY_CAPACITY = 8;
    private static final int FLUID_CAPACITY = 12;
    private static final int CONVERSION_RATIO = 16;

    private final TileLiquidEnergizerHatch tile;
    private long lastEnergy = Long.MIN_VALUE;
    private long lastFluid = Long.MIN_VALUE;
    private long lastEnergyCapacity = Long.MIN_VALUE;
    private long lastFluidCapacity = Long.MIN_VALUE;
    private long lastConversionRatio = Long.MIN_VALUE;

    public ContainerLiquidEnergizerHatch(EntityPlayer player,
                                         TileLiquidEnergizerHatch tile) {
        this.tile = tile;
        bindPlayerInventory(player.inventory);
    }

    public TileLiquidEnergizerHatch getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid()
            && tile.getWorld() == player.getEntityWorld()
            && player.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendLong(listener, ENERGY, tile.getCurrentEnergy());
        sendLong(listener, FLUID, tile.getFluidAmountLong());
        sendLong(listener, ENERGY_CAPACITY, tile.getMaxEnergy());
        sendLong(listener, FLUID_CAPACITY, tile.getFluidCapacityLong());
        sendLong(listener, CONVERSION_RATIO, tile.getConversionRatio());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        long energy = tile.getCurrentEnergy();
        long fluid = tile.getFluidAmountLong();
        long energyCapacity = tile.getMaxEnergy();
        long fluidCapacity = tile.getFluidCapacityLong();
        long conversionRatio = tile.getConversionRatio();
        for (IContainerListener listener : listeners) {
            if (energy != lastEnergy) sendLong(listener, ENERGY, energy);
            if (fluid != lastFluid) sendLong(listener, FLUID, fluid);
            if (energyCapacity != lastEnergyCapacity) {
                sendLong(listener, ENERGY_CAPACITY, energyCapacity);
            }
            if (fluidCapacity != lastFluidCapacity) {
                sendLong(listener, FLUID_CAPACITY, fluidCapacity);
            }
            if (conversionRatio != lastConversionRatio) {
                sendLong(listener, CONVERSION_RATIO, conversionRatio);
            }
        }
        lastEnergy = energy;
        lastFluid = fluid;
        lastEnergyCapacity = energyCapacity;
        lastFluidCapacity = fluidCapacity;
        lastConversionRatio = conversionRatio;
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id >= ENERGY && id < ENERGY + 4) {
            tile.setClientEnergy(replacePart(tile.getCurrentEnergy(),
                id - ENERGY, data));
        } else if (id >= FLUID && id < FLUID + 4) {
            tile.setClientFluidAmount(replacePart(tile.getFluidAmountLong(),
                id - FLUID, data));
        } else if (id >= ENERGY_CAPACITY && id < ENERGY_CAPACITY + 4) {
            tile.setClientEnergyCapacity(replacePart(tile.getMaxEnergy(),
                id - ENERGY_CAPACITY, data));
        } else if (id >= FLUID_CAPACITY && id < FLUID_CAPACITY + 4) {
            tile.setClientFluidCapacity(replacePart(tile.getFluidCapacityLong(),
                id - FLUID_CAPACITY, data));
        } else if (id >= CONVERSION_RATIO && id < CONVERSION_RATIO + 4) {
            tile.setClientConversionRatio(replacePart(tile.getConversionRatio(),
                id - CONVERSION_RATIO, data));
        }
    }

    private void sendLong(IContainerListener listener, int firstId,
                          long value) {
        for (int part = 0; part < 4; part++) {
            listener.sendWindowProperty(this,
                firstId + part, (int) ((value >>> (part * 16)) & 0xFFFFL));
        }
    }

    private static long replacePart(long value, int part, int data) {
        long shift = part * 16L;
        long mask = 0xFFFFL << shift;
        return (value & ~mask) | ((data & 0xFFFFL) << shift);
    }

    private void bindPlayerInventory(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventory,
                    column + row * 9 + 9, 8 + column * 18,
                    84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column,
                8 + column * 18, 142));
        }
    }
}
