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
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
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
public class CaxtonTextDrawer implements CharacterVisitor, TextRenderer.GlyphDrawable {
  private final CaxtonTextRenderer textRenderer;
  private final boolean shadow;
  private final int color;
  private final int backgroundColor;
  private float x;
  private float y;
  private final List<BakedGlyph.DrawnGlyph> glyphs;
  @Nullable private List<BakedGlyph.Rectangle> rectangles;
  private final float xRight;
  private final Threshold threshold;
  private final Random random = Random.createLocal();
  private boolean outline = false;

  private float minX = Float.MAX_VALUE;
  private float minY = Float.MAX_VALUE;
  private float maxX = Float.MIN_VALUE;
  private float maxY = Float.MIN_VALUE;
  private float minBackgroundX = Float.MAX_VALUE;
  private float minBackgroundY = Float.MAX_VALUE;
  private float maxBackgroundX = Float.MIN_VALUE;
  private float maxBackgroundY = Float.MIN_VALUE;

  private static final float FOREGROUND_Z_INDEX = 0.001F;
  private static final float BACKGROUND_Z_INDEX = -0.001F;

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      float x,
      float y,
      int color,
      int backgroundColor,
      boolean shadow,
      float xRight,
      Threshold threshold) {
    this.textRenderer = textRenderer;
    this.shadow = shadow;
    this.color = color;
    this.backgroundColor = backgroundColor;
    this.x = x;
    this.y = y;
    this.glyphs = new ArrayList<>();
    this.xRight = xRight;
    this.threshold = threshold;
  }

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      float x,
      float y,
      int color,
      boolean shadow,
      float xRight,
      Threshold threshold) {
    this(textRenderer, x, y, color, 0, shadow, xRight, threshold);
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

  private void updateTextBounds(float minX, float minY, float maxX, float maxY) {
    this.minX = Math.min(this.minX, minX);
    this.minY = Math.min(this.minY, minY);
    this.maxX = Math.max(this.maxX, maxX);
    this.maxY = Math.max(this.maxY, maxY);
  }

  private void updateBackgroundBounds(float x, float y, float width) {
    if (ColorHelper.getAlpha(this.backgroundColor) != 0) {
      this.minBackgroundX = Math.min(this.minBackgroundX, x - 1.0F);
      this.minBackgroundY = Math.min(this.minBackgroundY, y - 1.0F);
      this.maxBackgroundX = Math.max(this.maxBackgroundX, x + width);
      this.maxBackgroundY = Math.max(this.maxBackgroundY, y + 9.0F);
      this.updateTextBounds(
          this.minBackgroundX, this.minBackgroundY, this.maxBackgroundX, this.maxBackgroundY);
    }
  }

  private void addGlyph(BakedGlyph.DrawnGlyph glyph) {
    this.glyphs.add(glyph);
    this.updateTextBounds(
        glyph.getEffectiveMinX(),
        glyph.getEffectiveMinY(),
        glyph.getEffectiveMaxX(),
        glyph.getEffectiveMaxY());
  }

  public void addRectangle(BakedGlyph.Rectangle rectangle) {
    if (this.rectangles == null) {
      this.rectangles = new ArrayList<>();
    }
    this.rectangles.add(rectangle);
    this.updateTextBounds(
        rectangle.getEffectiveMinX(),
        rectangle.getEffectiveMinY(),
        rectangle.getEffectiveMaxX(),
        rectangle.getEffectiveMaxY());
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
    if (this.x >= this.xRight) {
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
      this.addGlyph(
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
              FOREGROUND_Z_INDEX,
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
              FOREGROUND_Z_INDEX,
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
    if (this.x >= this.xRight) {
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
        this.addGlyph(
            new BakedGlyph.DrawnGlyph(
                glyphX,
                glyphY,
                textColor,
                shadowColor,
                bakedGlyph,
                drawnStyle,
                0.0f,
                configuredFont.shadowOffset()));

        if (glyphX + advanceX * scale >= this.xRight) break;
      }

      float x0a = x + cumulAdvanceX * scale;
      float x1a = x + (cumulAdvanceX + advanceX) * scale;
      if (style.isUnderlined()) {
        this.addRectangle(
            new BakedGlyph.Rectangle(x0a, y1u, x1a, y0u, FOREGROUND_Z_INDEX + textZ, textColor));
        if (shadow) {
          this.addRectangle(
              new BakedGlyph.Rectangle(
                  x0a + shadowOffset,
                  y1u + shadowOffset,
                  x1a + shadowOffset,
                  y0u + shadowOffset,
                  FOREGROUND_Z_INDEX,
                  shadowColor));
        }
      }
      if (style.isStrikethrough()) {
        this.addRectangle(
            new BakedGlyph.Rectangle(x0a, y1s, x1a, y0s, FOREGROUND_Z_INDEX + textZ, textColor));
        if (shadow) {
          this.addRectangle(
              new BakedGlyph.Rectangle(
                  x0a + shadowOffset,
                  y1s + shadowOffset,
                  x1a + shadowOffset,
                  y0s + shadowOffset,
                  FOREGROUND_Z_INDEX,
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

  float drawLayer(
      float x,
      VertexConsumerProvider vertexConsumers,
      Matrix4f matrix,
      TextRenderer.TextLayerType layerType,
      int light) {
    BakedGlyph bakedGlyph = null;
    if (this.backgroundColor != 0) {
      BakedGlyph.Rectangle rectangle =
          new BakedGlyph.Rectangle(
              x - 1.0F,
              this.y - 1.0F,
              this.x,
              this.y + 9.0F,
              BACKGROUND_Z_INDEX,
              this.backgroundColor);
      bakedGlyph = this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(bakedGlyph.getLayer(layerType));
      bakedGlyph.drawRectangle(rectangle, matrix, vertexConsumer, light, false);
    }

    this.drawGlyphs(vertexConsumers, matrix, layerType, light);
    if (this.rectangles != null) {
      if (bakedGlyph == null) {
        bakedGlyph =
            this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      }

      VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(bakedGlyph.getLayer(layerType));

      for (BakedGlyph.Rectangle rectangle2 : this.rectangles) {
        bakedGlyph.drawRectangle(rectangle2, matrix, vertexConsumer2, light, false);
      }
    }

    return this.x;
  }

  void drawGlyphs(
      VertexConsumerProvider vertexConsumers,
      Matrix4f matrix,
      TextRenderer.TextLayerType layerType,
      int light) {
    for (BakedGlyph.DrawnGlyph drawnGlyph : this.glyphs) {
      BakedGlyph bakedGlyph = drawnGlyph.glyph();
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(bakedGlyph.getLayer(layerType));
      bakedGlyph.draw(drawnGlyph, matrix, vertexConsumer, light, false);
    }
  }

  public float getForegroundZIndex() {
    return FOREGROUND_Z_INDEX;
  }

  @Override
  public void draw(TextRenderer.GlyphDrawer glyphDrawer) {
    BakedGlyph bakedGlyph = null;
    if (ColorHelper.getAlpha(this.backgroundColor) != 0) {
      BakedGlyph.Rectangle rectangle =
          new BakedGlyph.Rectangle(
              this.minBackgroundX,
              this.minBackgroundY,
              this.maxBackgroundX,
              this.maxBackgroundY,
              -0.001F,
              this.backgroundColor);
      bakedGlyph = this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      glyphDrawer.drawRectangle(bakedGlyph, rectangle);
    }

    for (BakedGlyph.DrawnGlyph drawnGlyph : this.glyphs) {
      glyphDrawer.drawGlyph(drawnGlyph);
    }

    if (this.rectangles != null) {
      if (bakedGlyph == null) {
        bakedGlyph =
            this.textRenderer.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
      }

      for (BakedGlyph.Rectangle rectangle2 : this.rectangles) {
        glyphDrawer.drawRectangle(bakedGlyph, rectangle2);
      }
    }
  }

  @Override
  public @Nullable ScreenRect getScreenRect() {
    if (!(this.minX >= this.maxX) && !(this.minY >= this.maxY)) {
      int minX = MathHelper.floor(this.minX);
      int minY = MathHelper.floor(this.minY);
      int maxX = MathHelper.floor(this.maxX) + 1;
      int maxY = MathHelper.floor(this.maxY) + 1;
      return new ScreenRect(minX, minY, maxX - minX, maxY - minY);
    } else {
      return null;
    }
  }
}
