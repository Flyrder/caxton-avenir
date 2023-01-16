package xyz.flirora.caxton.font;

import it.unimi.dsi.fastutil.ints.Int2LongRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongSortedMap;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.util.ArrayList;
import java.util.List;

/**
 * An atlas for textures.
 * <p>
 * This is based on the caxton_impl implementation in Rust but is adapted to online settings where the list of glyphs is not fully known in advance. Instead of storing the list of free spaces in an array list, it uses a map sorted by the space’s height.
 */
public class CaxtonAtlas implements AutoCloseable {
    public static int PAGE_SIZE = 4096;
    private final List<NativeImageBackedTexture> pages = new ArrayList<>();
    private final Int2LongSortedMap spaces = new Int2LongRBTreeMap();

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
        var tallEnough = spaces.tailMap(height).int2LongEntrySet();
        var it = tallEnough.stream().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            long space = entry.getLongValue();
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
        spaces.put(height, pack(x, y, width, height, page));
    }

    private void addNewPage() {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, PAGE_SIZE, PAGE_SIZE, false);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        this.pages.add(texture);
    }

    @Override
    public void close() {
        for (NativeImageBackedTexture texture : this.pages) {
            texture.close();
        }
        this.pages.clear();
    }
}
