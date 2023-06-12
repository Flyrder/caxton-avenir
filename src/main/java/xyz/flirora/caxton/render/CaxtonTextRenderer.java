package xyz.flirora.caxton.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.Drawer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.Window;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontOptions;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;
import xyz.flirora.caxton.layout.*;
import xyz.flirora.caxton.mixin.TextRendererDrawerAccessor;

import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderer {
    // Copied from TextRenderer
    private static final Vector3f FORWARD_SHIFT = new Vector3f(0.0f, 0.0f, 0.03f);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Function<Identifier, FontStorage> fontStorageAccessor;
    private final CaxtonTextHandler handler;
    private final TextRenderer vanillaTextRenderer;
    private final Random RANDOM = Random.createLocal();
    private CaxtonGlyphCache cache;
    private final Supplier<CaxtonGlyphCache> cacheSupplier;
    private final CaxtonTextRenderLayers renderLayers;
    public boolean rtl;

    public CaxtonTextRenderer(Function<Identifier, FontStorage> fontStorageAccessor, TextRenderer vanillaTextRenderer, Supplier<CaxtonGlyphCache> cacheSupplier) {
        this.fontStorageAccessor = fontStorageAccessor;
        this.handler = new CaxtonTextHandler(fontStorageAccessor, vanillaTextRenderer.getTextHandler());
        this.vanillaTextRenderer = vanillaTextRenderer;
        this.cacheSupplier = cacheSupplier;
        this.renderLayers = new CaxtonTextRenderLayers();
    }

    public static CaxtonTextRenderer getInstance() {
        return ((HasCaxtonTextRenderer) MinecraftClient.getInstance().textRenderer).getCaxtonTextRenderer();
    }

    public static CaxtonTextRenderer getAdvanceValidatingInstance() {
        return ((HasCaxtonTextRenderer) MinecraftClient.getInstance().advanceValidatingTextRenderer).getCaxtonTextRenderer();
    }

    // Copy of TextRenderer.tweakTransparency
    private static int tweakTransparency(int argb) {
        if ((argb & 0xFC000000) == 0) {
            return argb | 0xFF000000;
        }
        return argb;
    }

    private static float getMinThickness(Matrix4f modelView) {
        // I don’t want to have to deal with computing this for every underlined character
        if (modelView.isAffine()) {
            Window window = MinecraftClient.getInstance().getWindow();
            Vector3f v = new Vector3f(0, 1, 0);
            modelView.transformDirection(v);
            RenderSystem.getProjectionMatrix().transformDirection(v);
            v.mul(window.getWidth() / 2.0f, window.getHeight() / 2.0f, 0);
            float fontUnitYPx = MathHelper.sqrt(v.x * v.x + v.y * v.y);
            return 1.0f / fontUnitYPx;
        }
        return 0.0f;
    }

    public Function<Identifier, FontStorage> getFontStorageAccessor() {
        return fontStorageAccessor;
    }

    private FontStorage getFontStorage(Identifier id) {
        return this.fontStorageAccessor.apply(id);
    }

    public float drawLayer(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, TextRenderer.TextLayerType layerType, int backgroundColor, int light, int leftmostCodePoint, float maxWidth) {
        CaxtonText runGroups = CaxtonText.fromFormatted(text, fontStorageAccessor, Style.EMPTY, false, this.rtl, handler.getCache());
        float newX = drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, layerType, backgroundColor, light, runGroups, leftmostCodePoint, maxWidth, getMinThickness(matrix));
        if (!shadow) this.rtl = false;
        return newX;
    }

    public float drawLayer(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, TextRenderer.TextLayerType layerType, int backgroundColor, int light, int leftmostCodePoint, float maxWidth) {
        CaxtonText runGroups = CaxtonText.from(text, fontStorageAccessor, false, this.rtl, handler.getCache());
        return drawRunGroups(x, y, color, shadow, matrix, vertexConsumerProvider, layerType, backgroundColor, light, runGroups, leftmostCodePoint, maxWidth, getMinThickness(matrix));
    }

    public void drawWithOutline(OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light, boolean addCorners) {
        boolean reuseDrawer = CaxtonModClient.CONFIG.reuseTextRendererDrawer;
        Threshold NO_THRESHOLD = new Threshold(-1);
        float minThickness = getMinThickness(matrix);

        CaxtonText runGroups = CaxtonText.from(text, fontStorageAccessor, false, this.rtl, handler.getCache());
        int effectiveOutlineColor = tweakTransparency(outlineColor);
        int effectiveColor = tweakTransparency(color);

        TextRenderer.Drawer outlineDrawer = vanillaTextRenderer.new Drawer(vertexConsumers, 0.0f, 0.0f, effectiveOutlineColor, false, matrix, TextRenderer.TextLayerType.NORMAL, light);
        TextRenderer.Drawer centralDrawer = vanillaTextRenderer.new Drawer(vertexConsumers, x, y, effectiveColor, false, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light);
        boolean usedDrawer = false;

        for (RunGroup runGroup : runGroups.runGroups()) {
            ConfiguredCaxtonFont font = runGroup.getFont();
            if (font == null) {
                if (!reuseDrawer) {
                    if (usedDrawer) {
                        outlineDrawer = vanillaTextRenderer.new Drawer(vertexConsumers, 0.0f, 0.0f, effectiveOutlineColor, false, matrix, TextRenderer.TextLayerType.NORMAL, light);
                        centralDrawer = vanillaTextRenderer.new Drawer(vertexConsumers, x, y, effectiveColor, false, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light);
                    }
                    usedDrawer = true;
                }
                MutableFloat xBox = new MutableFloat();
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dy = -1; dy <= 1; ++dy) {
                        if (dx == 0 && dy == 0) continue;
                        if (!addCorners && dx != 0 && dy != 0) continue;
                        xBox.setValue(x);
                        int dxf = dx, dyf = dy;
                        TextRenderer.Drawer outlineDrawer2 = outlineDrawer;
                        runGroup.acceptRender((index, style, codePoint) -> {
                            int index2 = runGroup.getCharOffset() + index;

                            FontStorage fontStorage = this.getFontStorage(style.getFont());
                            Glyph glyph = fontStorage.getGlyph(codePoint, false);
                            float shadowOffset = glyph.getShadowOffset();

                            ((TextRendererDrawerAccessor) outlineDrawer2).setX(xBox.floatValue() + dxf * shadowOffset);
                            ((TextRendererDrawerAccessor) outlineDrawer2).setY(y + dyf * shadowOffset);
                            xBox.add(glyph.getAdvance(style.isBold()));
                            return outlineDrawer2.accept(index2, style, codePoint);
                        });
                    }
                }
                ((TextRendererDrawerAccessor) centralDrawer).setX(x);
                TextRenderer.Drawer centralDrawer2 = centralDrawer;
                runGroup.acceptRender((index, style, codePoint) -> {
                    int index2 = runGroup.getCharOffset() + index;
                    return centralDrawer2.accept(index2, style, codePoint);
                });
                x = ((TextRendererDrawerAccessor) centralDrawer).getX();
            } else {
                ShapingResult[] shapingResults = runGroup.getShapingResults();

                // FIXME (hard): underlines and strikethroughs don’t render
                //  properly with this strategy
                for (int i = 0; i < shapingResults.length; i++) {
                    ShapingResult shapingResult = shapingResults[i];
                    if (font.font().getOptions().fontTech().hasTwoPhasedOutlineTextRendering()) {
                        drawShapedRun(shapingResult, runGroup, i, x, y, effectiveOutlineColor, 0, false, matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, light, outlineDrawer, NO_THRESHOLD, Float.POSITIVE_INFINITY, minThickness, font.font().getGlyphCount());
                        x = drawShapedRun(shapingResult, runGroup, i, x, y, effectiveColor, 0, false, matrix, vertexConsumers, TextRenderer.TextLayerType.POLYGON_OFFSET, light, outlineDrawer, NO_THRESHOLD, Float.POSITIVE_INFINITY, minThickness, 0);
                    } else {
                        x = drawShapedRun(shapingResult, runGroup, i, x, y, effectiveColor, effectiveOutlineColor, false, matrix, vertexConsumers, TextRenderer.TextLayerType.POLYGON_OFFSET, light, outlineDrawer, NO_THRESHOLD, Float.POSITIVE_INFINITY, minThickness, 0);
                    }
                }
            }
        }
    }

    public float draw(CaxtonText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int backgroundColor, int light, int leftmostCodePoint, float maxWidth) {
        float minThickness = getMinThickness(matrix);
        TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;
        color = tweakTransparency(color);
        Matrix4f matrix4f = new Matrix4f(matrix);
        if (shadow) {
            this.drawRunGroups(x, y, color, true, matrix, vertexConsumerProvider, layerType, backgroundColor, light, text, leftmostCodePoint, maxWidth, minThickness);
            matrix4f.translate(FORWARD_SHIFT);
        }
        x = this.drawRunGroups(x, y, color, false, matrix4f, vertexConsumerProvider, layerType, backgroundColor, light, text, leftmostCodePoint, maxWidth, minThickness);
        return (int) x + (shadow ? 1 : 0);
    }

    private float drawRunGroups(
            float x, float y,
            int color, boolean shadow,
            Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider,
            TextRenderer.TextLayerType layerType, int backgroundColor, int light,
            CaxtonText text, int leftmostCodePoint, float maxWidth, float minThickness) {
        boolean reuseDrawer = CaxtonModClient.CONFIG.reuseTextRendererDrawer;
//        System.err.println(text);
        getCache();
        Threshold threshold = new Threshold(leftmostCodePoint);
        float origX = x;
        float maxX = x + maxWidth;
        // We want to reuse the same TextRenderer.Drawer instance to improve
        // performance; however, ImmediatelyFast adds some extra fields to
        // the class in order to optimize text rendering. In particular, it saves
        // the last render layer for which a vertex consumer was requested from
        // the drawer’s vertex consumer provider and the corresponding vertex
        // consumer, reducing the need to call VertexConsumerProvider#getBuffer.
        // However, the return value of VertexConsumerProvider#getBuffer is only
        // valid until the next call to that method. This means that if a Caxton text
        // object has a Caxton run group between two legacy run groups, then the
        // “last vertex consumer” field stored by IF can become invalidated. In
        // practice, this conflict results in a crash.
        // To avoid this, we add an option to create a fresh TextRenderer.Drawer for
        // every legacy run group.
        // See <https://github.com/RaphiMC/ImmediatelyFast/blob/1.20/src/main/java/net/raphimc/immediatelyfast/injection/mixins/fast_text_lookup/MixinTextRenderer_Drawer.java>,
        //     <https://gitlab.com/Kyarei/caxton/-/issues/50>
        TextRenderer.Drawer drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, layerType, light);
        boolean usedDrawer = false;
        for (RunGroup runGroup : text.runGroups()) {
            if (threshold.shouldSkip(runGroup)) {
                continue;
            }
            if (x >= maxX) break;
            if (!reuseDrawer) {
                if (usedDrawer)
                    drawer = vanillaTextRenderer.new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, layerType, light);
                usedDrawer = true;
            }
            if (runGroup.getFont() == null) {
                ((TextRendererDrawerAccessor) drawer).setX(x);
                var drawer2 = drawer;
                runGroup.acceptRender((index, style, codePoint) -> {
                    int index2 = runGroup.getCharOffset() + index;
                    if (threshold.updateLegacy(index2)) {
                        return true;
                    }
                    if (((TextRendererDrawerAccessor) drawer2).getX() >= maxX + handler.getWidth(codePoint, style))
                        return false;
                    return drawer2.accept(index2, style, codePoint);
                });
                x = drawer.drawLayer(0, x);
            } else {
                ShapingResult[] shapingResults = runGroup.getShapingResults();

                for (int index = 0; index < shapingResults.length; ++index) {
                    ShapingResult shapingResult = shapingResults[index];
                    x = drawShapedRun(shapingResult, runGroup, index, x, y, color, 0, shadow, matrix, vertexConsumerProvider, layerType, light, drawer, threshold, maxX, minThickness, 0);
                }
            }
        }
        ((TextRendererDrawerAccessor) drawer).setX(x);
        drawer.drawLayer(backgroundColor, origX);
        return x;
    }

    private float drawShapedRun(
            ShapingResult shapedRun,
            RunGroup runGroup,
            int index,
            float x, float y,
            int color, int outlineColor, boolean shadow,
            Matrix4f matrix, VertexConsumerProvider vertexConsumers,
            TextRenderer.TextLayerType layerType, int light,
            TextRenderer.Drawer drawer,
            Threshold threshold, float maxX, float minThickness,
            int glyphOffset) {
        if (x >= maxX) return x;

        ConfiguredCaxtonFont configuredFont = runGroup.getFont();
        CaxtonFont font = configuredFont.font();
        CaxtonFontOptions options = font.getOptions();
        CaxtonGlyphCache.Font cacheForFont = cache.forFont(font);

        assert glyphOffset + font.getGlyphCount() <= font.getTlistSize();

        double shrink = options.shrinkage();
        int margin = options.margin();
        float shadowOffset = configuredFont.shadowOffset();
        float pageSize = CaxtonAtlas.PAGE_SIZE;

        int offset = runGroup.getBidiRuns()[3 * index];

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

        // I don’t want to have to deal with computing this for every underlined character
        if (dyu < minThickness) dyu = minThickness;
        if (dys < minThickness) dys = minThickness;

        float y0u = yu + 0.5f * dyu;
        float y1u = yu - 0.5f * dyu;
        float y0s = ys + 0.5f * dys;
        float y1s = ys - 0.5f * dys;

        float brightnessMultiplier = shadow ? 0.25f : 1.0f;
        float baseBlue = (color & 0xFF) / 255.0f * brightnessMultiplier;
        float baseGreen = ((color >> 8) & 0xFF) / 255.0f * brightnessMultiplier;
        float baseRed = ((color >> 16) & 0xFF) / 255.0f * brightnessMultiplier;
        float alpha = ((color >> 24) & 0xFF) / 255.0f;

        // Will the render layers accept two colors per vertex instead of one?
        boolean twoColored = layerType == TextRenderer.TextLayerType.POLYGON_OFFSET && !options.fontTech().hasTwoPhasedOutlineTextRendering();
        float olBlue = (outlineColor & 0xFF) / 255.0f * brightnessMultiplier;
        float olGreen = ((outlineColor >> 8) & 0xFF) / 255.0f * brightnessMultiplier;
        float olRed = ((outlineColor >> 16) & 0xFF) / 255.0f * brightnessMultiplier;
        float olAlpha = ((outlineColor >> 24) & 0xFF) / 255.0f;

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

            var styleColorObj = style.getColor();
            float red = baseRed, green = baseGreen, blue = baseBlue;
            if (styleColorObj != null) {
                int styleColor = styleColorObj.getRgb();
                red = ((styleColor >> 16) & 0xFF) / 255.0f * brightnessMultiplier;
                green = ((styleColor >> 8) & 0xFF) / 255.0f * brightnessMultiplier;
                blue = (styleColor & 0xFF) / 255.0f * brightnessMultiplier;
            }

            int advanceX = shapedRun.advanceX(i);
            int offsetX = shapedRun.offsetX(i);
            int offsetY = shapedRun.offsetY(i);
            int gx = cumulAdvanceX + offsetX;

            {
                long atlasLoc = cacheForFont.getOrCreateAtlasLocation(glyphOffset + glyphId);
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

                RenderLayer renderLayer = renderLayers.text(atlasPage, layerType, options.fontTech(), configuredFont.blur());
                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

                // Draw the quad

                float x0 = (float) (x + (gx - shrink * margin) * scale);
                float y1 = (float) (baselineY + (-offsetY + shrink * margin) * scale);
                float u0 = atlasX / pageSize;
                float v0 = atlasY / pageSize;
                float x1 = (float) (x + (gx + shrink * (atlasWidth - margin)) * scale);
                float y0 = (float) (baselineY + (-offsetY - shrink * (atlasHeight - margin)) * scale);
                float u1 = (atlasX + atlasWidth) / pageSize;
                float v1 = (atlasY + atlasHeight) / pageSize;

                if (x1 >= maxX) break;

                if (shadow) {
                    x0 += shadowOffset;
                    x1 += shadowOffset;
                    y0 += shadowOffset;
                    y1 += shadowOffset;
                }

                vertexConsumer.vertex(matrix, x0, y0, 0.0f)
                        .color(red, green, blue, alpha);
                if (twoColored)
                    vertexConsumer.color(olRed, olGreen, olBlue, olAlpha);
                vertexConsumer.texture(u0, v0)
                        .light(light)
                        .next();
                vertexConsumer.vertex(matrix, x0, y1, 0.0f)
                        .color(red, green, blue, alpha);
                if (twoColored)
                    vertexConsumer.color(olRed, olGreen, olBlue, olAlpha);
                vertexConsumer.texture(u0, v1)
                        .light(light)
                        .next();
                vertexConsumer.vertex(matrix, x1, y1, 0.0f)
                        .color(red, green, blue, alpha);
                if (twoColored)
                    vertexConsumer.color(olRed, olGreen, olBlue, olAlpha);
                vertexConsumer.texture(u1, v1)
                        .light(light)
                        .next();
                vertexConsumer.vertex(matrix, x1, y0, 0.0f)
                        .color(red, green, blue, alpha);
                if (twoColored)
                    vertexConsumer.color(olRed, olGreen, olBlue, olAlpha);
                vertexConsumer.texture(u1, v0)
                        .light(light)
                        .next();
            }

            float x0a = x + cumulAdvanceX * scale;
            float x1a = x + (cumulAdvanceX + advanceX) * scale;
            if (style.isUnderlined()) {
                if (twoColored) {
                    ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a - ou, y0u + ou, x1a + ou, y1u - ou, 0.005f, olRed, olGreen, olBlue, olAlpha));
                }
                ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a, y0u, x1a, y1u, 0.01f, red, green, blue, alpha));
            }
            if (style.isStrikethrough()) {
                if (twoColored) {
                    ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a - os, y0s + os, x1a + os, y1s - os, 0.005f, olRed, olGreen, olBlue, olAlpha));
                }
                ((TextRendererDrawerAccessor) drawer).callAddRectangle(new GlyphRenderer.Rectangle(x0a, y0s, x1a, y1s, 0.01f, red, green, blue, alpha));
            }

            cumulAdvanceX += advanceX;
        }
        return x + cumulAdvanceX * scale - configuredFont.shiftX();
    }

    public CaxtonGlyphCache getCache() {
        if (cache == null) cache = cacheSupplier.get();
        return cache;
    }

    public void clearCaches() {
        this.handler.clearCaches();
        LOGGER.info("Cleared layout caches");
        if (this.cache != null) this.cache.clear();
        this.renderLayers.clear();
        LOGGER.info("Cleared rendering caches");
    }

    public CaxtonTextHandler getHandler() {
        return handler;
    }
}
