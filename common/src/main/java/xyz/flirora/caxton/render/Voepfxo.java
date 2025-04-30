// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.mixin.DrawContextAccessor;

/**
 * Replacements for {@link net.minecraft.client.gui.DrawContext} methods that use floating-point
 * coordinates.
 */
public class Voepfxo {
  private static final char[] TABLE = {
    'u', 'v', 'm', 'c', 'y', 'g', 'w', 'l', 'e', 'x', 'j', 'd', 'p', 'r', 'a', 'f', 'k', 'h', 'n',
    's', 'i', 'q', 'b', 'z', 'o', 't'
  };

  public static void fill(DrawContext context, float x1, float y1, float x2, float y2, int color) {
    Voepfxo.fill(context, RenderPipelines.GUI, x1, y1, x2, y2, color);
  }

  public static void fill(
      DrawContext context,
      RenderPipeline pipeline,
      float x1,
      float y1,
      float x2,
      float y2,
      int color) {
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

    Voepfxo.fill(context, pipeline, TextureSetup.empty(), x1, y1, x2, y2, color, null);
  }

  private static void fill(
      DrawContext context,
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      float x1,
      float y1,
      float x2,
      float y2,
      int color,
      @Nullable Integer color2) {
    ((DrawContextAccessor) context)
        .getState()
        .addSimpleElement(
            new ColoredQuadGuiElementRenderStateFloat(
                pipeline,
                textureSetup,
                new Matrix3x2f(context.getMatrices()),
                x1,
                y1,
                x2,
                y2,
                color,
                color2 != null ? color2 : color,
                ((DrawContextAccessor) context).getScissorStack().peekLast()));
  }

  public static void drawSelection(
      DrawContext context, float left, float top, float right, float bottom) {
    fill(context, RenderPipelines.GUI_TEXT_HIGHLIGHT, left, top, right, bottom, 0xff0000ff);
  }

  public static int drawTextWithShadow(
      DrawContext context, TextRenderer textRenderer, Text text, float x, float y, int color) {
    return Voepfxo.drawText(context, textRenderer, text, x, y, color, true);
  }

  public static int drawTextWithShadow(
      DrawContext context, TextRenderer textRenderer, String text, float x, float y, int color) {
    return drawText(context, textRenderer, text, x, y, color, true);
  }

  public static int drawText(
      DrawContext context, TextRenderer textRenderer, String text, float x, float y, int color) {
    return drawText(context, textRenderer, text, x, y, color, false);
  }

  public static int drawText(
      DrawContext context,
      TextRenderer textRenderer,
      @Nullable Text text,
      float x,
      float y,
      int color,
      boolean shadow) {
    if (text == null) {
      return 0;
    }
    int i =
        textRenderer.draw(
            text,
            x,
            y,
            color,
            shadow,
            context.getMatrices().peek().getPositionMatrix(),
            ((DrawContextAccessor) context).getVertexConsumers(),
            TextRenderer.TextLayerType.NORMAL,
            0,
            0xF000F0);
    return i;
  }

  public static int drawText(
      DrawContext context,
      TextRenderer textRenderer,
      @Nullable String text,
      float x,
      float y,
      int color,
      boolean shadow) {
    if (text == null) {
      return 0;
    }
    int i =
        textRenderer.draw(
            text,
            x,
            y,
            color,
            shadow,
            context.getMatrices().peek().getPositionMatrix(),
            ((DrawContextAccessor) context).getVertexConsumers(),
            TextRenderer.TextLayerType.NORMAL,
            0,
            0xF000F0);
    return i;
  }

  public static float drawText(
      DrawContext context,
      TextRenderer textRenderer,
      @Nullable CaxtonText text,
      float x,
      float y,
      int color,
      boolean shadow) {
    return drawText(context, textRenderer, text, x, y, color, shadow, -1, Float.POSITIVE_INFINITY);
  }

  public static float drawText(
      DrawContext context,
      TextRenderer textRenderer,
      @Nullable CaxtonText text,
      float x,
      float y,
      int color,
      boolean shadow,
      int firstCharacterIndex,
      float maxWidth) {
    if (text == null) {
      return 0;
    }
    float i =
        ((HasCaxtonTextRenderer) textRenderer)
            .getCaxtonTextRenderer()
            .draw(
                text,
                x,
                y,
                color,
                shadow,
                context.getMatrices().peek().getPositionMatrix(),
                ((DrawContextAccessor) context).getVertexConsumers(),
                false,
                0,
                0xF000F0,
                firstCharacterIndex,
                maxWidth);
    return i;
  }

  public static void drawText4Way(
      DrawContext context,
      TextRenderer textRenderer,
      OrderedText text,
      float x,
      float y,
      int color,
      int outlineColor) {
    ((HasCaxtonTextRenderer) textRenderer)
        .getCaxtonTextRenderer()
        .drawWithOutline(
            text,
            x,
            y,
            color,
            outlineColor,
            context.getMatrices().peek().getPositionMatrix(),
            ((DrawContextAccessor) context).getVertexConsumers(),
            0xF000F0,
            false);
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

  private record ColoredQuadGuiElementRenderStateFloat(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2f pose,
      float x0,
      float y0,
      float x1,
      float y1,
      int col1,
      int col2,
      @Nullable ScreenRect scissorArea,
      @Nullable ScreenRect bounds)
      implements SimpleGuiElementRenderState {
    public ColoredQuadGuiElementRenderStateFloat(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        int col1,
        int col2,
        @Nullable ScreenRect scissorArea) {
      this(
          pipeline,
          textureSetup,
          pose,
          x0,
          y0,
          x1,
          y1,
          col1,
          col2,
          scissorArea,
          calculateBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    public ColoredQuadGuiElementRenderStateFloat(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        int col1,
        int col2,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds) {
      this.pipeline = pipeline;
      this.textureSetup = textureSetup;
      this.pose = pose;
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
      this.col1 = col1;
      this.col2 = col2;
      this.scissorArea = scissorArea;
      this.bounds = bounds;
    }

    public void setupVertices(VertexConsumer vertices, float depth) {
      vertices.vertex(this.pose(), this.x0(), this.y0(), depth).color(this.col1());
      vertices.vertex(this.pose(), this.x0(), this.y1(), depth).color(this.col2());
      vertices.vertex(this.pose(), this.x1(), this.y1(), depth).color(this.col2());
      vertices.vertex(this.pose(), this.x1(), this.y0(), depth).color(this.col1());
    }

    @Nullable private static ScreenRect calculateBounds(
        float x0, float y0, float x1, float y1, Matrix3x2f pose, @Nullable ScreenRect scissorArea) {
      ScreenRect screenRect2 =
          (new ScreenRect(Math.round(x0), Math.round(y0), Math.round(x1 - x0), Math.round(y1 - y0)))
              .method_71523(pose);
      return scissorArea != null ? scissorArea.intersection(screenRect2) : screenRect2;
    }

    public RenderPipeline pipeline() {
      return this.pipeline;
    }

    public TextureSetup textureSetup() {
      return this.textureSetup;
    }

    public Matrix3x2f pose() {
      return this.pose;
    }

    public float x0() {
      return this.x0;
    }

    public float y0() {
      return this.y0;
    }

    public float x1() {
      return this.x1;
    }

    public float y1() {
      return this.y1;
    }

    public int col1() {
      return this.col1;
    }

    public int col2() {
      return this.col2;
    }

    @Nullable public ScreenRect scissorArea() {
      return this.scissorArea;
    }

    @Nullable public ScreenRect bounds() {
      return this.bounds;
    }
  }
}
