// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontManager.class)
public interface FontManagerAccessor {
  @Accessor
  FontStorage getMissingStorage();
}
