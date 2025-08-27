// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.info.BakedGlyph;
import org.jetbrains.annotations.Nullable;

/** Describes a glyph from either a Caxton or a legacy font. */
@Environment(EnvType.CLIENT)
public sealed interface CaxtonGlyphResult {
  /**
   * Returns the Caxton font for this glyph.
   *
   * @return the {@link ConfiguredCaxtonFont} that this glyph came from, or {@code null} if it came
   *     from a legacy font
   */
  @Nullable ConfiguredCaxtonFont getCaxtonFont();

  /**
   * A glyph coming from a Caxton font. This does not hold information about the particular glyph
   * because in general, a Caxton font might use different glyphs for the same code point depending
   * on surrounding text.
   *
   * @param font the {@link ConfiguredCaxtonFont} that the glyph came from
   */
  record Caxton(ConfiguredCaxtonFont font) implements CaxtonGlyphResult {
    @Override
    public @Nullable ConfiguredCaxtonFont getCaxtonFont() {
      return font;
    }
  }

  /**
   * A glyph coming from a legacy font.
   *
   * @param glyph the {@link Glyph}
   */
  record Legacy(Supplier<BakedGlyph> glyph) implements CaxtonGlyphResult {
    @Override
    public @Nullable ConfiguredCaxtonFont getCaxtonFont() {
      return null;
    }
  }
}
