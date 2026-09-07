package net.edwin.mmcecomplement.mixin;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

/**
 * Keeps chat line wrapping independent from Cleanroom's optional ICU data.
 * Some installations ship only ICU's binary core data, which makes
 * ICU4JInstances fail during class initialisation and crashes any chat update.
 */
@SideOnly(Side.CLIENT)
@Mixin(GuiUtilRenderComponents.class)
public abstract class MixinGuiUtilRenderComponents {

    /**
     * @author MMCE Complement
     * @reason Use the vanilla formatter when ICU resource bundles are absent.
     */
    @Overwrite
    public static List<ITextComponent> splitText(ITextComponent textComponent,
                                                  int maxTextLength,
                                                  FontRenderer fontRenderer,
                                                  boolean lineSeparate,
                                                  boolean forceTextColor) {
        int width = 0;
        ITextComponent current = new TextComponentString("");
        List<ITextComponent> result = Lists.newArrayList();
        List<ITextComponent> pending = Lists.newArrayList(textComponent);

        for (int index = 0; index < pending.size(); index++) {
            ITextComponent component = pending.get(index);
            String text = component.getUnformattedComponentText();
            boolean split = false;
            if (text.contains("\n")) {
                int newline = text.indexOf('\n');
                String remainder = text.substring(newline + 1);
                text = text.substring(0, newline + 1);
                ITextComponent next = new TextComponentString(remainder);
                next.setStyle(component.getStyle().createShallowCopy());
                pending.add(index + 1, next);
                split = true;
            }

            String formatted = component.getStyle().getFormattingCode() + text;
            if (!forceTextColor) {
                formatted = mmceRemoveTextColors(formatted, false);
            }
            String withoutNewline = formatted.endsWith("\n")
                ? formatted.substring(0, formatted.length() - 1) : formatted;
            int componentWidth = fontRenderer.getStringWidth(withoutNewline);
            TextComponentString rendered = new TextComponentString(withoutNewline);
            rendered.setStyle(component.getStyle().createShallowCopy());

            if (width + componentWidth > maxTextLength) {
                String head = fontRenderer.trimStringToWidth(
                    formatted, maxTextLength - width, false);
                String tail = head.length() < formatted.length()
                    ? formatted.substring(head.length()) : null;
                if (tail != null && !tail.isEmpty()) {
                    int space = head.lastIndexOf(' ');
                    if (space >= 0 && fontRenderer.getStringWidth(
                        formatted.substring(0, space)) > 0) {
                        head = formatted.substring(0, space);
                        if (lineSeparate) space++;
                        tail = formatted.substring(space);
                    } else if (width > 0 && !formatted.contains(" ")) {
                        head = "";
                        tail = formatted;
                    }
                    tail = FontRenderer.getFormatFromString(head) + tail;
                    TextComponentString next = new TextComponentString(tail);
                    next.setStyle(component.getStyle().createShallowCopy());
                    pending.add(index + 1, next);
                }
                componentWidth = fontRenderer.getStringWidth(head);
                rendered = new TextComponentString(head);
                rendered.setStyle(component.getStyle().createShallowCopy());
                split = true;
            }

            if (width + componentWidth <= maxTextLength) {
                width += componentWidth;
                current.appendSibling(rendered);
            } else {
                split = true;
            }
            if (split) {
                result.add(current);
                width = 0;
                current = new TextComponentString("");
            }
        }
        result.add(current);
        return result;
    }

    private static String mmceRemoveTextColors(String text,
                                                boolean forceColor) {
        return !forceColor && !net.minecraft.client.Minecraft.getMinecraft()
            .gameSettings.chatColours
            ? TextFormatting.getTextWithoutFormattingCodes(text) : text;
    }
}
