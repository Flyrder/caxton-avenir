package xyz.flirora.caxton.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.mixin.DrawContextAccessor;

/**
 * Replacements for {@link net.minecraft.client.gui.DrawContext} methods that use floating-point coordinates.
 */
public class Voepfxo {
    private static final char[] TABLE = {'u', 'v', 'm', 'c', 'y', 'g', 'w', 'l', 'e', 'x', 'j', 'd', 'p', 'r', 'a', 'f', 'k', 'h', 'n', 's', 'i', 'q', 'b', 'z', 'o', 't'};

    public static void fill(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        Voepfxo.fill(context, RenderLayer.method_51784(), x1, y1, x2, y2, color);
    }

    public static void fill(DrawContext context, RenderLayer renderLayer, float x1, float y1, float x2, float y2, int color) {
        float tmp;
        if (x1 < x2) {
            tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        if (y1 < y2) {
            tmp = y1;
            y1 = y2;
            y2 = tmp;
        }

        float a = (float) (color >> 24 & 0xFF) / 255.0f;
        float r = (float) (color >> 16 & 0xFF) / 255.0f;
        float g = (float) (color >> 8 & 0xFF) / 255.0f;
        float b = (float) (color & 0xFF) / 255.0f;

        VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(renderLayer);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        vertexConsumer.vertex(matrix, x1, y2, 0.0f).color(r, g, b, a).next();
        vertexConsumer.vertex(matrix, x2, y2, 0.0f).color(r, g, b, a).next();
        vertexConsumer.vertex(matrix, x2, y1, 0.0f).color(r, g, b, a).next();
        vertexConsumer.vertex(matrix, x1, y1, 0.0f).color(r, g, b, a).next();
        ((DrawContextAccessor) context).callDraw();
    }

    public static void drawSelection(DrawContext context, float left, float top, float right, float bottom) {
        fill(context, RenderLayer.method_51786(), left, top, right, bottom, 0xff0000ff);
    }

    public static int drawTextWithShadow(DrawContext context, TextRenderer textRenderer, Text text, float x, float y, int color) {
        return textRenderer.draw(text, x, y, color, true, context.getMatrices().peek().getPositionMatrix(), context.getVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
    }

    public static int drawTextWithShadow(DrawContext context, TextRenderer textRenderer, String text, float x, float y, int color) {
        return drawText(context, textRenderer, text, x, y, color, true);
    }

    public static int drawText(DrawContext context, TextRenderer textRenderer, String text, float x, float y, int color) {
        return drawText(context, textRenderer, text, x, y, color, false);
    }

    public static int drawText(DrawContext context, TextRenderer textRenderer, @Nullable String text, float x, float y, int color, boolean shadow) {
        if (text == null) {
            return 0;
        }
        int i = textRenderer.draw(text, x, y, color, shadow, context.getMatrices().peek().getPositionMatrix(), context.getVertexConsumers(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0, textRenderer.isRightToLeft());
        ((DrawContextAccessor) context).callDraw();
        return i;
    }

    public static float drawText(DrawContext context, TextRenderer textRenderer, @Nullable CaxtonText text, float x, float y, int color, boolean shadow) {
        return drawText(context, textRenderer, text, x, y, color, shadow, -1, Float.POSITIVE_INFINITY);
    }

    public static float drawText(DrawContext context, TextRenderer textRenderer, @Nullable CaxtonText text, float x, float y, int color, boolean shadow, int firstCharacterIndex, float maxWidth) {
        if (text == null) {
            return 0;
        }
        float i = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer().draw(text, x, y, color, shadow, context.getMatrices().peek().getPositionMatrix(), context.getVertexConsumers(), false, 0, 0xF000F0, firstCharacterIndex, maxWidth);
        ((DrawContextAccessor) context).callDraw();
        return i;
    }

    public static void drawText4Way(DrawContext context, TextRenderer textRenderer, OrderedText text, float x, float y, int color, int outlineColor) {
        ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer().drawWithOutline(text, x, y, color, outlineColor, context.getMatrices().peek().getPositionMatrix(), context.getVertexConsumers(), 0xF000F0, false);
        ((DrawContextAccessor) context).callDraw();
    }

    public static int sus(int codePoint) {
        if (codePoint >= 'a' && codePoint <= 'z') {
            return TABLE[codePoint - 'a'];
        } else if (codePoint >= 'A' && codePoint <= 'Z') {
            return TABLE[codePoint - 'A'] + ('A' - 'a');
        } else {
            return codePoint;
        }
    }

    public static String shingetsuGaShirokuMarukuKagayaiteIruYoruNi(String input) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            buf.append((char) sus(c));
        }
        return buf.toString();
    }
}
