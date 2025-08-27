// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FontStorage.LazyBakedGlyph.class)
public interface LazyBakedGlyphAccessor {
  @Invoker("<init>")
  static FontStorage.LazyBakedGlyph construct(FontStorage storage, Glyph glyph) {
    throw new UnsupportedOperationException("Implemented in mixin");
  }

  @Accessor
  Glyph getGlyph();
}
