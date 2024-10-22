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
import xyz.flirora.caxton.mixin.TextRendererAccessor;

@Environment(EnvType.CLIENT)
public class CaxtonTextDrawer implements CharacterVisitor {
  private final CaxtonTextRenderer textRenderer;
  final VertexConsumerProvider vertexConsumers;
  private final boolean shadow;
  private final float brightnessMultiplier;
  private final int color;
  private final int backgroundColor;
  private final Matrix4f matrix;
  private final TextRenderer.TextLayerType layerType;
  private final int light;
  private final boolean swapZIndex;
  private float x;
  private float y;
  private final List<BakedGlyph.DrawnGlyph> glyphs;
  @Nullable private List<BakedGlyph.Rectangle> rectangles;

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      VertexConsumerProvider vertexConsumers,
      float x,
      float y,
      int color,
      int backgroundColor,
      boolean shadow,
      Matrix4f matrix,
      TextRenderer.TextLayerType layerType,
      int light,
      boolean swapZIndex) {
    this.textRenderer = textRenderer;
    this.vertexConsumers = vertexConsumers;
    this.shadow = shadow;
    this.brightnessMultiplier = shadow ? 0.25f : 1.0f;
    this.color = ColorHelper.scaleRgb(color, this.brightnessMultiplier);
    this.backgroundColor = backgroundColor;
    this.matrix = matrix;
    this.layerType = layerType;
    this.light = light;
    this.swapZIndex = swapZIndex;
    this.x = x;
    this.y = y;
    this.glyphs = new ArrayList<>();
  }

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      VertexConsumerProvider vertexConsumers,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      TextRenderer.TextLayerType layerType,
      int light) {
    this(textRenderer, vertexConsumers, x, y, color, 0, shadow, matrix, layerType, light, true);
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

  // Called for legacy-font glyphs
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
    int color =
        textColor != null
            ? ColorHelper.withAlpha(
                ColorHelper.getAlpha(this.color),
                ColorHelper.scaleRgb(textColor.getRgb(), this.brightnessMultiplier))
            : this.color;
    float advance = glyph.getAdvance(bold);
    float x = index == 0 ? this.x - 1.0f : this.x;
    if (!(bakedGlyph instanceof EmptyBakedGlyph)) {
      float boldOffset = bold ? glyph.getBoldOffset() : 0.0f;
      float shadowOffset = this.shadow ? glyph.getShadowOffset() : 0.0f;
      this.glyphs.add(
          new BakedGlyph.DrawnGlyph(
              this.x + shadowOffset, this.y + shadowOffset, color, bakedGlyph, style, boldOffset));
    }

    float shadowOffset = this.shadow ? 1.0f : 0.0f;
    if (style.isStrikethrough()) {
      this.addRectangle(
          new BakedGlyph.Rectangle(
              x + shadowOffset,
              this.y + shadowOffset + 4.5f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 3.5f,
              this.getForegroundZIndex(),
              color));
    }
    if (style.isUnderlined()) {
      this.addRectangle(
          new BakedGlyph.Rectangle(
              x + shadowOffset,
              this.y + shadowOffset + 9.0f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 8.0f,
              this.getForegroundZIndex(),
              color));
    }

    this.x += advance;
    return true;
  }

  float drawLayer(float x) {
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
          this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
      bakedGlyph.drawRectangle(rectangle, this.matrix, vertexConsumer, this.light);
    }

    this.drawGlyphs();
    if (this.rectangles != null) {
      if (bakedGlyph == null) {
        bakedGlyph =
            this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      }

      VertexConsumer vertexConsumer2 =
          this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));

      for (BakedGlyph.Rectangle rectangle2 : this.rectangles) {
        bakedGlyph.drawRectangle(rectangle2, this.matrix, vertexConsumer2, this.light);
      }
    }

    return this.x;
  }

  void drawGlyphs() {
    for (BakedGlyph.DrawnGlyph drawnGlyph : this.glyphs) {
      BakedGlyph bakedGlyph = drawnGlyph.glyph();
      VertexConsumer vertexConsumer =
          this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
      bakedGlyph.draw(drawnGlyph, this.matrix, vertexConsumer, this.light);
    }
  }

  public float getForegroundZIndex() {
    return this.swapZIndex ? 0.01F : -0.01F;
  }

  private float getBackgroundZIndex() {
    return this.swapZIndex ? -0.01F : 0.01F;
  }
}
