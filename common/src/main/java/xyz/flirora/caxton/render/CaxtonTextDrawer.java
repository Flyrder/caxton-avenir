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
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.RunGroup;
import xyz.flirora.caxton.layout.ShapingResult;
import xyz.flirora.caxton.layout.Threshold;

@Environment(EnvType.CLIENT)
public class CaxtonTextDrawer implements CharacterVisitor, TextRenderer.GlyphDrawable {
  private final CaxtonTextRenderer textRenderer;
  private final boolean shadow;
  private final int color;
  private final int backgroundColor;
  private final boolean trackSkeletons;
  private float x;
  private float y;
  private final List<TextDrawable.DrawnGlyph> glyphs;
  @Nullable private List<TextDrawable> rectangles;
  @Nullable private List<SkeletonGlyph> skeletonGlyphs;
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
      boolean trackSkeletons,
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
    this.trackSkeletons = trackSkeletons;
  }

  public CaxtonTextDrawer(
      CaxtonTextRenderer textRenderer,
      float x,
      float y,
      int color,
      boolean shadow,
      boolean trackSkeletons,
      float xRight,
      Threshold threshold) {
    this(textRenderer, x, y, color, 0, shadow, trackSkeletons, xRight, threshold);
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

  private void addGlyph(TextDrawable.DrawnGlyph glyph) {
    this.glyphs.add(glyph);
    this.updateTextBounds(
        glyph.getEffectiveMinX(),
        glyph.getEffectiveMinY(),
        glyph.getEffectiveMaxX(),
        glyph.getEffectiveMaxY());
  }

  public void addRectangle(TextDrawable rectangle) {
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

  private void addSkeletonGlyph(SkeletonGlyph glyph) {
    if (this.skeletonGlyphs == null) {
      this.skeletonGlyphs = new ArrayList<>();
    }
    this.skeletonGlyphs.add(glyph);
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

    GlyphProvider glyphProvider = this.textRenderer.getGlyphs(style.getFont());
    var glyph = glyphProvider.get(codePoint);
    return this.accept(index, style, glyph);
  }

  public boolean accept(int index, Style style, BakedGlyph glyph) {
    GlyphMetrics glyphMetrics = glyph.getMetrics();
    boolean bold = style.isBold();
    TextColor textColor = style.getColor();
    int renderColor = getRenderColor(textColor);
    int shadowColor = getShadowColor(style, renderColor);
    float advance = glyphMetrics.getAdvance(bold);
    float x = index == 0 ? this.x - 1.0f : this.x;
    float shadowOffset = glyphMetrics.getShadowOffset();
    float boldOffset = bold ? glyphMetrics.getBoldOffset() : 0.0f;

    TextDrawable.DrawnGlyph drawnGlyph =
        glyph.create(this.x, this.y, renderColor, shadowColor, style, boldOffset, shadowOffset);
    if (drawnGlyph != null) {
      this.addGlyph(drawnGlyph);
    } else if (this.trackSkeletons) {
      this.addSkeletonGlyph(new SkeletonGlyph(this.x, this.y, advance, 7.0F, 9.0F, style));
    }

    this.updateBackgroundBounds(this.x, this.y, advance);

    EffectGlyph rectangle = textRenderer.getFonts().getRectangleGlyph();
    if (style.isStrikethrough()) {
      this.addRectangle(
          rectangle.create(
              x + shadowOffset,
              this.y + shadowOffset + 3.5f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 4.5f,
              FOREGROUND_Z_INDEX,
              renderColor,
              shadowColor,
              shadowOffset));
    }
    if (style.isUnderlined()) {
      this.addRectangle(
          rectangle.create(
              x + shadowOffset,
              this.y + shadowOffset + 8.0f,
              this.x + shadowOffset + advance,
              this.y + shadowOffset + 9.0f,
              FOREGROUND_Z_INDEX,
              renderColor,
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
    EffectGlyph rectangle = textRenderer.getFonts().getRectangleGlyph();

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
      int textColor = this.getRenderColor(style.getColor());
      int shadowColor = this.getShadowColor(style, textColor);
      Style drawnStyle = style.withBold(false).withItalic(false);

      int advanceX = shapedRun.advanceX(i);
      int offsetX = shapedRun.offsetX(i);
      int offsetY = shapedRun.offsetY(i);
      int gx = cumulAdvanceX + offsetX;

      long atlasLoc = cacheForFont.getOrCreateAtlasLocation(glyphOffset + glyphId);
      BakedGlyph bakedGlyph =
          cacheForFont.getBakedGlyph(
              glyphId, renderLayers, cache, configuredFont, this.outline, advanceX * scale);
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
            bakedGlyph.create(
                glyphX,
                glyphY,
                textColor,
                shadowColor,
                drawnStyle,
                0.0f,
                configuredFont.shadowOffset()));

        this.updateBackgroundBounds(glyphX, this.y, advanceX * scale);

        if (glyphX + advanceX * scale >= this.xRight) break;
      }

      float x0a = x + cumulAdvanceX * scale;
      float x1a = x + (cumulAdvanceX + advanceX) * scale;
      if (style.isUnderlined()) {
        this.addRectangle(
            rectangle.create(
                x0a,
                y1u,
                x1a,
                y0u,
                FOREGROUND_Z_INDEX + textZ,
                textColor,
                shadowColor,
                shadowOffset));
      }
      if (style.isStrikethrough()) {
        this.addRectangle(
            rectangle.create(
                x0a,
                y1s,
                x1a,
                y0s,
                FOREGROUND_Z_INDEX + textZ,
                textColor,
                shadowColor,
                shadowOffset));
      }

      cumulAdvanceX += advanceX;
    }

    this.x = origX + cumulAdvanceX * scale - configuredFont.shiftX();
    return true;
  }

  public int getRenderColor(@Nullable TextColor override) {
    if (override != null && !this.outline) {
      int alpha = ColorHelper.getAlpha(this.color);
      int rgb = override.getRgb();
      return ColorHelper.withAlpha(alpha, rgb);
    } else {
      return this.color;
    }
  }

  public int getShadowColor(Style style, int renderColor) {
    Integer shadowColor = style.getShadowColor();
    if (shadowColor != null) {
      float textAlpha = ColorHelper.getAlphaFloat(renderColor);
      float shadowAlpha = ColorHelper.getAlphaFloat(shadowColor);
      return textAlpha != 1.0F
          ? ColorHelper.withAlpha(
              ColorHelper.channelFromFloat(textAlpha * shadowAlpha), shadowColor)
          : shadowColor;
    } else {
      return this.shadow ? ColorHelper.scaleRgb(renderColor, 0.25F) : 0;
    }
  }

  public float getForegroundZIndex() {
    return FOREGROUND_Z_INDEX;
  }

  @Override
  public void draw(TextRenderer.GlyphDrawer glyphDrawer) {
    BakedGlyph bakedGlyph = null;
    if (ColorHelper.getAlpha(this.backgroundColor) != 0) {
      glyphDrawer.drawRectangle(
          textRenderer
              .getFonts()
              .getRectangleGlyph()
              .create(
                  this.minBackgroundX,
                  this.minBackgroundY,
                  this.maxBackgroundX,
                  this.maxBackgroundY,
                  -0.001F,
                  this.backgroundColor,
                  0,
                  0.0F));
    }

    for (TextDrawable.DrawnGlyph drawnGlyph : this.glyphs) {
      glyphDrawer.drawGlyph(drawnGlyph);
    }

    if (this.rectangles != null) {
      for (TextDrawable rectangle2 : this.rectangles) {
        glyphDrawer.drawRectangle(rectangle2);
      }
    }

    if (this.skeletonGlyphs != null) {
      for (SkeletonGlyph skeletonGlyph : this.skeletonGlyphs) {
        glyphDrawer.addSkeleton(skeletonGlyph);
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
