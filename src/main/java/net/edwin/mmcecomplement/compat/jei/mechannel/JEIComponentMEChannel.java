package net.edwin.mmcecomplement.compat.jei.mechannel;

import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.integration.recipe.RecipeLayoutPart;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

/** MMCE recipe-layout adapter for the custom channel ingredient. */
public final class JEIComponentMEChannel
    extends ComponentRequirement.JEIComponent<MEChannelIngredient> {

    private final int amount;

    public JEIComponentMEChannel(int amount) {
        this.amount = amount;
    }

    @Override
    public Class<MEChannelIngredient> getJEIRequirementClass() {
        return MEChannelIngredient.class;
    }

    @Override
    public List<MEChannelIngredient> getJEIIORequirements() {
        return Collections.singletonList(new MEChannelIngredient(amount));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public RecipeLayoutPart<MEChannelIngredient> getLayoutPart(Point offset) {
        return new Layout(offset);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onJEIHoverTooltip(int slotIndex, boolean input,
                                  MEChannelIngredient ingredient,
                                  List<String> tooltip) { }

    private static final class Layout
        extends RecipeLayoutPart<MEChannelIngredient> {

        private Layout(Point offset) {
            super(offset);
        }

        @Override public int getComponentWidth() { return 18; }
        @Override public int getComponentHeight() { return 18; }
        @Override public Class<MEChannelIngredient> getLayoutTypeClass() {
            return MEChannelIngredient.class;
        }
        @Override public int getRendererPaddingX() { return 1; }
        @Override public int getRendererPaddingY() { return 1; }
        @Override public int getMaxHorizontalCount() { return 1; }
        @Override public int getComponentHorizontalGap() { return 0; }
        @Override public int getComponentVerticalGap() { return 0; }
        @Override public int getComponentHorizontalSortingOrder() { return 500; }
        @Override public boolean canBeScaled() { return false; }

        @Override
        public IIngredientRenderer<MEChannelIngredient>
        provideIngredientRenderer() {
            return MEChannelIngredientRenderer.INSTANCE;
        }

        @Override
        public void drawBackground(Minecraft minecraft) {
            new RecipeLayoutPart.Item(getOffset()).drawBackground(minecraft);
        }
    }
}
