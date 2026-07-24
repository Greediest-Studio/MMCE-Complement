package net.edwin.mmcecomplement.client.gui.widget;

import github.kasuminova.mmce.client.gui.util.MousePos;
import github.kasuminova.mmce.client.gui.util.RenderPos;
import github.kasuminova.mmce.client.gui.util.RenderSize;
import github.kasuminova.mmce.client.gui.widget.Button4State;
import github.kasuminova.mmce.client.gui.widget.base.WidgetGui;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;

/** Four-state 52x13 strip: normal, hovered, pressed, unavailable. */
public class PreviewIconButton4State extends Button4State {

    private static final int STATE_SIZE = 13;
    private static final int TEXTURE_WIDTH = STATE_SIZE * 4;

    @Override
    public void render(WidgetGui gui, RenderSize renderSize, RenderPos renderPos, MousePos mousePos) {
        if (!isVisible()) {
            return;
        }
        int textureX;
        if (isUnavailable()) {
            textureX = STATE_SIZE * 3;
        } else if (isMouseDown()) {
            textureX = STATE_SIZE * 2;
        } else if (isMouseOver(mousePos)) {
            textureX = STATE_SIZE;
        } else {
            textureX = 0;
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        gui.getGui().mc.getTextureManager().bindTexture(textureLocation);
        Gui.drawScaledCustomSizeModalRect(renderPos.posX(), renderPos.posY(),
            textureX, 0.0F, STATE_SIZE, STATE_SIZE,
            renderSize.width(), renderSize.height(), TEXTURE_WIDTH, STATE_SIZE);
    }

    @Override
    public boolean onMouseReleased(MousePos mousePos, RenderPos renderPos) {
        boolean clicked = super.onMouseReleased(mousePos, renderPos);
        if (clicked) {
            net.minecraft.client.Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return clicked;
    }
}
