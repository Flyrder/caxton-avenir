package xyz.flirora.caxton.render;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.font.CaxtonFont;

import java.util.HashMap;
import java.util.Map;

public class CaxtonGlyphCache {
    private final CaxtonAtlas atlas;
    private final Map<CaxtonFont, Int2LongMap> cache = new HashMap<>();

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
        return atlas.getAtlasPageTexture(pageNum);
    }

    public void clear() {
        atlas.clear();
        cache.clear();
    }

    public class Font {
        private final CaxtonFont font;
        private final Int2LongMap entry;

        private Font(CaxtonFont font, Int2LongMap entry) {
            this.font = font;
            this.entry = entry;
        }

        public long getOrCreateAtlasLocation(int glyphId, long tlLoc, int fontRange) {
            return entry.computeIfAbsent(glyphId, id -> {
                int width = (int) (tlLoc & 0xFFFF);
                int height = (int) ((tlLoc >> 16) & 0xFFFF);
                int offset = (int) (tlLoc >>> 32);
                long location = CaxtonGlyphCache.this.atlas.insert(width, height, 1, fontRange);
                atlas.getAtlasPageTexture(CaxtonAtlas.getPage(location)).bindTexture();
                GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, 0);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, NativeImage.Format.RGBA.getChannelCount());
                GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, CaxtonAtlas.getX(location), CaxtonAtlas.getY(location), width, height, NativeImage.Format.RGBA.toGl(), GlConst.GL_UNSIGNED_BYTE, font.getPixelData() + 4 * Integer.toUnsignedLong(offset));
                return location;
            });
        }
    }
}
