// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * An atlas for textures.
 * <p>
 * This is based on the caxton_impl implementation in Rust but is adapted to online settings where the list of glyphs is not fully known in advance. Instead of storing the list of free spaces in an array list, it uses a map sorted by the space’s height.
 */
public class CaxtonAtlas implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static int PAGE_SIZE = 4096;
    private final List<Page> pages = new ArrayList<>();
    // w is interpreted as number of mipmap levels; h is interpreted as range
    private final List<LongList> shelves = new ArrayList<>();
    private final IntList unallocatedSpaces = new IntArrayList();
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

    private static int heightToBucketId(int height) {
        assert height >= 1;
        return ((height + 7) / 8) - 1;
    }

    private static int bucketIdToHeight(int bucket) {
        return 8 * (bucket + 1);
    }

    private int roundToNextPowerOf2(int n, int p) {
        return (n + ((1 << p) - 1)) >> p << p;
    }

    public Identifier getAtlasPage(int pageNum) {
        return this.pages.get(pageNum).id;
    }

    public Page getAtlasPageTexture(int pageNum) {
        return this.pages.get(pageNum);
    }

    public int getNumPages() {
        return this.pages.size();
    }

    public List<Page> getAllPages() {
        return pages;
    }

    public long insert(int width, int height, int additionalMargin, int fontRange, int numMipmapLevels, NativeImage.Format format) {
        if (fontRange < 0 || fontRange >= 256) {
            throw new IllegalArgumentException("fontRange must be in [0, 256)");
        }
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative");
        }
        int totalWidth = roundToNextPowerOf2(width + additionalMargin, numMipmapLevels - 1);
        int totalHeight = roundToNextPowerOf2(height + additionalMargin, numMipmapLevels - 1);
        if (totalWidth > PAGE_SIZE) {
            throw new IllegalArgumentException("width + additionalMargin must not be greater than 4096");
        }
        if (totalHeight > PAGE_SIZE) {
            throw new IllegalArgumentException("height + additionalMargin must not be greater than 4096");
        }
        if (width == 0 || height == 0) {
            return pack(0, 0, width, height, 0);
        }
        long space = this.insertInternal(totalWidth, totalHeight, fontRange, numMipmapLevels, format);
        return pack(
                getX(space),
                getY(space),
                width,
                height,
                getPage(space)
        );
    }

    private long insertInternal(int width, int height, int range, int numMipmapLevels, NativeImage.Format format) {
        int bucket = heightToBucketId(height);
        while (bucket >= shelves.size()) {
            shelves.add(new LongArrayList());
        }
        LongList shelvesInBucket = shelves.get(bucket);
        for (int i = shelvesInBucket.size() - 1; i >= 0; --i) {
            long shelf = shelvesInBucket.getLong(i);
            int x = getX(shelf), y = getY(shelf);
            int page = getPage(shelf);
            if (x + width > PAGE_SIZE) continue; // Too narrow
            if (getW(shelf) != numMipmapLevels) continue; // Wrong # mipmap levels
            if (getH(shelf) != range) continue; // Wrong range
            if (getAtlasPageTexture(page).format != format) continue; // Wrong format
            long space = pack(x, y, width, height, page);
            long remainingShelf = pack(x + width, y, numMipmapLevels, range, page);
            shelvesInBucket.set(i, remainingShelf);
            if (i > 0) {
                long prev = shelvesInBucket.getLong(i - 1);
                if (x + width > getW(prev)) {
                    shelvesInBucket.set(i, prev);
                    shelvesInBucket.set(i - 1, remainingShelf);
                }
            }
            return space;
        }
        {
            long shelf = allocateShelf(bucket, range, numMipmapLevels, format);
            int x = getX(shelf), y = getY(shelf);
            int page = getPage(shelf);
            long remainingShelf = pack(width, y, numMipmapLevels, range, page);
            shelvesInBucket.add(remainingShelf);
            return pack(x, y, width, height, page);
        }
    }

    private long allocateShelf(int bucket, int range, int numMipmapLevels, NativeImage.Format format) {
        int height = bucketIdToHeight(bucket);
        int minHeight = bucketIdToHeight(0);
        while (true) {
            int len = unallocatedSpaces.size();
            for (int i = 0; i < len; ++i) {
                int space = unallocatedSpaces.getInt(i);
                int y = (space & 0xFFF);
                int page = (space >> 12) & 0xFFF;
                Page pageTex = this.getAtlasPageTexture(page);
                if (pageTex.range != range || pageTex.numMipmapLevels != numMipmapLevels || pageTex.format != format)
                    continue;
                int y2 = y + height;
                if (y2 > PAGE_SIZE) continue;
                if (y2 + minHeight <= PAGE_SIZE) {
                    unallocatedSpaces.set(i, y2 | (page << 12));
                } else {
                    unallocatedSpaces.set(i, unallocatedSpaces.getInt(len - 1));
                    unallocatedSpaces.removeInt(len - 1);
                }
                return pack(0, y, numMipmapLevels, range, page);
            }
            addNewPage(range, numMipmapLevels, format);
        }
    }

    private void addNewPage(int range, int numMipmapLevels, NativeImage.Format format) {
        int pageNum = pages.size();
        if (pageNum >= 4096) {
            throw new RuntimeException("Only 4096 pages are supported");
        }
        LOGGER.info("I am {}", this);
        LOGGER.info("Adding a new page #{} with range {} and {} mipmap levels", pageNum, range, numMipmapLevels);
        Page page = new Page(this, pageNum, range, numMipmapLevels, format);
        this.pages.add(page);
        this.unallocatedSpaces.add(pageNum << 12);
    }

    @Override
    public void close() {
        for (Page page : this.pages) {
            page.close();
        }
        this.pages.clear();
        this.unallocatedSpaces.clear();
    }

    public void clear() {
        for (Page page : this.pages) {
            page.close();
        }
        this.pages.clear();
        shelves.clear();
        unallocatedSpaces.clear();
    }

    public static class Page extends AbstractTexture {
        private final Identifier id;
        private final int range;
        private final int numMipmapLevels;
        private final NativeImage.Format format;

        public Page(CaxtonAtlas atlas, int pageNum, int range, int numMipmapLevels, NativeImage.Format format) {
            super();
            this.format = format;
            NativeImage.InternalFormat internalFormat = switch (format) {
                case RGBA -> NativeImage.InternalFormat.RGBA;
                case RGB -> NativeImage.InternalFormat.RGB;
                case LUMINANCE_ALPHA -> NativeImage.InternalFormat.RG;
                case LUMINANCE -> NativeImage.InternalFormat.RED;
            };
            TextureUtil.prepareImage(internalFormat, this.getGlId(), numMipmapLevels - 1, PAGE_SIZE, PAGE_SIZE);
            // Technically, the contents of this texture are undefined at this
            // point, which can pose a problem with the edges of any inserted
            // textures. Hopefully this doesn’t blow up on us at some point!
            // this.clearContents();
            this.id = new Identifier("caxton", "atlas/" + pageNum);
            this.range = range;
            this.numMipmapLevels = numMipmapLevels;
            atlas.textureManager.registerTexture(id, this);
        }

        public Identifier getId() {
            return id;
        }

        public int getRange() {
            return range;
        }

        public int getNumMipmapLevels() {
            return numMipmapLevels;
        }

        public NativeImage.Format getFormat() {
            return format;
        }

        @Override
        public void load(ResourceManager manager) {
        }

        @Override
        public void close() {
            this.clearGlId();
        }
    }
}
