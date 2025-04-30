// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.flirora.caxton.font.CaxtonFont;
import xyz.flirora.caxton.font.CaxtonFontOptions;
import xyz.flirora.caxton.font.ConfiguredCaxtonFont;

public class CaxtonGlyphCache {
  private final CaxtonAtlas atlas;
  private final Map<CaxtonFont, Font> cache = new HashMap<>();
  private static final Logger LOGGER = LogUtils.getLogger();

  public CaxtonGlyphCache(TextureManager textureManager) {
    this.atlas = new CaxtonAtlas(textureManager);
  }

  public Font forFont(CaxtonFont font) {
    return cache.computeIfAbsent(
        font,
        f -> {
          LOGGER.info("Adding entry for font {}", font);
          return new Font(font, new Int2LongOpenHashMap(), new Int2ObjectOpenHashMap<>());
        });
  }

  public Identifier getAtlasPage(int pageNum) {
    return atlas.getAtlasPage(pageNum);
  }

  public CaxtonAtlas.Page getAtlasPageTexture(int pageNum) {
    int pageCount = this.atlas.getNumPages();
    if (pageNum >= pageCount) {
      LOGGER.error("*** ATLAS PAGE INDEX OUT OF BOUNDS: {} >= {} ***", pageNum, pageCount);
      LOGGER.error("This probably indicates a race condition.");
      LOGGER.error("See <https://gitlab.com/Kyarei/caxton/-/issues/44> for more details.");
      throw new IndexOutOfBoundsException("Al quira Prenia Callta caprinini");
    }
    return atlas.getAtlasPageTexture(pageNum);
  }

  // Hack to avoid crashing when loading a server resource pack.
  public synchronized void clear() {
    LOGGER.info(
        "Clearing glyph atlas with {} pages and {} fonts", atlas.getNumPages(), cache.size());
    atlas.clear();
    cache.clear();
  }

  public CaxtonAtlas getAtlas() {
    return atlas;
  }

  public Map<CaxtonFont, Font> getLocations() {
    return cache;
  }

  public class Font {
    private final CaxtonFont font;
    private final Int2LongMap entry;
    private final Int2ObjectMap<BakedGlyph> entryBaked;

    private Font(CaxtonFont font, Int2LongMap entry, Int2ObjectMap<BakedGlyph> entryBaked) {
      this.font = font;
      this.entry = entry;
      this.entryBaked = entryBaked;
    }

    public Int2LongMap getEntry() {
      return this.entry;
    }

    public long getOrCreateAtlasLocation(int glyphId) {
      return entry.computeIfAbsent(
          glyphId,
          id -> {
            long tlLoc = font.getTlistLocation(glyphId, 0);
            int width = (int) (tlLoc & 0xFFFF);
            int height = (int) ((tlLoc >> 16) & 0xFFFF);
            int numMipmapLevels = font.getNumMipmapLevels();
            long location =
                CaxtonGlyphCache.this.atlas.insert(
                    width,
                    height,
                    1,
                    font.getOptions().range(),
                    numMipmapLevels,
                    font.getPixelFormat());
            if (location == 0) return location;

            GpuTexture page =
                atlas.getAtlasPageTexture(CaxtonAtlas.getPage(location)).getGlTexture();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

            for (int mipmap = 0; mipmap < numMipmapLevels; ++mipmap) {
              tlLoc = font.getTlistLocation(glyphId, mipmap);
              width = (int) (tlLoc & 0xFFFF);
              height = (int) ((tlLoc >> 16) & 0xFFFF);
              int offset = (int) (tlLoc >>> 32);
              NativeImage nativeImage =
                  new NativeImage(
                      font.getPixelFormat(),
                      width,
                      height,
                      false,
                      font.getPixelData(mipmap) + 4 * Integer.toUnsignedLong(offset));
              commandEncoder.writeToTexture(
                  page,
                  nativeImage,
                  mipmap,
                  CaxtonAtlas.getX(location) >> mipmap,
                  CaxtonAtlas.getY(location) >> mipmap,
                  width,
                  height,
                  0,
                  0);
              // Since nativeImage uses borrowed memory,
              // we should not close it
            }
            return location;
          });
    }

    public @Nullable BakedGlyph getBakedGlyph(
        int glyphId,
        CaxtonTextRenderLayers renderLayers,
        CaxtonGlyphCache cache,
        ConfiguredCaxtonFont configuredFont) {
      return this.entryBaked.computeIfAbsent(
          glyphId,
          id -> {
            long atlasLoc = this.getOrCreateAtlasLocation(glyphId);
            if (atlasLoc == 0) {
              return null;
            }

            int atlasX = CaxtonAtlas.getX(atlasLoc);
            int atlasY = CaxtonAtlas.getY(atlasLoc);
            int atlasWidth = CaxtonAtlas.getW(atlasLoc);
            int atlasHeight = CaxtonAtlas.getH(atlasLoc);
            int atlasPageIndex = CaxtonAtlas.getPage(atlasLoc);
            CaxtonAtlas.Page atlasPage = cache.getAtlasPageTexture(atlasPageIndex);

            float pageSize = CaxtonAtlas.PAGE_SIZE;
            float u0 = atlasX / pageSize;
            float v0 = atlasY / pageSize;
            float u1 = (atlasX + atlasWidth) / pageSize;
            float v1 = (atlasY + atlasHeight) / pageSize;

            CaxtonFontOptions options = font.getOptions();
            double shrink = options.shrinkage();
            int margin = options.margin();
            float scale = configuredFont.getScale();
            float x0 = (float) ((-shrink * margin) * scale);
            float y1 = (float) ((shrink * margin) * scale);
            float x1 = (float) ((shrink * (atlasWidth - margin)) * scale);
            float y0 = (float) ((-shrink * (atlasHeight - margin)) * scale);

            return new BakedGlyph(
                renderLayers.renderLayerSet(atlasPage, options.fontTech(), configuredFont.blur()),
                atlasPage.getGlTexture(),
                u0,
                u1,
                v0,
                v1,
                x0,
                x1,
                y0,
                y1);
          });
    }
  }
}
