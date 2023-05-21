package xyz.flirora.caxton.render;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import xyz.flirora.caxton.font.CaxtonFont;

import java.util.HashMap;
import java.util.Map;

public class CaxtonGlyphCache {
    private final CaxtonAtlas atlas;
    private final Map<CaxtonFont, Int2LongMap> cache = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    public CaxtonGlyphCache(TextureManager textureManager) {
        this.atlas = new CaxtonAtlas(textureManager);
    }

    public Font forFont(CaxtonFont font) {
        return new Font(font, cache.computeIfAbsent(font, f -> new Int2LongOpenHashMap()));
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
        atlas.clear();
        cache.clear();
    }

    public CaxtonAtlas getAtlas() {
        return atlas;
    }

    public Map<CaxtonFont, Int2LongMap> getLocations() {
        return cache;
    }

    public class Font {
        private final CaxtonFont font;
        private final Int2LongMap entry;

        private Font(CaxtonFont font, Int2LongMap entry) {
            this.font = font;
            this.entry = entry;
        }

        public long getOrCreateAtlasLocation(int glyphId) {
            return entry.computeIfAbsent(glyphId, id -> {
                long tlLoc = font.getTlistLocation(glyphId, 0);
                int width = (int) (tlLoc & 0xFFFF);
                int height = (int) ((tlLoc >> 16) & 0xFFFF);
                int numMipmapLevels = font.getNumMipmapLevels();
                long location = CaxtonGlyphCache.this.atlas.insert(width, height, 1, font.getOptions().range(), numMipmapLevels, font.getPixelFormat());

                atlas.getAtlasPageTexture(CaxtonAtlas.getPage(location)).bindTexture();
                GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, font.getPixelFormat().getChannelCount());

                for (int mipmap = 0; mipmap < numMipmapLevels; ++mipmap) {
                    tlLoc = font.getTlistLocation(glyphId, mipmap);
                    width = (int) (tlLoc & 0xFFFF);
                    height = (int) ((tlLoc >> 16) & 0xFFFF);
                    int offset = (int) (tlLoc >>> 32);
                    GlStateManager._texSubImage2D(
                            GlConst.GL_TEXTURE_2D,
                            mipmap,
                            CaxtonAtlas.getX(location) >> mipmap, CaxtonAtlas.getY(location) >> mipmap,
                            width, height,
                            font.getPixelFormat().toGl(),
                            GlConst.GL_UNSIGNED_BYTE,
                            font.getPixelData(mipmap) + 4 * Integer.toUnsignedLong(offset));
                }
                return location;
            });
        }
    }
}
