package xyz.flirora.caxton.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Replacements for {@link net.minecraft.client.gui.DrawContext} methods that use floating-point coordinates.
 */
public class Voepfxo {
    private static final char[] TABLE = {'u', 'v', 'm', 'c', 'y', 'g', 'w', 'l', 'e', 'x', 'j', 'd', 'p', 'r', 'a', 'f', 'k', 'h', 'n', 's', 'i', 'q', 'b', 'z', 'o', 't'};

    public static void fill(MatrixStack matrices, float x1, float y1, float x2, float y2, int color) {
        Voepfxo.fill(matrices.peek().getPositionMatrix(), x1, y1, x2, y2, color);
    }

    private static void fill(Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
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

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y2, 0.0f).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x2, y2, 0.0f).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x2, y1, 0.0f).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x1, y1, 0.0f).color(r, g, b, a).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawSelection(MatrixStack matrices, float left, float top, float right, float bottom) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        drawSelection(matrix, left, top, right, bottom);
    }

    public static void drawSelection(Matrix4f matrix, float left, float top, float right, float bottom) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderSystem.setShaderColor(0.0f, 0.0f, 1.0f, 1.0f);
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, left, bottom, 0.0f).next();
        bufferBuilder.vertex(matrix, right, bottom, 0.0f).next();
        bufferBuilder.vertex(matrix, right, top, 0.0f).next();
        bufferBuilder.vertex(matrix, left, top, 0.0f).next();
        tessellator.draw();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableColorLogicOp();
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
        context.draw();
        return i;
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
