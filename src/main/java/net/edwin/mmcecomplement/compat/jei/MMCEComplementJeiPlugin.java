package net.edwin.mmcecomplement.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IGhostIngredientHandler;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import net.edwin.mmcecomplement.compat.jei.mechannel.MEChannelIngredient;
import net.edwin.mmcecomplement.compat.jei.mechannel.MEChannelIngredientHelper;
import net.edwin.mmcecomplement.compat.jei.mechannel.MEChannelIngredientRenderer;
import net.edwin.mmcecomplement.gui.GuiFilteredFluidOutputHatch;
import net.edwin.mmcecomplement.gui.GuiFilteredItemOutputHatch;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/** JEI/HEI ghost targets for the two filtered output configuration slots. */
@JEIPlugin
public class MMCEComplementJeiPlugin implements IModPlugin {

    @Override
    public void registerIngredients(IModIngredientRegistration registry) {
        registry.register(() -> MEChannelIngredient.class,
            Collections.emptyList(), MEChannelIngredientHelper.INSTANCE,
            MEChannelIngredientRenderer.INSTANCE);
    }

    @Override
    public void register(IModRegistry registry) {
        registry.addGhostIngredientHandler(GuiFilteredItemOutputHatch.class,
            new ItemFilterGhostHandler());
        registry.addGhostIngredientHandler(GuiFilteredFluidOutputHatch.class,
            new FluidFilterGhostHandler());
    }

    private static final class ItemFilterGhostHandler
        implements IGhostIngredientHandler<GuiFilteredItemOutputHatch> {

        @Override
        public <I> List<Target<I>> getTargets(
            GuiFilteredItemOutputHatch gui, I ingredient,
            boolean doStart) {
            if (!(ingredient instanceof ItemStack)
                || ((ItemStack) ingredient).isEmpty()) {
                return Collections.emptyList();
            }
            final ItemStack stack = ((ItemStack) ingredient).copy();
            return Collections.singletonList(new Target<I>() {
                @Override
                public Rectangle getArea() {
                    return new Rectangle(gui.getFilterScreenX(),
                        gui.getFilterScreenY(), 16, 16);
                }

                @Override
                public void accept(I ignored) {
                    gui.setFilterFromJei(stack);
                }
            });
        }

        @Override public void onComplete() { }
    }

    private static final class FluidFilterGhostHandler
        implements IGhostIngredientHandler<GuiFilteredFluidOutputHatch> {

        @Override
        public <I> List<Target<I>> getTargets(
            GuiFilteredFluidOutputHatch gui, I ingredient,
            boolean doStart) {
            if (!(ingredient instanceof FluidStack)
                || ((FluidStack) ingredient).amount <= 0) {
                return Collections.emptyList();
            }
            final FluidStack stack = ((FluidStack) ingredient).copy();
            return Collections.singletonList(new Target<I>() {
                @Override
                public Rectangle getArea() {
                    return new Rectangle(gui.getFilterScreenX(),
                        gui.getFilterScreenY(), 16, 16);
                }

                @Override
                public void accept(I ignored) {
                    gui.setFilterFromJei(stack);
                }
            });
        }

        @Override public void onComplete() { }
    }
}
