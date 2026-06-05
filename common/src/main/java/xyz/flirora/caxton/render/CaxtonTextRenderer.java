// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.*;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.window.Window;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.slf4j.Logger;
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

  private static float getMinThickness(Matrix4fc modelView) {
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

  public TextRenderer.GlyphDrawable drawWithOutline(
      OrderedText text, float x, float y, int outlineColor, boolean addCorners) {
    // FIXME: will this be set to the right matrix?
    float minThickness = getMinThickness(RenderSystem.getModelViewMatrixCopy());
    Threshold NO_THRESHOLD = new Threshold();

    CaxtonText runGroups =
        CaxtonText.from(text, fontStorageAccessor, validatesAdvance, this.rtl, handler.getCache());

    CaxtonTextDrawer outlineDrawer =
        new CaxtonTextDrawer(
                this, 0.0f, 0.0f, outlineColor, false, false, Float.POSITIVE_INFINITY, NO_THRESHOLD)
            .outlined();

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
        x = xBox.floatValue();
      } else {
        ShapingResult[] shapingResults = runGroup.getShapingResults();

        int outlinedGlyphOffset =
            font.font().getOptions().fontTech().offsetOutlineGlyphsByGlyphCounts()
                ? font.font().getGlyphCount()
                : 0;
        outlineDrawer.setX(x);
        outlineDrawer.setY(y);

        // FIXME (hard): underlines and strikethroughs don’t render
        // properly with this strategy
        for (int i = 0; i < shapingResults.length; i++) {
          ShapingResult shapingResult = shapingResults[i];

          outlineDrawer.acceptShapedRun(
              shapingResult,
              runGroup,
              i,
              this.cache,
              outlinedGlyphOffset,
              minThickness,
              this.renderLayers);
        }
        x = outlineDrawer.getX();
      }
    }
    outlineDrawer.clearRectangles();
    return outlineDrawer;
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
    float minThickness = getMinThickness(RenderSystem.getModelViewMatrixCopy());
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
    float minThickness = getMinThickness(RenderSystem.getModelViewMatrixCopy());

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
