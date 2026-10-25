package net.edwin.mmcecomplement.variant;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class StorageFluidInteractionTest {
    private static Capability<IFluidHandlerItem> originalCapability;
    private static ForgeModContainer originalForgeContainer;
    private static Field forgeInstance;

    @BeforeAll @SuppressWarnings("unchecked")
    static void bootstrap() throws Exception {
        Bootstrap.register();
        originalCapability = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
        // Forge's annotation injection normally supplies this token at startup.
        // Supply only the token here so real vanilla bucket capabilities work
        // without starting a client or mutating the capability registry.
        Constructor<?> constructor = Capability.class.getDeclaredConstructor(
            String.class, Capability.IStorage.class, Callable.class);
        constructor.setAccessible(true);
        CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY = (Capability<IFluidHandlerItem>)
            constructor.newInstance(IFluidHandlerItem.class.getName(), null, null);
        // Empty vanilla buckets consult Forge's universal bucket field even
        // when universal buckets are disabled. No Forge lifecycle runs here.
        forgeInstance = ForgeModContainer.class.getDeclaredField("INSTANCE");
        forgeInstance.setAccessible(true);
        originalForgeContainer = ForgeModContainer.getInstance();
        if (originalForgeContainer == null) {
            Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
            Field unsafe = unsafeType.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            forgeInstance.set(null, unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(unsafe.get(null), ForgeModContainer.class));
        }
    }

    @AfterAll static void restoreCapability() throws Exception {
        CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY = originalCapability;
        if (forgeInstance != null) forgeInstance.set(null, originalForgeContainer);
    }

    @Test void waterBucketEmptiesAndEmptyBucketFills() {
        FluidTank tank = new FluidTank(4000);
        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
        FluidActionResult emptied = StorageFluidInteraction.interact(
            waterBucket, tank, new ItemStackHandler(1), null);
        assertTrue(emptied.isSuccess());
        assertEquals(Items.BUCKET, emptied.getResult().getItem());
        assertEquals(1000, tank.getFluidAmount());
        assertEquals(Items.WATER_BUCKET, waterBucket.getItem());

        FluidActionResult filled = StorageFluidInteraction.interact(
            emptied.getResult(), tank, new ItemStackHandler(1), null);
        assertTrue(filled.isSuccess());
        assertEquals(Items.WATER_BUCKET, filled.getResult().getItem());
        assertEquals(0, tank.getFluidAmount());
    }

    @Test void insufficientTankRoomLeavesBucketAndFluidUnchanged() {
        FluidTank tank = new FluidTank(new FluidStack(FluidRegistry.WATER, 3500), 4000);
        ItemStack cursor = new ItemStack(Items.WATER_BUCKET);
        FluidActionResult result = StorageFluidInteraction.interact(cursor, tank, new ItemStackHandler(1), null);
        assertFalse(result.isSuccess());
        assertEquals(3500, tank.getFluidAmount());
        assertEquals(Items.WATER_BUCKET, cursor.getItem());
    }

    @Test void stackedEmptyBucketsStowTheFilledBucketAndConsumeOnlyOne() {
        FluidTank tank = new FluidTank(new FluidStack(FluidRegistry.WATER, 2000), 4000);
        ItemStackHandler inventory = new ItemStackHandler(1);
        FluidActionResult result = StorageFluidInteraction.interact(
            new ItemStack(Items.BUCKET, 8), tank, inventory, null);
        assertTrue(result.isSuccess());
        assertEquals(7, result.getResult().getCount());
        assertEquals(Items.BUCKET, result.getResult().getItem());
        assertEquals(Items.WATER_BUCKET, inventory.getStackInSlot(0).getItem());
        assertEquals(1000, tank.getFluidAmount());
    }

    @Test void unavailableResultSpaceDoesNotConsumeFluidOrStackedBuckets() {
        FluidTank tank = new FluidTank(new FluidStack(FluidRegistry.WATER, 2000), 4000);
        ItemStack cursor = new ItemStack(Items.BUCKET, 8);
        FluidActionResult result = StorageFluidInteraction.interact(cursor, tank, new ItemStackHandler(0), null);
        assertFalse(result.isSuccess());
        assertEquals(8, cursor.getCount());
        assertEquals(2000, tank.getFluidAmount());
    }

    @Test void wrongFluidAndNonContainerItemsAreRejected() {
        FluidTank tank = new FluidTank(new FluidStack(FluidRegistry.LAVA, 1000), 4000);
        assertFalse(StorageFluidInteraction.interact(
            new ItemStack(Items.WATER_BUCKET), tank, new ItemStackHandler(1), null).isSuccess());
        assertFalse(StorageFluidInteraction.interact(
            new ItemStack(Items.APPLE), tank, new ItemStackHandler(1), null).isSuccess());
        assertEquals(1000, tank.getFluidAmount());
        assertEquals(FluidRegistry.LAVA, tank.getFluid().getFluid());
    }
}
