package net.edwin.mmcecomplement.compat.ae.tile;

import appeng.api.parts.IPartHost;
import appeng.api.storage.ICellContainer;
import appeng.me.helpers.IGridProxyable;
import net.edwin.mmcecomplement.compat.ae.block.BlockStorageInteractionAssembly;
import net.edwin.mmcecomplement.variant.StorageController;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Callable;
import static org.junit.jupiter.api.Assertions.*;

class StorageInteractionAssemblyTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void plainComponentHasNoMeNodeOrBusGui() {
        TileStorageInteractionAssembly tile = new TileStorageInteractionAssembly();
        assertFalse(tile instanceof IGridProxyable);
        assertFalse(tile instanceof ICellContainer);
        assertFalse(tile instanceof IPartHost);
        Block block = new BlockStorageInteractionAssembly();
        assertFalse(block instanceof github.kasuminova.mmce.common.block.appeng.BlockMEMachineComponent);
        assertFalse(block.onBlockActivated(null, null, null, null, null, null, 0, 0, 0));
    }

    @Test void twoPipePortsMutateTheSameControllerInventoryWithoutBuffers() throws Exception {
        Capability<IItemHandler> originalItems = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        Capability<IFluidHandler> originalFluids = CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
        try {
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY = token("test-items");
            CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY = token("test-fluids");
            ItemStackHandler items = new ItemStackHandler(1);
            FluidTank fluids = new FluidTank(1000);
            StorageController controller = (StorageController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { StorageController.class }, (proxy, method, args) -> {
                    if (method.getName().equals("mmceComplement$getStorageInventory")) return items;
                    if (method.getName().equals("mmceComplement$getFluidStorage")) return fluids;
                    return null;
                });
            TileStorageInteractionAssembly first = port(controller), second = port(controller);
            IItemHandler firstItems = first.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            IItemHandler secondItems = second.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            assertSame(items, firstItems);
            assertSame(items, secondItems);
            assertTrue(firstItems.insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty());
            assertTrue(secondItems.insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty());
            assertEquals(2, items.getStackInSlot(0).getCount());
            assertEquals(1, firstItems.extractItem(0, 1, false).getCount());
            assertEquals(1, secondItems.getStackInSlot(0).getCount());
            IFluidHandler firstFluids = first.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            IFluidHandler secondFluids = second.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            assertSame(fluids, firstFluids);
            assertSame(fluids, secondFluids);
            assertEquals(600, firstFluids.fill(new FluidStack(FluidRegistry.WATER, 600), true));
            assertEquals(400, secondFluids.fill(new FluidStack(FluidRegistry.WATER, 600), true));
            assertEquals(1000, fluids.getFluidAmount());
        } finally {
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY = originalItems;
            CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY = originalFluids;
        }
    }

    @Test void oldUpgradeCardsSurviveReloadAndDropOnceWithoutFilterGhosts() {
        for (boolean supergiant : new boolean[] { false, true }) {
            NBTTagCompound oldData = new NBTTagCompound(), bus = new NBTTagCompound();
            NBTTagList cards = new NBTTagList();
            cards.appendTag(new ItemStack(Items.DIAMOND).writeToNBT(new NBTTagCompound()));
            if (supergiant) bus.setTag("upgrades", cards);
            else {
                NBTTagCompound inventory = new NBTTagCompound();
                inventory.setTag("Items", cards);
                bus.setTag("upgrades", inventory);
            }
            bus.setTag("config", new ItemStack(Items.EMERALD, 64).writeToNBT(new NBTTagCompound()));
            oldData.setTag(supergiant ? "Ae2sStorageBus" : "StorageBusSettings", bus);
            TileStorageInteractionAssembly original = new TileStorageInteractionAssembly();
            original.readCustomNBT(oldData);
            NBTTagCompound saved = new NBTTagCompound();
            original.writeCustomNBT(saved);
            TileStorageInteractionAssembly restored = new TileStorageInteractionAssembly();
            restored.readCustomNBT(saved);
            List<ItemStack> drops = restored.takeLegacyUpgrades();
            assertEquals(1, drops.size());
            assertSame(Items.DIAMOND, drops.get(0).getItem());
            assertEquals(1, drops.get(0).getCount());
            assertTrue(restored.takeLegacyUpgrades().isEmpty());
            restored.writeCustomNBT(saved);
            restored.readCustomNBT(saved);
            assertTrue(restored.takeLegacyUpgrades().isEmpty());
        }
    }

    private static TileStorageInteractionAssembly port(StorageController controller) {
        return new TileStorageInteractionAssembly() {
            @Override protected StorageController storage() { return controller; }
        };
    }

    @SuppressWarnings("unchecked") private static <T> Capability<T> token(String name) throws Exception {
        Constructor<?> constructor = Capability.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return (Capability<T>) constructor.newInstance(name, null, (Callable<T>) () -> null);
    }
}
