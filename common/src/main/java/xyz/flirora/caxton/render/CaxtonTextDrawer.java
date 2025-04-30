// (C) 2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontOptions;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.RunGroup;
import xyz.flirora.caxton.layout.ShapingResult;
import xyz.flirora.caxton.mixin.TextRendererAccessor;

@Environment(EnvType.CLIENT)
public class CaxtonTextDrawer implements CharacterVisitor {
  private final CaxtonTextRenderer textRenderer;
  private final boolean shadow;
  private final int color;
  private final int backgroundColor;
  private final TextRenderer.TextLayerType layerType;
  private final int light;
  private final boolean swapZIndex;
  private float x;
  private float y;
  private final List<BakedGlyph.DrawnGlyph> glyphs;
  @Nullable private List<BakedGlyph.Rectangle> rectangles;

  // TODO: If we want to convert all text rendering over to CTD,
  // then we need to track maxX and threshold.

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      float x,
      float y,
      int color,
      int backgroundColor,
      boolean shadow,
      TextRenderer.TextLayerType layerType,
      int light,
      boolean swapZIndex) {
    this.textRenderer = textRenderer;
    this.shadow = shadow;
    this.color = color;
    this.backgroundColor = backgroundColor;
    this.layerType = layerType;
    this.light = light;
    this.swapZIndex = swapZIndex;
    this.x = x;
    this.y = y;
    this.glyphs = new ArrayList<>();
  }

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      TextRenderer.TextLayerType layerType,
      int light) {
    this(textRenderer, x, y, color, 0, shadow, layerType, light, true);
  }

  public float getX() {
    return this.x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return this.y;
  }

  public void setY(float y) {
    this.y = y;
  }

  public void addRectangle(BakedGlyph.Rectangle rectangle) {
    if (this.rectangles == null) {
      this.rectangles = new ArrayList<>();
    }
    this.rectangles.add(rectangle);
  }

  /**
   * Add a glyph from a legacy font.
   *
   * @param index the current index of the character
   * @param style the style of the character, containing formatting and font information
   * @param codePoint the code point of the character
   * @return whether to continue with visiting the text, namely {@code true}
   */
  @Override
  public boolean accept(int index, Style style, int codePoint) {
    FontStorage storage = this.textRenderer.getFontStorage(style.getFont());
    Glyph glyph =
        storage.getGlyph(
            codePoint,
            ((TextRendererAccessor) this.textRenderer.vanillaTextRenderer).isValidateAdvance());
    BakedGlyph bakedGlyph =
        style.isObfuscated() && codePoint != ' '
            ? storage.getObfuscatedBakedGlyph(glyph)
            : storage.getBaked(codePoint);
    boolean bold = style.isBold();
    TextColor textColor = style.getColor();
    int color = getTextColor(textColor);
    int shadowColor = getShadowColor(style, color);
    float advance = glyph.getAdvance(bold);
    float x = index == 0 ? this.x - 1.0f : this.x;
    if (!(bakedGlyph instanceof EmptyBakedGlyph)) {
      float boldOffset = bold ? glyph.getBoldOffset() : 0.0f;
      this.glyphs.add(
          new BakedGlyph.DrawnGlyph(
              this.x,
              this.y,
              color,
              shadowColor,
              bakedGlyph,
              style,
              boldOffset,
              glyph.getShadowOffset()));
    }

    float shadowOffset = glyph.getShadowOffset();
    if (style.isStrikethrough()) {
      this.addRectangle(
          new BakedGlyph.Rectangle(
              x + shadowOffset,
              this.y + shadowOffset + 4.5f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 3.5f,
              this.getForegroundZIndex(),
              color,
              shadowColor,
              shadowOffset));
    }
    if (style.isUnderlined()) {
      this.addRectangle(
          new BakedGlyph.Rectangle(
              x + shadowOffset,
              this.y + shadowOffset + 9.0f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 8.0f,
              this.getForegroundZIndex(),
              color,
              shadowColor,
              shadowOffset));
    }

    this.x += advance;
    return true;
  }

  public void acceptShapedRun(
      ShapingResult shapingResult, RunGroup runGroup, int index, CaxtonGlyphCache cache) {
    ConfiguredCaxtonFont configuredFont = runGroup.getFont();
    CaxtonFont font = configuredFont.font();
    CaxtonFontOptions options = font.getOptions();
    CaxtonGlyphCache.Font cacheForFont = cache.forFont(font);

    // TODO…
  }

  public int getTextColor(TextColor textColor) {
    return textColor != null
        ? ColorHelper.withAlpha(ColorHelper.getAlpha(this.color), textColor.getRgb())
        : this.color;
  }

  public int getShadowColor(Style style, int textColor) {
    Integer shadowColor = style.getShadowColor();
    if (shadowColor != null) {
      float textAlpha = ColorHelper.getAlphaFloat(textColor);
      float shadowAlpha = ColorHelper.getAlphaFloat(shadowColor);
      return textAlpha != 1.0F
          ? ColorHelper.withAlpha(
              ColorHelper.channelFromFloat(textAlpha * shadowAlpha), shadowColor)
          : shadowColor;
    } else {
      return this.shadow ? ColorHelper.scaleRgb(textColor, 0.25F) : 0;
    }
  }

  float drawLayer(float x, VertexConsumerProvider vertexConsumers, Matrix4f matrix) {
    BakedGlyph bakedGlyph = null;
    if (this.backgroundColor != 0) {
      BakedGlyph.Rectangle rectangle =
          new BakedGlyph.Rectangle(
              x - 1.0F,
              this.y + 9.0F,
              this.x,
              this.y - 1.0F,
              this.getBackgroundZIndex(),
              this.backgroundColor);
      bakedGlyph = this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      VertexConsumer vertexConsumer =
          vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
      bakedGlyph.drawRectangle(rectangle, matrix, vertexConsumer, this.light);
    }

    this.drawGlyphs(vertexConsumers, matrix);
    if (this.rectangles != null) {
      if (bakedGlyph == null) {
        bakedGlyph =
            this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      }

      VertexConsumer vertexConsumer2 =
          vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));

      for (BakedGlyph.Rectangle rectangle2 : this.rectangles) {
        bakedGlyph.drawRectangle(rectangle2, matrix, vertexConsumer2, this.light);
      }
    }

    return this.x;
  }

  void drawGlyphs(VertexConsumerProvider vertexConsumers, Matrix4f matrix) {
    for (BakedGlyph.DrawnGlyph drawnGlyph : this.glyphs) {
      BakedGlyph bakedGlyph = drawnGlyph.glyph();
      VertexConsumer vertexConsumer =
          vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
      bakedGlyph.draw(drawnGlyph, matrix, vertexConsumer, this.light);
    }
  }

  public float getForegroundZIndex() {
    return this.swapZIndex ? 0.01F : -0.01F;
  }

  private float getBackgroundZIndex() {
    return this.swapZIndex ? -0.01F : 0.01F;
  }
}
