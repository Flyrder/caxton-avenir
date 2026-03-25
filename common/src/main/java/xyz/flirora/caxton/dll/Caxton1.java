// (C) 2026 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.dll;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/// Handles calls to the caxton-impl native library through the Foreign Function & Memory API.
public class Caxton1 {
  private static final Linker LINKER = Linker.nativeLinker();
  // TODO: replace with libraryLookup (might need to make Caxton1 a singleton class?)
  private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();
  public static final AddressLayout FONTPTR = ValueLayout.ADDRESS.withName("Font *");
  public static final ValueLayout UINTPTR_T =
      (ValueLayout) Objects.requireNonNull(LINKER.canonicalLayouts().get("uintptr_t"));
  public static final String VERSION = getVersion();

  private static final MethodHandle STATUSMSG_DESTROY =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_statusmsg_destroy"),
          FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

  private static void statusmsgDestroy(MemorySegment message) {
    try {
      STATUSMSG_DESTROY.invoke(message);
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
              UINTPTR_T,
              ValueLayout.ADDRESS,
              ValueLayout.ADDRESS));

  public static MemorySegment fontCreate(MemorySegment fontData, String cachePath, String options) {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment cachePathSegment = arena.allocateFrom(cachePath);
      MemorySegment optionsSegment = arena.allocateFrom(options);
      try {
        MemorySegment result =
            (MemorySegment)
                FONT_CREATE.invoke(fontData, fontData.byteSize(), cachePathSegment, optionsSegment);
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
      FONT_DESTROY.invoke(font);
    } catch (Throwable e) {
      þrö(e);
    }
  }

  private static final MethodHandle CODEPOINT_TO_GLYPH_INDEX =
      LINKER.downcallHandle(
          LOOKUP.findOrThrow("caxton1_font_codepoint_to_glyph_index"),
          FunctionDescriptor.of(ValueLayout.JAVA_INT, FONTPTR, ValueLayout.JAVA_INT));

  public static int fontCodePointToGlyphIndex(MemorySegment font, int codePoint) {
    try {
      return (int) CODEPOINT_TO_GLYPH_INDEX.invoke(font, codePoint);
    } catch (Throwable e) {
      þrö(e);
      return -1;
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
      StructLayout layout = MemoryLayout.structLayout(TAG_LAYOUT, unionLayout.withName("payload"));
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
        String clsName = clsSegment.reinterpret(Long.MAX_VALUE).getString(0);
        String message = messageSegment.reinterpret(Long.MAX_VALUE).getString(0);
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

  private static String getVersion() {
    MemorySegment ptrSegment =
        LOOKUP.findOrThrow("caxton1_version").reinterpret(ValueLayout.ADDRESS.byteSize());
    MemorySegment strSegment = ptrSegment.get(ValueLayout.ADDRESS, 0).reinterpret(Long.MAX_VALUE);
    return strSegment.getString(0);
  }

  @SuppressWarnings("NonAsciiCharacters") // Go StreamCodec yourself
  private static void þrö(Throwable e) {
    if (e instanceof RuntimeException re) throw re;
    if (e instanceof Error re) throw re;
    else throw new RuntimeException(e);
  }
}
