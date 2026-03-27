// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.command.DebugShapeInfo;
import xyz.flirora.caxton.dll.Caxton1;

/**
 * Holds information related to a single font file.
 *
 * @see ConfiguredCaxtonFont
 * @see CaxtonTypeface
 */
@Environment(EnvType.CLIENT)
public class CaxtonFont implements AutoCloseable {
  private static final Arena ARENA = Arena.ofAuto();
  private static final Logger LOGGER = LogUtils.getLogger();
  private static String cacheDir = null;
  private final Identifier id;
  private final short[] metrics;
  private final int glyphCount;
  private final int tlistEntryCount;
  private final MemorySegment[] tlistLocations;
  private final MemorySegment bboxes;
  private final CaxtonFontOptions options;
  private final Int2ObjectMap<IntList> glyphsByWidth;
  private final MemorySegment[] pixelData;
  private final NativeImage.Format format;
  private MemorySegment fontData;
  private MemorySegment fontSegment;
  // CaxtonFont.{cloneReference, close} are called infrequently, so it wouldn’t
  // hurt much to protect them against race conditions.
  private final AtomicInteger refCount = new AtomicInteger(1);
  private final List<ChangeEntry> changes;

  /**
   * Constructs a new {@link CaxtonFont}.
   *
   * <p>The font has an initial reference count of 0; thus, it is recommended to call {@link
   * CaxtonFont#cloneReference()} on the returned object.
   *
   * @param input the {@link InputStream} containing the font file’s data in TTF or OTF format
   * @param id the {@link Identifier} under which this font should be known
   * @param options options to pass during font generation as a JSON object
   * @throws IOException if the underlying I/O operations fail
   */
  public CaxtonFont(InputStream input, Identifier id, JsonObject options) throws IOException {
    this.id = id;

    try (Arena arena = Arena.ofConfined()) {
      byte[] readInput = input.readAllBytes();

      fontData = ARENA.allocate(ValueLayout.JAVA_BYTE, readInput.length);
      MemorySegment.copy(readInput, 0, fontData, ValueLayout.JAVA_BYTE, 0, readInput.length);
      fontSegment = Caxton1.fontCreate(fontData, getCacheDir(), options.toString());
      MemorySegment metricsSegment = Caxton1.fontGetMetrics(fontSegment, arena);
      metrics = metricsSegment.toArray(ValueLayout.JAVA_SHORT);
      assert metrics.length == 9;
      glyphCount = Caxton1.fontNumGlyphs(fontSegment);
      tlistEntryCount = Caxton1.fontNumTlistLocations(fontSegment);
      bboxes = Caxton1.fontGetBoundingBoxes(fontSegment, glyphCount);

      int mipmapLayers = Caxton1.fontNumMipmapLayers(fontSegment);
      if (mipmapLayers <= 0) {
        throw new IllegalArgumentException("mipmapLayers must be at least 1");
      }
      if (mipmapLayers > 5) {
        throw new IllegalArgumentException("mipmapLayers must be at most 5");
      }
      tlistLocations = new MemorySegment[mipmapLayers];
      pixelData = new MemorySegment[mipmapLayers];
      for (int i = 0; i < mipmapLayers; ++i) {
        tlistLocations[i] = Caxton1.fontGetTlistPointer(fontSegment, i, tlistEntryCount);
        pixelData[i] = Caxton1.fontGetPixelData(fontSegment, i);
      }
      this.format =
          switch (Caxton1.fontNumBytesPerPixel(fontSegment)) {
            case 1 -> NativeImage.Format.LUMINANCE;
            case 2 -> NativeImage.Format.LUMINANCE_ALPHA;
            case 3 -> NativeImage.Format.RGB;
            case 4 -> NativeImage.Format.RGBA;
            default ->
                throw new UnsupportedOperationException(
                    "caxton_impl returned an unsupported value for bytes per pixel");
          };

      this.options = new CaxtonFontOptions(options);
      this.glyphsByWidth = new Int2ObjectOpenHashMap<>();
      for (int glyphId = 0; glyphId < glyphCount; ++glyphId) {
        long tlLoc =
            tlistLocations[0].getAtIndex(ValueLayout.JAVA_LONG, Integer.toUnsignedLong(glyphId));
        int width = (int) (tlLoc & 0xFFFF);
        this.glyphsByWidth.computeIfAbsent(width, w -> new IntArrayList()).add(glyphId);
      }
      this.changes =
          CaxtonModClient.CONFIG.debugRefcountChanges
              ? Collections.synchronizedList(new ArrayList<>())
              : null;
    } catch (Exception e) {
      try {
        //                this.cloneReference();
        this.close();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes the font.
   *
   * <p>This might not actually free the font data; more precisely, it decreases its reference
   * count. Once the reference count becomes zero, the underlying resources to the font are freed.
   *
   * @throws IllegalStateException if this method is called when the font has no remaining
   *     references
   */
  @Override
  public void close() {
    int remainingRefs = this.refCount.decrementAndGet();
    if (remainingRefs < 0) {
      LOGGER.error("ERROR: Font closed with a refcount of 0.");
      this.logRefcountChanges();
      throw new IllegalStateException("font closed with a refcount of 0");
    }
    if (remainingRefs == 0) {
      if (fontSegment != null) Caxton1.fontDestroy(fontSegment);
      fontSegment = null;
      fontData = null;
    }
    if (this.changes != null) {
      this.changes.add(ChangeEntry.capture(false));
    }
  }

  public boolean isClosed() {
    return this.fontData == null;
  }

  private void assertAvailable() {
    if (this.isClosed()) throw new IllegalStateException("Font already closed");
  }

  public String toString() {
    return "CaxtonFont[" + id + "@" + fontSegment + "]";
  }

  /**
   * Returns whether this font has a glyph for a given code point.
   *
   * @param codePoint a Unicode code point
   * @return {@code true} if this font supports this code point
   */
  public boolean supportsCodePoint(int codePoint) {
    this.assertAvailable();
    return Caxton1.fontCodePointToGlyphIndex(fontSegment, codePoint) != -1;
  }

  /**
   * Gets the identifier of this font.
   *
   * @return the {@link Identifier} of this font
   */
  public Identifier getId() {
    return id;
  }

  /**
   * Gets the value of metric from this font.
   *
   * @param i one of the values in {@link Metrics}
   * @return the value for the given metric
   * @see Metrics
   */
  public short getMetrics(int i) {
    return metrics[i];
  }

  /**
   * Gets the options associated with this font.
   *
   * @return the {@link CaxtonFontOptions} for this font
   */
  public CaxtonFontOptions getOptions() {
    return options;
  }

  /**
   * Gets the raw pointer to this font.
   *
   * @return the address of the pointer to the font in native memory
   * @deprecated Use {@link #getFontSegment()} instead
   */
  @Deprecated(forRemoval = true)
  public long getFontPtr() {
    this.assertAvailable();
    return fontSegment.address();
  }

  /**
   * Gets the {@link MemorySegment} to this font.
   *
   * @return the {@link MemorySegment} to the font in native memory
   */
  public MemorySegment getFontSegment() {
    this.assertAvailable();
    return fontSegment;
  }

  public int getNumMipmapLevels() {
    this.assertAvailable();
    return this.tlistLocations.length;
  }

  /**
   * Gets the location of glyph number {@code glyphId} in the glyph storage.
   *
   * <p>This is laid out as such, with bit 0 being the least significant:
   *
   * <ul>
   *   <li><b>Bits 0 – 15:</b> The width of this image, in pixels.
   *   <li><b>Bits 16 – 31:</b> The height of this image, in pixels.
   *   <li><b>Bits 32 63:</b> The offset of this image relative to {@link
   *       CaxtonFont#getPixelData(int)}.
   * </ul>
   *
   * <p>In addition, the glyph atlas leaves a margin of {@code getOptions().margin()} pixels on all
   * sides of the glyph itself. This margin is reflected in the returned value.
   *
   * @param glyphId the ID of the glyph to retrieve the atlas location for
   * @param mipmap the mipmap layer to retrieve the atlas location for
   * @return a packed atlas location value
   */
  public long getTlistLocation(int glyphId, int mipmap) {
    this.assertAvailable();
    if (glyphId < 0 || glyphId >= tlistEntryCount) {
      throw new IndexOutOfBoundsException(
          "i must be in [0, " + tlistEntryCount + ") (got " + glyphId + ")");
    }
    return tlistLocations[mipmap].getAtIndex(
        ValueLayout.JAVA_LONG, Integer.toUnsignedLong(glyphId));
  }

  /**
   * Gets the bounding box of glyph number {@param glyphId} as defined in the font file.
   *
   * <p>This is laid out as such, with bit 0 being the least significant:
   *
   * <ul>
   *   <li><b>Bits 0 – 15:</b> The minimum <var>x</var>-coordinate of the bounding box.
   *   <li><b>Bits 16 – 31:</b> The minimum <var>y</var>-coordinate of the bounding box.
   *   <li><b>Bits 32 – 47:</b> The maximum <var>x</var>-coordinate of the bounding box.
   *   <li><b>Bits 48 – 63:</b> The maximum <var>y</var>-coordinate of the bounding box.
   * </ul>
   *
   * <p>Unlike the coordinates returned by {@link CaxtonFont#getTlistLocation(int, int)}, the
   * coordinates here have the <var>y</var>-axis pointing up; that is, higher
   * <var>y</var>-coordinates correspond to points farther up.
   *
   * @param glyphId the ID of the glyph to retrieve the atlas location for
   * @return a packed bounding box for the glyph
   */
  public long getBbox(int glyphId) {
    this.assertAvailable();
    if (glyphId < 0 || glyphId >= glyphCount) {
      throw new IndexOutOfBoundsException(
          "i must be in [0, " + glyphCount + ") (got " + glyphId + ")");
    }
    return bboxes.getAtIndex(ValueLayout.JAVA_LONG, Integer.toUnsignedLong(glyphId));
  }

  /**
   * Gets the pointer to the pixel data.
   *
   * @param mipmap the mipmap layer to retrieve the pixel data for
   * @return the pointer to the pixel data.
   */
  public long getPixelData(int mipmap) {
    this.assertAvailable();
    return this.pixelData[mipmap].address();
  }

  public MemorySegment getPixelDataSegment(int mipmap) {
    this.assertAvailable();
    return this.pixelData[mipmap];
  }

  public NativeImage.Format getPixelFormat() {
    return format;
  }

  /**
   * Returns a map from width values to glyph IDs.
   *
   * <p>In particular, the atlas bounding box width from {@link CaxtonFont#getTlistLocation(int,
   * int)} (with {@code mipmap} = 0) is used, not the value from {@link CaxtonFont#getBbox(int)}.
   *
   * @return a map from width values to glyph IDs
   */
  public Int2ObjectMap<IntList> getGlyphsByWidth() {
    return glyphsByWidth;
  }

  public @Nullable String getGlyphName(int id) {
    this.assertAvailable();
    return Caxton1.fontGetGlyphName(this.fontSegment, id);
  }

  /**
   * Returns the number of logical glyphs in this font.
   *
   * <p>Note that this is not necessarily equal to the number of texture list locations. For
   * instance, a font might store a texture for each glyph twice: once for the glyph itself and
   * another for its outline. (For instance, the bitmap font implementation stores glyphs this way
   * rather than assigning separate channels for interiors and outlines in order to be compatible
   * with the built-in core shaders.)
   *
   * @return the number of logical glyphs in this font
   */
  public int getGlyphCount() {
    return glyphCount;
  }

  public int getTlistSize() {
    return tlistEntryCount;
  }

  public DebugShapeInfo shapeForDebug(String s) {
    this.assertAvailable();
    return Caxton1.fontShapeForDebug(this.fontSegment, s);
  }

  private static String getCacheDir() {
    if (cacheDir == null) {
      var dir = new File(MinecraftClient.getInstance().runDirectory, "caxton_cache");
      try {
        Files.createDirectories(dir.toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      cacheDir = dir.getAbsolutePath();
    }
    return cacheDir;
  }

  /**
   * Increments the reference count of this font.
   *
   * @return the receiver
   */
  public CaxtonFont cloneReference() {
    this.assertAvailable();
    if (this.changes != null) {
      this.changes.add(ChangeEntry.capture(true));
    }
    this.refCount.incrementAndGet();
    return this;
  }

  private void logRefcountChanges() {
    if (this.changes != null) {
      LOGGER.error("Refcount changes:");
      for (var change : this.changes) {
        change.log(LOGGER);
      }
    }
  }

  private record ChangeEntry(Thread thread, StackTraceElement[] stackTrace, boolean added) {
    private static ChangeEntry capture(boolean added) {
      Thread currentThread = Thread.currentThread();
      return new ChangeEntry(currentThread, currentThread.getStackTrace(), added);
    }

    private void log(Logger logger) {
      logger.error(
          this.added ? "Reference added in thread {}:" : "Reference removed in thread {}:",
          this.thread);
      for (var elem : this.stackTrace) {
        logger.error(elem.toString());
      }
    }
  }

  /**
   * Constants for metric types.
   *
   * @see CaxtonFont#getMetrics(int)
   */
  public static class Metrics {
    public static int UNITS_PER_EM = 0;
    public static int ASCENDER = 1;
    public static int DESCENDER = 2;
    public static int HEIGHT = 3;
    public static int LINE_GAP = 4;
    public static int UNDERLINE_POSITION = 5;
    public static int UNDERLINE_THICKNESS = 6;
    public static int STRIKEOUT_POSITION = 7;
    public static int STRIKEOUT_THICKNESS = 8;
  }
}
