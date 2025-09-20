// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.Glyph;

/**
 * Describes a pair of glyphs: a non-advance-validating glyph and an advance-validating glyph. The
 * distinction is currently supported only for legacy fonts; Caxton fonts do not distinguish the
 * two.
 */
@Environment(EnvType.CLIENT)
public interface CaxtonGlyphPair {
  /**
   * Gets one of the glyphs in the glyph pair depending on whether to validate advances.
   *
   * @param advanceValidating If true, then gets the advance-validating glyph. If false, then gets
   *     the non-advance-validating glyph.
   * @return a {@link CaxtonGlyphResult} corresponding to the glyph chosen
   */
  CaxtonGlyphResult getGlyph(boolean advanceValidating);

  /**
   * A glyph pair coming from a Caxton font. This does not hold information about the particular
   * glyph because in general, a Caxton font might use different glyphs for the same code point
   * depending on surrounding text.
   *
   * @param font the {@link ConfiguredCaxtonFont} that the glyph came from
   */
  record Caxton(ConfiguredCaxtonFont font) implements CaxtonGlyphPair {
    @Override
    public CaxtonGlyphResult getGlyph(boolean advanceValidating) {
      return new CaxtonGlyphResult.Caxton(font);
    }
  }

  /**
   * A glyph pair coming from a legacy font.
   *
   * @param glyph the non-advance-validating {@link Glyph}
   * @param advanceValidatingGlyph the advance-validating {@link Glyph}
   */
  record Legacy(Supplier<BakedGlyph> glyph, Supplier<BakedGlyph> advanceValidatingGlyph)
      implements CaxtonGlyphPair {
    @Override
    public CaxtonGlyphResult getGlyph(boolean advanceValidating) {
      return new CaxtonGlyphResult.Legacy(advanceValidating ? advanceValidatingGlyph : glyph);
    }
  }
}
