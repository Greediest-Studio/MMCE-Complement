package net.edwin.mmcecomplement.compat.ae.tile;

import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.ArrayList;
import java.util.List;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.edwin.mmcecomplement.compat.CompatMods;
import net.edwin.mmcecomplement.mechannel.ModMEChannelTypes;
import net.edwin.mmcecomplement.variant.StorageAccessPort;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Direct capability view of the storage controller backing a formed storage
 * multiblock. No local buffer is kept: every port on the same structure
 * returns the controller's authoritative handler, so inserts and extracts
 * are immediately visible to all other ports and to the controller GUI.
 */
public class TileStorageInteractionAssembly extends TileColorableMachineComponent
    implements StorageAccessPort, MachineComponentTile {

    private volatile TileMultiblockMachineController controller;
    private NBTTagList legacyUpgrades = new NBTTagList();

    @Override public void readCustomNBT(NBTTagCompound data) {
        super.readCustomNBT(data);
        legacyUpgrades = data.getTagList("LegacyStorageBusUpgrades", 10).copy();
        // Recover real upgrade cards installed before this block stopped being
        // an ME bus. Configuration/filter ghost stacks must never be dropped.
        NBTTagCompound uel = data.getCompoundTag("StorageBusSettings").getCompoundTag("upgrades");
        appendLegacyUpgrades(uel.getTagList("Items", 10));
        appendLegacyUpgrades(data.getCompoundTag("Ae2sStorageBus").getTagList("upgrades", 10));
    }

    private void appendLegacyUpgrades(NBTTagList items) {
        for (int i = 0; i < items.tagCount(); i++) legacyUpgrades.appendTag(items.getCompoundTagAt(i).copy());
    }

    @Override public void writeCustomNBT(NBTTagCompound data) {
        super.writeCustomNBT(data);
        if (legacyUpgrades.tagCount() > 0) data.setTag("LegacyStorageBusUpgrades", legacyUpgrades.copy());
        else data.removeTag("LegacyStorageBusUpgrades");
        data.removeTag("StorageBusSettings");
        data.removeTag("Ae2sStorageBus");
    }

    final List<ItemStack> takeLegacyUpgrades() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < legacyUpgrades.tagCount(); i++) {
            ItemStack stack = new ItemStack(legacyUpgrades.getCompoundTagAt(i));
            if (!stack.isEmpty()) drops.add(stack);
        }
        legacyUpgrades = new NBTTagList();
        markDirty();
        return drops;
    }

    public final void dropLegacyUpgrades() {
        if (getWorld() == null || getWorld().isRemote) return;
        for (ItemStack stack : takeLegacyUpgrades()) Block.spawnAsEntity(getWorld(), getPos(), stack);
    }
    @Override
    public MachineComponent<Object> provideComponent() {
        return new MachineComponent<Object>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ModMEChannelTypes.STORAGE_ACCESS_COMPONENT;
            }

            @Override
            public Object getContainerProvider() {
                return TileStorageInteractionAssembly.this;
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }
        };
    }

    @Override
    public void bindStorage(TileMultiblockMachineController candidate) {
        if (!(candidate instanceof StorageController)
            || !((StorageController) candidate)
                .mmceComplement$isStorageController()) {
            return;
        }
        synchronized (this) {
            controller = candidate;
        }
    }

    @Override
    public void unbindStorage(TileMultiblockMachineController candidate) {
        synchronized (this) {
            if (controller == candidate) controller = null;
        }
    }

    @Nullable
    protected StorageController storage() {
        TileMultiblockMachineController candidate = controller;
        if (candidate == null || candidate.isInvalid()
            || candidate.getWorld() != getWorld()
            || getWorld() == null
            || !getWorld().isBlockLoaded(candidate.getPos())
            || getWorld().getTileEntity(candidate.getPos()) != candidate
            || !candidate.isStructureFormed()) {
            return null;
        }
        StorageController result = (StorageController) candidate;
        return result.mmceComplement$isStorageController() ? result : null;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability,
                                 @Nullable EnumFacing facing) {
        return getCapability(capability, facing) != null
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T getCapability(@Nonnull Capability<T> capability,
                               @Nullable EnumFacing facing) {
        StorageController storage = storage();
        if (storage != null) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                IItemHandler items = storage.mmceComplement$getStorageInventory();
                if (items != null) return (T) items;
            }
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                IFluidHandler fluids = storage.mmceComplement$getFluidStorage();
                if (fluids != null) return (T) fluids;
            }
            Capability gasCapability = gasCapability();
            if (gasCapability != null && capability == gasCapability) {
                Object gases = storage.mmceComplement$getGasStorage();
                if (gases != null) return (T) gases;
            }
        }
        return super.getCapability(capability, facing);
    }

    /** Resolve Mekanism's optional capability without loading Mekanism classes. */
    @Nullable
    private static Capability<?> gasCapability() {
        if (!CompatMods.isMekanismCompatLoaded()) return null;
        try {
            Class<?> type = Class.forName(
                "mekanism.common.capabilities.Capabilities", false,
                TileStorageInteractionAssembly.class.getClassLoader());
            Field field = type.getField("GAS_HANDLER_CAPABILITY");
            return (Capability<?>) field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void invalidate() {
        controller = null;
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        controller = null;
        super.onChunkUnload();
    }

}
