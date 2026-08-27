package net.edwin.mmcecomplement.compat.jei.mechannel;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.edwin.mmcecomplement.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Renders the channel hatch icon with the exact requested amount. */
public final class MEChannelIngredientRenderer
    implements IIngredientRenderer<MEChannelIngredient> {

    private static final ResourceLocation ICON = new ResourceLocation(
        Tags.MOD_ID, "textures/gui/me_channel_ingredient.png");

    public static final MEChannelIngredientRenderer INSTANCE =
        new MEChannelIngredientRenderer();

    private MEChannelIngredientRenderer() { }

    @Override
    public void render(Minecraft minecraft, int xPosition, int yPosition,
                       @Nullable MEChannelIngredient ingredient) {
        if (ingredient == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        minecraft.getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(xPosition, yPosition,
            0F, 0F, 16, 16, 16F, 16F);
        GlStateManager.popMatrix();

        String amount = Integer.toString(ingredient.getAmount());
        int textX = xPosition + 17
            - minecraft.fontRenderer.getStringWidth(amount);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        minecraft.fontRenderer.drawStringWithShadow(amount, textX,
            yPosition + 9, 0xFFFFFF);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override
    public List<String> getTooltip(Minecraft minecraft,
                                   MEChannelIngredient ingredient,
                                   ITooltipFlag tooltipFlag) {
        if (ingredient == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(I18n.format(
            "jei.mmce_complement.me_channel.amount",
            ingredient.getAmount()));
    }
}
