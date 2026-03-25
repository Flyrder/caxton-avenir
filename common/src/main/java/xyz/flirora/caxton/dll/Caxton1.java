// (C) 2026 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.dll;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.PathElement.sequenceElement;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import xyz.flirora.caxton.command.DebugShapeInfo;

/// Handles calls to the caxton-impl native library through the Foreign Function & Memory API.
public class Caxton1 {
  private static final Linker LINKER = Linker.nativeLinker();
  // TODO: replace with libraryLookup (might need to make Caxton1 a singleton class?)
  private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();
  public static final AddressLayout FONTPTR = ValueLayout.ADDRESS.withName("Font *");
  public static final AddressLayout CFONTPTR = ValueLayout.ADDRESS.withName("ConfiguredFont *");
  public static final String VERSION = getVersion();

  private static final MethodHandle STATUSMSG_DESTROY =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_statusmsg_destroy"),
          FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

  private static void statusmsgDestroy(MemorySegment message) {
    try {
      STATUSMSG_DESTROY.invokeExact(message);
    } catch (Throwable e) {
      þrö(e);
    }
  }

  private static final MethodHandle FONT_CREATE =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_create"),
          FunctionDescriptor.of(
              Status.FONT.layout(),
              ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_BYTE),
              ValueLayout.JAVA_LONG,
              ValueLayout.ADDRESS,
              ValueLayout.ADDRESS));

  public static MemorySegment fontCreate(MemorySegment fontData, String cachePath, String options) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment cachePathSegment = arena.allocateFrom(cachePath);
      MemorySegment optionsSegment = arena.allocateFrom(options);
      try {
        MemorySegment result =
            (MemorySegment)
                FONT_CREATE.invokeExact(
                    fontData, fontData.byteSize(), cachePathSegment, optionsSegment);
        return (MemorySegment) Status.FONT.handleError(result);
      } catch (Throwable e) {
        þrö(e);
        return null;
      }
    }
  }

  private static final MethodHandle FONT_DESTROY =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_destroy"), FunctionDescriptor.ofVoid(FONTPTR));

  public static void fontDestroy(MemorySegment font) {
    try {
      FONT_DESTROY.invokeExact(font);
    } catch (Throwable e) {
      þrö(e);
    }
  }

  private static final MethodHandle FONT_CODEPOINT_TO_GLYPH_INDEX =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_codepoint_to_glyph_index"),
          FunctionDescriptor.of(ValueLayout.JAVA_INT, FONTPTR, ValueLayout.JAVA_INT));

  public static int fontCodePointToGlyphIndex(MemorySegment font, int codePoint) {
    try {
      return (int) FONT_CODEPOINT_TO_GLYPH_INDEX.invokeExact(font, codePoint);
    } catch (Throwable e) {
      þrö(e);
      return -1;
    }
  }

  private static final MethodHandle FONT_GET_METRICS =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_get_metrics"),
          FunctionDescriptor.of(Metrics.LAYOUT, FONTPTR));

  public static MemorySegment fontGetMetrics(MemorySegment font, SegmentAllocator allocator) {
    try {
      return (MemorySegment) FONT_GET_METRICS.invokeExact(font, allocator);
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  private static final FunctionDescriptor FONT_TO_I32_DESCRIPTOR =
      FunctionDescriptor.of(ValueLayout.JAVA_INT, FONTPTR);

  private static MethodHandle fontToI32Func(String name) {
    return LINKER.downcallHandle(LOOKUP.findOrThrow(name), FONT_TO_I32_DESCRIPTOR);
  }

  private static final MethodHandle FONT_NUM_MIPMAP_LAYERS =
      fontToI32Func("caxton1_font_num_mipmap_layers");
  private static final MethodHandle FONT_NUM_GLYPHS = fontToI32Func("caxton1_font_num_glyphs");
  private static final MethodHandle FONT_NUM_TLIST_LOCATIONS =
      fontToI32Func("caxton1_font_num_tlist_locations");
  private static final MethodHandle FONT_NUM_BYTES_PER_PIXEL =
      fontToI32Func("caxton1_font_num_bytes_per_pixel");

  public static int fontNumMipmapLayers(MemorySegment font) {
    try {
      return (int) FONT_NUM_MIPMAP_LAYERS.invokeExact(font);
    } catch (Throwable e) {
      þrö(e);
      return -1;
    }
  }

  public static int fontNumGlyphs(MemorySegment font) {
    try {
      return (int) FONT_NUM_GLYPHS.invokeExact(font);
    } catch (Throwable e) {
      þrö(e);
      return -1;
    }
  }

  public static int fontNumTlistLocations(MemorySegment font) {
    try {
      return (int) FONT_NUM_TLIST_LOCATIONS.invokeExact(font);
    } catch (Throwable e) {
      þrö(e);
      return -1;
    }
  }

  public static int fontNumBytesPerPixel(MemorySegment font) {
    try {
      return (int) FONT_NUM_BYTES_PER_PIXEL.invokeExact(font);
    } catch (Throwable e) {
      þrö(e);
      return -1;
    }
  }

  private static final MethodHandle FONT_GET_BOUNDING_BOXES =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_get_bounding_boxes"),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_LONG), FONTPTR));

  public static MemorySegment fontGetBoundingBoxes(MemorySegment font, long glyphCount) {
    try {
      MemorySegment segment = (MemorySegment) FONT_GET_BOUNDING_BOXES.invokeExact(font);
      return segment.reinterpret(glyphCount * ValueLayout.JAVA_LONG.byteSize());
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  private static final ValueLayout TLOCATION = ValueLayout.JAVA_LONG;
  private static final MethodHandle FONT_GET_TLIST_POINTER =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_get_tlist_pointer"),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS.withTargetLayout(TLOCATION), FONTPTR, ValueLayout.JAVA_INT));

  public static MemorySegment fontGetTlistPointer(
      MemorySegment font, int mipmap, long tlistLocationCount) {
    try {
      MemorySegment segment = (MemorySegment) FONT_GET_TLIST_POINTER.invokeExact(font, mipmap);
      return segment.reinterpret(tlistLocationCount * TLOCATION.byteSize());
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  private static final MethodHandle FONT_GET_PIXEL_DATA =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_get_pixel_data"),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_BYTE),
              FONTPTR,
              ValueLayout.JAVA_INT,
              ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_LONG)));

  public static MemorySegment fontGetPixelData(MemorySegment font, int mipmap) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment sizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      MemorySegment segment =
          (MemorySegment) FONT_GET_PIXEL_DATA.invokeExact(font, mipmap, sizeSegment);
      return segment.reinterpret(
          sizeSegment.getAtIndex(ValueLayout.JAVA_LONG, 0) * TLOCATION.byteSize());
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  private static final MethodHandle FONT_GET_GLYPH_NAME =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_get_glyph_name"),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_BYTE),
              FONTPTR,
              ValueLayout.JAVA_INT,
              ValueLayout.ADDRESS.withTargetLayout(ValueLayout.JAVA_LONG)));

  public static @Nullable String fontGetGlyphName(MemorySegment font, int glyphId) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment sizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      MemorySegment segment =
          (MemorySegment) FONT_GET_GLYPH_NAME.invokeExact(font, glyphId, sizeSegment);
      if (segment.address() == 0) return null;
      segment =
          segment.reinterpret(
              sizeSegment.getAtIndex(ValueLayout.JAVA_LONG, 0) * ValueLayout.JAVA_BYTE.byteSize());
      byte[] bytes = segment.toArray(ValueLayout.JAVA_BYTE);
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  private static final MethodHandle FONT_SHAPE_FOR_DEBUG =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_shape_for_debug"),
          FunctionDescriptor.of(DebugShapeInfo1.LAYOUT, FONTPTR, ValueLayout.ADDRESS));

  public static DebugShapeInfo fontShapeForDebug(MemorySegment font, String text) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment textSegment = arena.allocateFrom(text);
      MemorySegment result =
          (MemorySegment) FONT_SHAPE_FOR_DEBUG.invokeExact(font, textSegment, arena);
      DebugShapeInfo info = DebugShapeInfo1.fromNative(result);
      DebugShapeInfo1.destroy(result);
      return info;
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  public static final MethodHandle FONT_CONFIGURE =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_configure"),
          FunctionDescriptor.of(Status.CFONT.layout(), FONTPTR, ValueLayout.ADDRESS));

  public static MemorySegment fontConfigure(MemorySegment font, String settings) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment settingsSegment = arena.allocateFrom(settings);
      MemorySegment result = (MemorySegment) FONT_CONFIGURE.invokeExact(font, settingsSegment);
      return (MemorySegment) Status.CFONT.handleError(result);
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  public static final MethodHandle CFONT_DESTROY =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_cfont_destroy"), FunctionDescriptor.ofVoid(CFONTPTR));

  public static void cfontDestroy(MemorySegment cfont) {
    try {
      CFONT_DESTROY.invokeExact(cfont);
    } catch (Throwable e) {
      þrö(e);
    }
  }

  public static final MethodHandle CFONT_SHAPE =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_cfont_shape"),
          FunctionDescriptor.of(
              ShapingResult1.LAYOUT,
              CFONTPTR,
              ValueLayout.ADDRESS,
              ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_BOOLEAN,
              ValueLayout.JAVA_INT));

  public static MemorySegment cfontShape(
      MemoryLayout cfont, MemorySegment s, int start, int end, boolean rtl, int script) {
    try (Arena arena = Arena.ofConfined()) {
      return (MemorySegment)
          CFONT_SHAPE.invokeExact(
              cfont, s, s.byteSize() / ValueLayout.JAVA_CHAR.byteSize(), start, end, rtl, script);
    } catch (Throwable e) {
      þrö(e);
      return null;
    }
  }

  public static final MethodHandle SHAPINGRESULT_DESTROY =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_shapingresult_destroy"),
          FunctionDescriptor.ofVoid(ValueLayout.ADDRESS.withTargetLayout(ShapingResult1.LAYOUT)));

  public static void shapingresultDestroy(MemorySegment shapingResult) {
    try {
      SHAPINGRESULT_DESTROY.invokeExact(shapingResult);
    } catch (Throwable e) {
      þrö(e);
    }
  }

  /// A wrapper for caxton-impl’s `Caxton1Status<T>` type, parameterized on the value type `T`.
  ///
  /// @param layout  The memory layout for this type.
  /// @param tag     The [VarHandle] describing the tag field.
  /// @param value   The [VarHandle] describing the value field of the `Ok` variant.
  /// @param cls     The [VarHandle] describing the `cls` field of the `Err` variant.
  /// @param message The [VarHandle] describing the `message` field of the `Err` variant.
  public record Status(
      StructLayout layout, VarHandle tag, VarHandle value, VarHandle cls, VarHandle message) {
    public static final ValueLayout.OfInt TAG_LAYOUT = ValueLayout.JAVA_INT.withName("tag");
    public static final int OK = 0;
    public static final int ERR = 1;
    public static final StructLayout ERR_LAYOUT =
        MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("cls"), ValueLayout.ADDRESS.withName("message"))
            .withName("err");
    public static final Status FONT = of(FONTPTR);
    public static final Status CFONT = of(CFONTPTR);

    /// Constructs a new instance of [Status] describing `Caxton1Status<T>`.
    ///
    /// [#handleError] will return an instance of the carrier class of `layoutT`.
    ///
    /// @param layoutT the layout of the enclosed type `T`
    /// @return a new [Status] object describing the resulting type
    public static Status of(MemoryLayout layoutT) {
      UnionLayout unionLayout =
          MemoryLayout.unionLayout(
              MemoryLayout.structLayout(layoutT.withName("value")).withName("ok"), ERR_LAYOUT);
      StructLayout layout =
          MemoryLayout.structLayout(
              TAG_LAYOUT,
              MemoryLayout.paddingLayout(unionLayout.byteAlignment() - TAG_LAYOUT.byteSize()),
              unionLayout.withName("payload"));
      VarHandle tag = layout.varHandle(groupElement("tag"));
      VarHandle value =
          layout.varHandle(groupElement("payload"), groupElement("ok"), groupElement("value"));
      VarHandle cls =
          layout.varHandle(groupElement("payload"), groupElement("err"), groupElement("cls"));
      VarHandle message =
          layout.varHandle(groupElement("payload"), groupElement("err"), groupElement("message"));
      return new Status(layout, tag, value, cls, message);
    }

    /// Returns the `value` field of a `Caxton1Status`, or throws
    /// if there is an error.
    ///
    /// @param resultSegment the [MemorySegment] corresponding to the result
    /// @return an object of the carrier class of the [MemoryLayout] used when constructing this
    // status using [Status#of]
    public Object handleError(MemorySegment resultSegment) {
      int tag = (int) this.tag.get(resultSegment, 0);
      if (tag == OK) {
        return this.value.get(resultSegment, 0);
      } else {
        MemorySegment clsSegment = (MemorySegment) this.cls.get(resultSegment, 0);
        MemorySegment messageSegment = (MemorySegment) this.message.get(resultSegment, 0);
        String clsName = fromCStr(clsSegment);
        String message = fromCStr(messageSegment);
        statusmsgDestroy(messageSegment);
        try {
          throw (Exception)
              Class.forName(clsName).getConstructor(String.class).newInstance(message);
        } catch (Exception e) {
          þrö(e);
          return null;
        }
      }
    }
  }

  public static class Metrics {
    public static final MemoryLayout LAYOUT =
        MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(9, ValueLayout.JAVA_SHORT).withName("values"));
    public static final VarHandle VALUES =
        LAYOUT.varHandle(groupElement("values"), sequenceElement());

    public static short get(MemorySegment metrics, int index) {
      return (short) VALUES.get(metrics, 0, index);
    }
  }

  public static class DebugShapeInfo1 {
    public static final MemoryLayout LAYOUT =
        MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("inferred_language"),
            ValueLayout.ADDRESS.withName("serialized_results"),
            ValueLayout.JAVA_INT.withName("inferred_script"),
            ValueLayout.JAVA_BYTE.withName("inferred_direction"),
            MemoryLayout.paddingLayout(3));
    public static final VarHandle INFERRED_LANGUAGE =
        LAYOUT.varHandle(groupElement("inferred_language"));
    public static final VarHandle SERIALIZED_RESULTS =
        LAYOUT.varHandle(groupElement("serialized_results"));
    public static final VarHandle INFERRED_SCRIPT =
        LAYOUT.varHandle(groupElement("inferred_script"));
    public static final VarHandle INFERRED_DIRECTION =
        LAYOUT.varHandle(groupElement("inferred_direction"));

    public static DebugShapeInfo fromNative(MemorySegment segment) {
      String inferredLanguage = fromCStr((MemorySegment) INFERRED_LANGUAGE.get(segment, 0));
      String serializedResult = fromCStr((MemorySegment) SERIALIZED_RESULTS.get(segment, 0));
      int inferredScript = (int) INFERRED_SCRIPT.get(segment, 0);
      byte direction = (byte) INFERRED_DIRECTION.get(segment, 0);

      String inferredScriptStr =
          new String(
              new byte[] {
                (byte) inferredScript,
                (byte) (inferredScript >>> 8),
                (byte) (inferredScript >>> 16),
                (byte) (inferredScript >>> 24),
              },
              StandardCharsets.UTF_8);
      String inferredDirectionStr = DebugShapeInfo.Direction.VALUES[direction].asString();

      return new DebugShapeInfo(
          serializedResult, inferredLanguage, inferredScriptStr, inferredDirectionStr);
    }

    public static void destroy(MemorySegment segment) {
      MemorySegment inferredLanguage = (MemorySegment) INFERRED_LANGUAGE.get(segment, 0);
      MemorySegment serializedResult = (MemorySegment) SERIALIZED_RESULTS.get(segment, 0);
      if (inferredLanguage.address() != 0) {
        statusmsgDestroy(inferredLanguage);
      }
      if (serializedResult.address() != 0) {
        statusmsgDestroy(serializedResult);
      }
    }
  }

  public static class ShapingResult1 {
    public static final MemoryLayout LAYOUT =
        MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("data_count"),
            ValueLayout.ADDRESS.withName("data"),
            ValueLayout.JAVA_INT.withName("total_width"),
            MemoryLayout.paddingLayout(
                ValueLayout.ADDRESS.byteAlignment() - ValueLayout.JAVA_INT.byteSize()));
    public static final VarHandle DATA_COUNT = LAYOUT.varHandle(groupElement("data_count"));
    public static final VarHandle DATA = LAYOUT.varHandle(groupElement("data"));
    public static final VarHandle TOTAL_WIDTH = LAYOUT.varHandle(groupElement("total_width"));

    public static final MemoryLayout ENTRY_LAYOUT =
        MemoryLayout.sequenceLayout(6, ValueLayout.JAVA_LONG);
    public static final VarHandle ENTRY_GLYPH_ID_AND_FLAGS = dataElement(0);
    public static final VarHandle ENTRY_CLUSTER_INDEX = dataElement(1);
    public static final VarHandle ENTRY_ADVANCE_X = dataElement(2);
    public static final VarHandle ENTRY_ADVANCE_Y = dataElement(3);
    public static final VarHandle ENTRY_OFFSET_X = dataElement(4);
    public static final VarHandle ENTRY_OFFSET_Y = dataElement(5);

    private static VarHandle dataElement(int index) {
      return ENTRY_LAYOUT.arrayElementVarHandle(sequenceElement(index));
    }
  }

  private static String getVersion() {
    System.err.println(LINKER.canonicalLayouts());
    MemorySegment ptrSegment =
        LOOKUP.findOrThrow("caxton1_version").reinterpret(ValueLayout.ADDRESS.byteSize());
    return fromCStr(ptrSegment.get(ValueLayout.ADDRESS, 0));
  }

  @SuppressWarnings("NonAsciiCharacters") // Go StreamCodec yourself
  private static void þrö(Throwable e) {
    if (e instanceof RuntimeException re) throw re;
    if (e instanceof Error re) throw re;
    else throw new RuntimeException(e);
  }

  private static String fromCStr(MemorySegment cStr) {
    if (cStr.address() == 0) return null;
    return cStr.reinterpret(Long.MAX_VALUE).getString(0);
  }
}
