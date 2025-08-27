// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.minecraft.client.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontManager.Fonts.class)
public interface FontManagerFontsAccessor {
  @Accessor("field_61607")
  FontManager getOuter();

  @Accessor
  boolean getAdvanceValidating();
}
