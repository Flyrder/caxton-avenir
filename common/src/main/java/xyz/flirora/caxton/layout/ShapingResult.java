// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.flirora.caxton.font.CaxtonFont;

/**
 * A result of shaping a run of text. Typically, a separate instance is returned for each bidi run.
 *
 * <p>This is returned by the native method {@link xyz.flirora.caxton.dll.CaxtonInternal#shape(long,
 * char[], int[])}.
 *
 * <p>The {@link #data} field is laid out as an interleaved array, with each glyph represented by 6
 * {@code int}s:
 *
 * <table>
 *     <thead><tr><th>Offset</th><th>Use</th></tr></thead>
 *     <tbody>
 *         <tr><td>0</td><td><a href="https://docs.rs/harfrust/latest/harfrust/struct.GlyphInfo.html#structfield.glyph_id">Glyph ID</a>, plus {@code 1 << 24} if this glyph is marked as <a href="https://docs.rs/harfrust/latest/harfrust/struct.GlyphInfo.html#method.unsafe_to_break">unsafe to break</a>. (Bits 16 to 23 are unused for <a href="https://github.com/harfbuzz/boring-expansion-spec/blob/main/beyond-64k.md">potential future extensions to glyph limits</a>.)</td></tr>
 *         <tr><td>1</td><td><a href="https://docs.rs/harfrust/latest/harfrust/struct.GlyphInfo.html#structfield.cluster">Cluster index</a></td></tr>
 *         <tr><td>2</td><td><var>x</var>-advance, in font units</td></tr>
 *         <tr><td>3</td><td><var>y</var>-advance, in font units</td></tr>
 *         <tr><td>4</td><td><var>x</var>-offset, in font units</td></tr>
 *         <tr><td>5</td><td><var>y</var>-offset, in font units</td></tr>
 *     </tbody>
 * </table>
 *
 * <p><strong>Glyphs are always returned in visual order.</strong>
 *
 * @param data An array holding <a
 *     href="https://docs.rs/harfrust/latest/harfrust/struct.GlyphInfo.html">glyph</a> and <a
 *     href="https://docs.rs/harfrust/latest/harfrust/struct.GlyphPosition.html">position</a> data
 *     (see impl note)
 * @param totalWidth The sum of the <var>x</var>-advances for the glyphs
 * @param totalLength The length of the string that was laid out, in UTF-16 code units
 * @implNote The unsafe-to-break flag is unused, as Caxton does not yet support reusing shaping
 *     results after line breaking. The <var>y</var>-advance data is also unused, since Caxton does
 *     not support vertical layout.
 */
@Environment(EnvType.CLIENT)
public record ShapingResult(int[] data, int totalWidth, int totalLength) {
  public ShapingResult {
    if (data.length % 6 != 0) {
      throw new IllegalArgumentException("length of data must be divisible by 6");
    }
  }

  public int numGlyphs() {
    return data.length / 6;
  }

  public int glyphId(int i) {
    return data[6 * i] & 0xFF_FFFF;
  }

  public boolean isUnsafeToBreak(int i) {
    return (data[6 * i] & 0x100_0000) != 0;
  }

  public int clusterIndex(int i) {
    return data[6 * i + 1];
  }

  private int leftboundLimit(int i) {
    int curr = clusterIndex(i);
    for (int j = i - 1; j >= 0; --j) {
      int prev = clusterIndex(j);
      if (prev != curr) return prev;
    }
    return -1;
  }

  private int rightboundLimit(int i) {
    int curr = clusterIndex(i);
    for (int j = i + 1; j < this.numGlyphs(); ++j) {
      int next = clusterIndex(j);
      if (next != curr) return next;
    }
    return -1;
  }

  public int clusterLimit(int i) {
    int curr = clusterIndex(i);
    int prev = this.leftboundLimit(i);
    int next = this.rightboundLimit(i);
    if (prev < 0 && next < curr) {
      prev = this.totalLength;
    }
    if (next < 0 && prev < curr) {
      next = this.totalLength;
    }
    return prev > next ? prev : next;
  }

  public int advanceX(int i) {
    return data[6 * i + 2];
  }

  public int advanceY(int i) {
    return data[6 * i + 3];
  }

  public int offsetX(int i) {
    return data[6 * i + 4];
  }

  public int offsetY(int i) {
    return data[6 * i + 5];
  }

  public String toString() {
    StringBuilder builder = new StringBuilder("ShapingResult[glyphs=[");
    this.serializeGlyphs(builder, null);
    builder.append("], totalWidth=");
    builder.append(totalWidth);
    builder.append("]");
    return builder.toString();
  }

  public void serializeGlyphs(StringBuilder builder, CaxtonFont font) {
    // Emulate HB’s serialization format. *sigh*
    // https://harfbuzz.github.io/harfbuzz-hb-buffer.html#hb-buffer-serialize-glyphs
    for (int i = 0; i < numGlyphs(); ++i) {
      if (i > 0) {
        builder.append("|");
      }
      if (font != null) {
        builder.append(font.getGlyphName(glyphId(i)));
      } else {
        builder.append(glyphId(i));
      }
      if (isUnsafeToBreak(i)) {
        builder.append("!");
      }
      builder.append("=");
      builder.append(clusterIndex(i));
      int offsetX = offsetX(i),
          offsetY = offsetY(i),
          advanceX = advanceX(i),
          advanceY = advanceY(i);
      if (offsetX != 0 || offsetY != 0) {
        builder.append("@");
        builder.append(offsetX);
        builder.append(",");
        builder.append(offsetY);
      }
      builder.append("+");
      builder.append(advanceX);
      if (advanceY != 0) {
        builder.append(",");
        builder.append(advanceY);
      }
    }
  }
}
