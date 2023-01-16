package xyz.flirora.caxton.render;

import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An atlas for textures.
 * <p>
 * This is based on the caxton_impl implementation in Rust but is adapted to online settings where the list of glyphs is not fully known in advance. Instead of storing the list of free spaces in an array list, it uses a map sorted by the space’s height.
 */
public class CaxtonAtlas implements AutoCloseable {
    public static int PAGE_SIZE = 4096;
    private final List<NativeImageBackedTexture> pages = new ArrayList<>();
    private final LongSortedSet spaces = new LongRBTreeSet(Comparator.comparingInt(CaxtonAtlas::getH));
    private final List<Identifier> ids = new ArrayList<>();
    private final TextureManager textureManager;

    public CaxtonAtlas(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public static int getX(long packed) {
        return (int) (packed & 0x1FFF);
    }

    public static int getY(long packed) {
        return (int) ((packed >> 13) & 0x1FFF);
    }

    public static int getW(long packed) {
        return (int) ((packed >> 26) & 0x1FFF);
    }

    public static int getH(long packed) {
        return (int) ((packed >> 39) & 0x1FFF);
    }

    public static int getPage(long packed) {
        return (int) (packed >>> 52);
    }

    private static long pack(int x, int y, int w, int h, int page) {
        return ((long) x) | (((long) y) << 13) | (((long) w) << 26) | (((long) h) << 39) | (((long) page) << 52);
    }

    public Identifier getAtlasPage(int pageNum) {
        return this.ids.get(pageNum);
    }

    public NativeImageBackedTexture getAtlasPageTexture(int pageNum) {
        return this.pages.get(pageNum);
    }

    public long insert(int width, int height, int additionalMargin) {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative");
        }
        int totalWidth = width + additionalMargin;
        int totalHeight = height + additionalMargin;
        if (totalWidth > PAGE_SIZE) {
            throw new IllegalArgumentException("width + additionalMargin must not be greater than 4096");
        }
        if (totalHeight > PAGE_SIZE) {
            throw new IllegalArgumentException("height + additionalMargin must not be greater than 4096");
        }
        if (width == 0 || height == 0) {
            return pack(0, 0, width, height, 0);
        }
        long space;
        while (true) {
            space = this.tryInsert(totalWidth, totalHeight);
            if (space != -1) break;
            this.addNewPage();
        }
        return space - ((long) additionalMargin) * ((1L << 26) | (1L << 39));
    }

    private long tryInsert(int width, int height) {
        long space = chooseSpace(width, height);
        if (space == -1) return -1;
        return deductPortion(space, width, height);
    }

    private long chooseSpace(int width, int height) {
        var it = spaces.iterator(pack(0, 0, 0, Math.max(height - 1, 0), 0));
        while (it.hasNext()) {
            long space = it.nextLong();
            if (getW(space) >= width) {
                it.remove();
                return space;
            }
        }
        return -1;
    }

    private long deductPortion(long space, int width, int height) {
        int spaceX = getX(space), spaceY = getY(space);
        int spaceW = getW(space), spaceH = getH(space);
        int spacePage = getPage(space);
        int excessW = spaceW - width, excessH = spaceH - height;
        if (excessW == 0) {
            if (excessH != 0) {
                addSpace(spaceX, spaceY + height, spaceW, excessH, spacePage);
            }
        } else if (excessH == 0) {
            addSpace(spaceX + width, spaceY, excessW, spaceH, spacePage);
        } else if (excessW > excessH) {
            addSpace(spaceX + width, spaceY, excessW, spaceH, spacePage);
            addSpace(spaceX, spaceY + height, width, excessH, spacePage);
        } else {
            addSpace(spaceX, spaceY + height, spaceW, excessH, spacePage);
            addSpace(spaceX + width, spaceY, excessW, height, spacePage);
        }
        return pack(spaceX, spaceY, width, height, spacePage);
    }

    private void addSpace(int x, int y, int width, int height, int page) {
        spaces.add(pack(x, y, width, height, page));
    }

    private void addNewPage() {
        int pageNum = pages.size();
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, PAGE_SIZE, PAGE_SIZE, false);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        Identifier id = new Identifier("caxton", "atlas/" + pageNum);
        this.textureManager.registerTexture(id, texture);
        this.pages.add(texture);
        this.ids.add(id);
        this.addSpace(0, 0, PAGE_SIZE, PAGE_SIZE, pageNum);
    }

    @Override
    public void close() {
        for (NativeImageBackedTexture texture : this.pages) {
            texture.close();
        }
        this.pages.clear();
    }

    public void clear() {
        this.close();
        pages.clear();
        spaces.clear();
        ids.clear();
    }
}
