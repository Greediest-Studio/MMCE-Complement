package net.edwin.mmcecomplement.compat.ae2fc;

import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** AE2 Fluid Crafting Rework fake-fluid decoding. */
public final class Ae2FcrPatternCompat {

    private Ae2FcrPatternCompat() {}

    public static boolean appendFakeFluid(ItemStack stack,
                                          InfItemFluidHandler target) {
        if (!FakeFluids.isFluidFakeItem(stack)) return false;
        FluidStack fluid = FakeItemRegister.getStack(stack);
        if (fluid != null) target.fill(fluid, true);
        return fluid != null;
    }
}
