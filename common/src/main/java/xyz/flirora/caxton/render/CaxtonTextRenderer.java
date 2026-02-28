// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import static xyz.flirora.caxton.layout.Interleaving.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.*;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.window.Window;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontOptions;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.*;
import xyz.flirora.caxton.mixin.FontManagerAccessor;
import xyz.flirora.caxton.mixin.FontManagerFontsAccessor;
import xyz.flirora.caxton.mixin.TextRendererAccessor;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
  // Copied from TextRenderer
  private static final Vector3f FORWARD_SHIFT = new Vector3f(0.0f, 0.0f, 0.03f);
  private static final Logger LOGGER = LogUtils.getLogger();
  private final CaxtonTextHandler handler;
  final TextRenderer vanillaTextRenderer;
  private final Random RANDOM = Random.createLocal();
  private CaxtonGlyphCache cache;
  private final Supplier<CaxtonGlyphCache> cacheSupplier;
  private final CaxtonTextRenderLayers renderLayers;
  public boolean rtl;
  private final boolean validatesAdvance;
  private final Function<Identifier, FontStorage> fontStorageAccessor;

  public CaxtonTextRenderer(
      TextRenderer vanillaTextRenderer,
      TextHandler vanillaTextHandler,
      Supplier<CaxtonGlyphCache> cacheSupplier) {
    this.vanillaTextRenderer = vanillaTextRenderer;
    this.fontStorageAccessor =
        ((FontManagerAccessor) ((FontManagerFontsAccessor) this.getFonts()).getOuter())
            ::callGetStorageInternal;
    this.handler = new CaxtonTextHandler(this.fontStorageAccessor, vanillaTextHandler);
    this.cacheSupplier = cacheSupplier;
    this.renderLayers = new CaxtonTextRenderLayers();
    this.validatesAdvance = ((FontManagerFontsAccessor) this.getFonts()).getAdvanceValidating();
  }

  public static CaxtonTextRenderer getInstance() {
    return ((HasCaxtonTextRenderer) MinecraftClient.getInstance().textRenderer)
        .getCaxtonTextRenderer();
  }

  public static CaxtonTextRenderer getAdvanceValidatingInstance() {
    return ((HasCaxtonTextRenderer) MinecraftClient.getInstance().advanceValidatingTextRenderer)
        .getCaxtonTextRenderer();
  }

  // Copy of TextRenderer.tweakTransparency
  private static int tweakTransparency(int argb) {
    if ((argb & 0xFC000000) == 0) {
      return argb | 0xFF000000;
    }
    return argb;
  }

  private static float getMinThickness(Matrix4f modelView) {
    // I don’t want to have to deal with computing this for every underlined
    // character
    if (modelView.isAffine()) {
      Window window = MinecraftClient.getInstance().getWindow();
      Vector3f v = new Vector3f(0, 1, 0);
      modelView.transformDirection(v);
      //      RenderSystem.getProjectionMatrix().transformDirection(v);
      v.mul(window.getWidth() / 2.0f, window.getHeight() / 2.0f, 0);
      float fontUnitYPx = MathHelper.sqrt(v.x * v.x + v.y * v.y);
      return 1.0f / fontUnitYPx;
    }
    return 0.0f;
  }

  public GlyphProvider getGlyphs(StyleSpriteSource source) {
    return ((TextRendererAccessor) this.vanillaTextRenderer).callGetGlyphs(source);
  }

  public FontManager.Fonts getFonts() {
    // This is the only implementor of GlyphsProvider
    return (FontManager.Fonts) ((TextRendererAccessor) this.vanillaTextRenderer).getFonts();
  }

  public Function<Identifier, FontStorage> getFontStorageAccessor() {
    return this.fontStorageAccessor;
  }

  public boolean validatesAdvance() {
    return this.validatesAdvance;
  }

  public void drawWithOutline(
      OrderedText text,
      float x,
      float y,
      int color,
      int outlineColor,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      int light,
      boolean addCorners) {
    Threshold NO_THRESHOLD = new Threshold();
    float minThickness = getMinThickness(matrix);

    CaxtonText runGroups =
        CaxtonText.from(text, fontStorageAccessor, validatesAdvance, this.rtl, handler.getCache());
    int effectiveOutlineColor = tweakTransparency(outlineColor);
    int effectiveColor = tweakTransparency(color);

    CaxtonTextDrawer outlineDrawer =
        new CaxtonTextDrawer(
                this,
                0.0f,
                0.0f,
                effectiveOutlineColor,
                false,
                false,
                Float.POSITIVE_INFINITY,
                NO_THRESHOLD)
            .outlined();
    CaxtonTextDrawer centralDrawer =
        new CaxtonTextDrawer(
            this, x, y, effectiveColor, false, false, Float.POSITIVE_INFINITY, NO_THRESHOLD);

    for (RunGroup runGroup : runGroups.runGroups()) {
      ConfiguredCaxtonFont font = runGroup.getFont();
      if (font == null) {
        MutableFloat xBox = new MutableFloat();
        for (int dx = -1; dx <= 1; ++dx) {
          for (int dy = -1; dy <= 1; ++dy) {
            if (dx == 0 && dy == 0) continue;
            if (!addCorners && dx != 0 && dy != 0) continue;
            xBox.setValue(x);
            int dxf = dx, dyf = dy;
            runGroup.acceptRender(
                (index, style, codePoint) -> {
                  int index2 = runGroup.getCharOffset() + index;

                  BakedGlyph glyph =
                      ((TextRendererAccessor) this.vanillaTextRenderer)
                          .callGetGlyph(codePoint, style);
                  float shadowOffset = glyph.getMetrics().getShadowOffset();

                  outlineDrawer.setX(xBox.floatValue() + dxf * shadowOffset);
                  outlineDrawer.setY(y + dyf * shadowOffset);
                  xBox.add(glyph.getMetrics().getAdvance(style.isBold()));
                  return outlineDrawer.accept(index2, style, codePoint);
                });
          }
        }
        centralDrawer.setX(x);
        runGroup.acceptRender(
            (index, style, codePoint) -> {
              int index2 = runGroup.getCharOffset() + index;
              return centralDrawer.accept(index2, style, codePoint);
            });
        x = centralDrawer.getX();
      } else {
        ShapingResult[] shapingResults = runGroup.getShapingResults();

        int outlinedGlyphOffset =
            font.font().getOptions().fontTech().offsetOutlineGlyphsByGlyphCounts()
                ? font.font().getGlyphCount()
                : 0;

        // FIXME (hard): underlines and strikethroughs don’t render
        // properly with this strategy
        for (int i = 0; i < shapingResults.length; i++) {
          ShapingResult shapingResult = shapingResults[i];
          drawShapedRun(
              shapingResult,
              runGroup,
              i,
              x,
              y,
              false,
              matrix,
              vertexConsumers,
              CaxtonTextLayerType.OUTLINE,
              light,
              outlineDrawer,
              NO_THRESHOLD,
              Float.POSITIVE_INFINITY,
              minThickness,
              outlinedGlyphOffset);
          x =
              drawShapedRun(
                  shapingResult,
                  runGroup,
                  i,
                  x,
                  y,
                  false,
                  matrix,
                  vertexConsumers,
                  CaxtonTextLayerType.POLYGON_OFFSET,
                  light,
                  centralDrawer,
                  NO_THRESHOLD,
                  Float.POSITIVE_INFINITY,
                  minThickness,
                  0);
        }
      }
      // FIXME: I’m sure we only need to do this at the end of the method, not every loop iteration
      outlineDrawer.draw(
          TextRenderer.GlyphDrawer.drawing(
              vertexConsumers, matrix, TextRenderer.TextLayerType.NORMAL, light));
      centralDrawer.draw(
          TextRenderer.GlyphDrawer.drawing(
              vertexConsumers, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light));
    }
  }

  private float drawShapedRun(
      ShapingResult shapedRun,
      RunGroup runGroup,
      int index,
      float x,
      float y,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      CaxtonTextLayerType layerType,
      int light,
      CaxtonTextDrawer drawer,
      Threshold threshold,
      float maxX,
      float minThickness,
      int glyphOffset) {
    if (x >= maxX) return x;

    ConfiguredCaxtonFont configuredFont = runGroup.getFont();
    CaxtonFont font = configuredFont.font();
    CaxtonFontOptions options = font.getOptions();
    CaxtonGlyphCache.Font cacheForFont = cache.forFont(font);
    EffectGlyph rectangle = this.getFonts().getRectangleGlyph();

    assert glyphOffset + font.getGlyphCount() <= font.getTlistSize();

    double shrink = options.shrinkage();
    int margin = options.margin();
    float shadowOffset = configuredFont.shadowOffset();
    float textZ = shadow ? 0.03f : 0.0f;
    float pageSize = CaxtonAtlas.PAGE_SIZE;

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
        glyphId = others.getInt(RANDOM.nextInt(others.size()));
      }
      int textColor = drawer.getRenderColor(style.getColor());
      int shadowColor = drawer.getShadowColor(style, textColor);

      int advanceX = shapedRun.advanceX(i);
      int offsetX = shapedRun.offsetX(i);
      int offsetY = shapedRun.offsetY(i);
      int gx = cumulAdvanceX + offsetX;

      long atlasLoc = cacheForFont.getOrCreateAtlasLocation(glyphOffset + glyphId);
      if (atlasLoc != 0) {
        int atlasX = CaxtonAtlas.getX(atlasLoc);
        int atlasY = CaxtonAtlas.getY(atlasLoc);
        int atlasWidth = CaxtonAtlas.getW(atlasLoc);
        int atlasHeight = CaxtonAtlas.getH(atlasLoc);
        int atlasPageIndex = CaxtonAtlas.getPage(atlasLoc);
        CaxtonAtlas.Page atlasPage = cache.getAtlasPageTexture(atlasPageIndex);

        long glyphBbox = font.getBbox(glyphId);
        short bbXMin = (short) glyphBbox;
        short bbYMin = (short) (glyphBbox >> 16);
        short bbXMax = (short) (glyphBbox >> 32);
        short bbYMax = (short) (glyphBbox >> 48);
        int bbWidth = ((int) bbXMax) - ((int) bbXMin);
        int bbHeight = ((int) bbYMax) - ((int) bbYMin);
        gx += bbXMin;
        offsetY += bbYMin;

        RenderLayer renderLayer = renderLayers.text(atlasPage, layerType, options.fontTech());
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

        // Draw the quad

        float x0 = (float) (x + (gx - shrink * margin) * scale);
        float y1 = (float) ((-offsetY + shrink * margin) * scale);
        float u0 = atlasX / pageSize;
        float v0 = atlasY / pageSize;
        float x1 = (float) (x + (gx + shrink * (atlasWidth - margin)) * scale);
        float y0 = (float) ((-offsetY - shrink * (atlasHeight - margin)) * scale);
        float u1 = (atlasX + atlasWidth) / pageSize;
        float v1 = (atlasY + atlasHeight) / pageSize;
        float lowerOffset = configuredFont.slant() * y0;
        float upperOffset = configuredFont.slant() * y1;
        y0 += baselineY;
        y1 += baselineY;

        if (x1 >= maxX) break;

        vertexConsumer.vertex(matrix, x0 + lowerOffset, y0, textZ).color(textColor);
        vertexConsumer.texture(u0, v0).light(light);
        vertexConsumer.vertex(matrix, x0 + upperOffset, y1, textZ).color(textColor);
        vertexConsumer.texture(u0, v1).light(light);
        vertexConsumer.vertex(matrix, x1 + upperOffset, y1, textZ).color(textColor);
        vertexConsumer.texture(u1, v1).light(light);
        vertexConsumer.vertex(matrix, x1 + lowerOffset, y0, textZ).color(textColor);
        vertexConsumer.texture(u1, v0).light(light);

        if (shadow) {
          x0 += shadowOffset;
          x1 += shadowOffset;
          y0 += shadowOffset;
          y1 += shadowOffset;
          vertexConsumer.vertex(matrix, x0 + lowerOffset, y0, 0.0f).color(shadowColor);
          vertexConsumer.texture(u0, v0).light(light);
          vertexConsumer.vertex(matrix, x0 + upperOffset, y1, 0.0f).color(shadowColor);
          vertexConsumer.texture(u0, v1).light(light);
          vertexConsumer.vertex(matrix, x1 + upperOffset, y1, 0.0f).color(shadowColor);
          vertexConsumer.texture(u1, v1).light(light);
          vertexConsumer.vertex(matrix, x1 + lowerOffset, y0, 0.0f).color(shadowColor);
          vertexConsumer.texture(u1, v0).light(light);
        }
      }

      float x0a = x + cumulAdvanceX * scale;
      float x1a = x + (cumulAdvanceX + advanceX) * scale;
      if (style.isUnderlined()) {
        drawer.addRectangle(
            rectangle.create(
                x0a,
                y0u,
                x1a,
                y1u,
                drawer.getForegroundZIndex() + textZ,
                textColor,
                shadowColor,
                shadowOffset));
      }
      if (style.isStrikethrough()) {
        drawer.addRectangle(
            rectangle.create(
                x0a,
                y0s,
                x1a,
                y1s,
                drawer.getForegroundZIndex() + textZ,
                textColor,
                shadowColor,
                shadowOffset));
      }

      cumulAdvanceX += advanceX;
    }
    return x + cumulAdvanceX * scale - configuredFont.shiftX();
  }

  public TextRenderer.GlyphDrawable prepare(
      CaxtonText text,
      float x,
      float y,
      int color,
      boolean shadow,
      boolean trackEmpty,
      int backgroundColor,
      int leftmostCodePoint,
      float maxWidth) {
    getCache();
    // FIXME: will this be set to the right matrix?
    float minThickness = getMinThickness(RenderSystem.getModelViewMatrix());
    CaxtonTextDrawer drawer =
        new CaxtonTextDrawer(
            this,
            x,
            y,
            color,
            backgroundColor,
            shadow,
            trackEmpty,
            x + maxWidth,
            new Threshold(leftmostCodePoint));
    for (RunGroup runGroup : text.runGroups()) {
      if (runGroup.getFont() == null) {
        runGroup.acceptRender(
            (index, style, codePoint) -> {
              int index2 = runGroup.getCharOffset() + index;
              return drawer.accept(index2, style, codePoint);
            });
      } else {
        ShapingResult[] shapingResults = runGroup.getShapingResults();

        for (int index = 0; index < shapingResults.length; ++index) {
          ShapingResult shapingResult = shapingResults[index];
          drawer.acceptShapedRun(
              shapingResult, runGroup, index, this.cache, 0, minThickness, this.renderLayers);
        }
      }
    }
    return drawer;
  }

  public TextRenderer.GlyphDrawable prepareOutlined(
      CaxtonText runGroups,
      float x,
      float y,
      int color,
      int outlineColor,
      int light,
      boolean addCorners) {
    getCache();
    Threshold NO_THRESHOLD = new Threshold();
    // FIXME: will this be set to the right matrix?
    float minThickness = getMinThickness(RenderSystem.getModelViewMatrix());

    int effectiveOutlineColor = tweakTransparency(outlineColor);
    int effectiveColor = tweakTransparency(color);

    CaxtonTextDrawer outlineDrawer =
        new CaxtonTextDrawer(
                this,
                0.0f,
                0.0f,
                effectiveOutlineColor,
                false,
                false,
                Float.POSITIVE_INFINITY,
                NO_THRESHOLD)
            .outlined();
    CaxtonTextDrawer centralDrawer =
        new CaxtonTextDrawer(
            this, x, y, effectiveColor, false, false, Float.POSITIVE_INFINITY, NO_THRESHOLD);

    for (RunGroup runGroup : runGroups.runGroups()) {
      ConfiguredCaxtonFont font = runGroup.getFont();
      if (font == null) {
        MutableFloat xBox = new MutableFloat();
        for (int dx = -1; dx <= 1; ++dx) {
          for (int dy = -1; dy <= 1; ++dy) {
            if (dx == 0 && dy == 0) continue;
            if (!addCorners && dx != 0 && dy != 0) continue;
            xBox.setValue(x);
            int dxf = dx, dyf = dy;
            runGroup.acceptRender(
                (index, style, codePoint) -> {
                  int index2 = runGroup.getCharOffset() + index;

                  BakedGlyph glyph =
                      ((TextRendererAccessor) this.vanillaTextRenderer)
                          .callGetGlyph(codePoint, style);
                  float shadowOffset = glyph.getMetrics().getShadowOffset();

                  outlineDrawer.setX(xBox.floatValue() + dxf * shadowOffset);
                  outlineDrawer.setY(y + dyf * shadowOffset);
                  xBox.add(glyph.getMetrics().getAdvance(style.isBold()));
                  return outlineDrawer.accept(index2, style, codePoint);
                });
          }
        }
        centralDrawer.setX(x);
        runGroup.acceptRender(
            (index, style, codePoint) -> {
              int index2 = runGroup.getCharOffset() + index;
              return centralDrawer.accept(index2, style, codePoint);
            });
        x = centralDrawer.getX();
      } else {
        ShapingResult[] shapingResults = runGroup.getShapingResults();

        int outlinedGlyphOffset =
            font.font().getOptions().fontTech().offsetOutlineGlyphsByGlyphCounts()
                ? font.font().getGlyphCount()
                : 0;

        for (int i = 0; i < shapingResults.length; i++) {
          ShapingResult shapingResult = shapingResults[i];
          centralDrawer.acceptShapedRun(
              shapingResult, runGroup, i, this.cache, 0, minThickness, this.renderLayers);
          outlineDrawer.setX(x);
          outlineDrawer.setY(y);
          outlineDrawer.acceptShapedRun(
              shapingResult,
              runGroup,
              i,
              this.cache,
              outlinedGlyphOffset,
              minThickness,
              this.renderLayers);
          x = centralDrawer.getX();
        }
      }
    }
    return new TextRenderer.GlyphDrawable() {
      @Override
      public void draw(TextRenderer.GlyphDrawer glyphDrawer) {
        outlineDrawer.draw(glyphDrawer);
        centralDrawer.draw(glyphDrawer);
      }

      @Override
      public @Nullable ScreenRect getScreenRect() {
        ScreenRect rect1 = outlineDrawer.getScreenRect();
        ScreenRect rect2 = centralDrawer.getScreenRect();
        if (rect1 == null) return rect2;
        if (rect2 == null) return rect1;
        int left = Math.min(rect1.getLeft(), rect2.getLeft());
        int right = Math.max(rect1.getRight(), rect2.getRight());
        int top = Math.min(rect1.getTop(), rect2.getTop());
        int bottom = Math.max(rect1.getBottom(), rect2.getBottom());
        return new ScreenRect(left, top, right - left, bottom - top);
      }
    };
  }

  public CaxtonGlyphCache getCache() {
    RenderSystem.assertOnRenderThread();
    if (cache == null) {
      cache = cacheSupplier.get();
    }
    return cache;
  }

  public void clearCaches() {
    RenderSystem.assertOnRenderThread();
    this.handler.clearCaches();
    LOGGER.info("Cleared layout caches");
    if (this.cache != null) {
      this.cache.clear();
    }
    this.renderLayers.clear();
    LOGGER.info("Cleared rendering caches");
  }

  public CaxtonTextHandler getHandler() {
    return handler;
  }

  public CaxtonText from(OrderedText text) {
    return CaxtonText.from(
        text, fontStorageAccessor, validatesAdvance, this.rtl, handler.getCache());
  }

  public CaxtonText fromForwards(String text, Style style) {
    return CaxtonText.fromForwards(
        text, fontStorageAccessor, style, validatesAdvance, this.rtl, handler.getCache());
  }
}
