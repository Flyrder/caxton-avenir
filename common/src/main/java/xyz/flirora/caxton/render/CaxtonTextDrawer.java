// (C) 2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import static xyz.flirora.caxton.layout.Interleaving.SHAPING_RUN_OFF_START;
import static xyz.flirora.caxton.layout.Interleaving.SHAPING_RUN_STRIDE;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.*;
import net.minecraft.client.gui.render.state.TextRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.RunGroup;
import xyz.flirora.caxton.layout.ShapingResult;
import xyz.flirora.caxton.layout.Threshold;
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
  private final float maxX;
  private final Threshold threshold;
  private final Random random = Random.createLocal();
  private boolean outline = false;

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
      boolean swapZIndex,
      float maxX,
      Threshold threshold) {
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
    this.maxX = maxX;
    this.threshold = threshold;
  }

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      float x,
      float y,
      int color,
      boolean shadow,
      TextRenderer.TextLayerType layerType,
      int light,
      float maxX,
      Threshold threshold) {
    this(textRenderer, x, y, color, 0, shadow, layerType, light, true, maxX, threshold);
  }

  public CaxtonTextDrawer outlined() {
    this.outline = true;
    return this;
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
    if (this.x >= this.maxX) {
      return false;
    }

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
              this.y + shadowOffset + 3.5f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 4.5f,
              this.getForegroundZIndex(),
              color,
              shadowColor,
              shadowOffset));
    }
    if (style.isUnderlined()) {
      this.addRectangle(
          new BakedGlyph.Rectangle(
              x + shadowOffset,
              this.y + shadowOffset + 8.0f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 9.0f,
              this.getForegroundZIndex(),
              color,
              shadowColor,
              shadowOffset));
    }

    this.x += advance;
    this.threshold.updateLegacy(index);
    return true;
  }

  public boolean acceptShapedRun(
      ShapingResult shapedRun,
      RunGroup runGroup,
      int index,
      CaxtonGlyphCache cache,
      int glyphOffset,
      float minThickness,
      CaxtonTextRenderLayers renderLayers) {
    if (this.x >= this.maxX) {
      return false;
    }
    float origX = x;

    ConfiguredCaxtonFont configuredFont = runGroup.getFont();
    CaxtonFont font = configuredFont.font();
    CaxtonGlyphCache.Font cacheForFont = cache.forFont(font);

    assert glyphOffset + font.getGlyphCount() <= font.getTlistSize();
    float shadowOffset = configuredFont.shadowOffset();
    float textZ = shadow ? 0.03f : 0.0f;

    int offset = runGroup.getBidiRuns()[SHAPING_RUN_STRIDE * index + SHAPING_RUN_OFF_START];

    int underlinePosition = font.getMetrics(CaxtonFont.Metrics.UNDERLINE_POSITION);
    int underlineThickness = font.getMetrics(CaxtonFont.Metrics.UNDERLINE_THICKNESS);
    int strikeoutPosition = font.getMetrics(CaxtonFont.Metrics.STRIKEOUT_POSITION);
    int strikeoutThickness = font.getMetrics(CaxtonFont.Metrics.STRIKEOUT_THICKNESS);

    float scale = configuredFont.getScale();
    float baselineY = y + 7.0f + configuredFont.shiftY();
    x += configuredFont.shiftX();

    float yu = baselineY - underlinePosition * scale;
    float ys = baselineY - strikeoutPosition * scale;
    float dyu = underlineThickness * scale;
    float dys = strikeoutThickness * scale;
    float ou = 0.5f * dyu, os = 0.5f * dys;

    // I don’t want to have to deal with computing this for every underlined
    // character
    if (dyu < minThickness) dyu = minThickness;
    if (dys < minThickness) dys = minThickness;

    float y0u = yu + 0.5f * dyu;
    float y1u = yu - 0.5f * dyu;
    float y0s = ys + 0.5f * dys;
    float y1s = ys - 0.5f * dys;

    int numGlyphs = shapedRun.numGlyphs();
    int cumulAdvanceX = 0;

    for (int i = 0; i < numGlyphs; ++i) {
      int glyphId = shapedRun.glyphId(i);
      int clusterIndex = shapedRun.clusterIndex(i);

      if (threshold.updateCaxton(runGroup, index, shapedRun, i)) {
        continue;
      }

      Style style = runGroup.getStyleAt(offset + clusterIndex);
      if (style.isObfuscated()) {
        long tlLoc = font.getTlistLocation(glyphId, 0);
        int width = (int) (tlLoc & 0xFFFF);
        IntList others = font.getGlyphsByWidth().get(width);
        glyphId = others.getInt(this.random.nextInt(others.size()));
      }
      int textColor = this.getTextColor(style.getColor());
      int shadowColor = this.getShadowColor(style, textColor);
      Style drawnStyle = style.withBold(false).withItalic(false);

      int advanceX = shapedRun.advanceX(i);
      int offsetX = shapedRun.offsetX(i);
      int offsetY = shapedRun.offsetY(i);
      int gx = cumulAdvanceX + offsetX;

      long atlasLoc = cacheForFont.getOrCreateAtlasLocation(glyphOffset + glyphId);
      BakedGlyph bakedGlyph =
          cacheForFont.getBakedGlyph(glyphId, renderLayers, cache, configuredFont, this.outline);
      if (bakedGlyph != null) {
        int atlasPageIndex = CaxtonAtlas.getPage(atlasLoc);

        long glyphBbox = font.getBbox(glyphId);
        short bbXMin = (short) glyphBbox;
        short bbYMin = (short) (glyphBbox >> 16);
        gx += bbXMin;
        offsetY += bbYMin;

        // Draw the quad

        float glyphX = x + gx * scale;
        float glyphY = -offsetY * scale + baselineY;
        this.glyphs.add(
            new BakedGlyph.DrawnGlyph(
                glyphX,
                glyphY,
                textColor,
                shadowColor,
                bakedGlyph,
                drawnStyle,
                0.0f,
                configuredFont.shadowOffset()));

        if (glyphX + advanceX >= this.maxX) break;
      }

      float x0a = x + cumulAdvanceX * scale;
      float x1a = x + (cumulAdvanceX + advanceX) * scale;
      if (style.isUnderlined()) {
        this.addRectangle(
            new BakedGlyph.Rectangle(
                x0a, y1u, x1a, y0u, this.getForegroundZIndex() + textZ, textColor));
        if (shadow) {
          this.addRectangle(
              new BakedGlyph.Rectangle(
                  x0a + shadowOffset,
                  y1u + shadowOffset,
                  x1a + shadowOffset,
                  y0u + shadowOffset,
                  this.getForegroundZIndex(),
                  shadowColor));
        }
      }
      if (style.isStrikethrough()) {
        this.addRectangle(
            new BakedGlyph.Rectangle(
                x0a, y1s, x1a, y0s, this.getForegroundZIndex() + textZ, textColor));
        if (shadow) {
          this.addRectangle(
              new BakedGlyph.Rectangle(
                  x0a + shadowOffset,
                  y1s + shadowOffset,
                  x1a + shadowOffset,
                  y0s + shadowOffset,
                  this.getForegroundZIndex(),
                  shadowColor));
        }
      }

      cumulAdvanceX += advanceX;
    }

    this.x = origX + cumulAdvanceX * scale - configuredFont.shiftX();
    return true;
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
              this.y - 1.0F,
              this.x,
              this.y + 9.0F,
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

  public TextRenderState buildState() {
    return new TextRenderState(
        this.shadow,
        this.color,
        this.backgroundColor,
        this.layerType,
        this.light,
        this.glyphs,
        this.rectangles,
        9,
        textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph(),
        this.x);
  }
}
