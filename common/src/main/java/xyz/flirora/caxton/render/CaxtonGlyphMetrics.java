// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import net.minecraft.client.font.GlyphMetrics;

public record CaxtonGlyphMetrics(float shadowOffset, float advance) implements GlyphMetrics {
  @Override
  public float getAdvance() {
    return advance;
  }

  @Override
  public float getAdvance(boolean bold) {
    return advance;
  }

  @Override
  public float getBoldOffset() {
    return 0;
  }

  @Override
  public float getShadowOffset() {
    return shadowOffset;
  }
}
