// (C) 2026 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.fabric.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.FontType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.flirora.caxton.font.CaxtonFontLoader;

@Mixin(FontType.class)
public enum FontTypeMixin {
  CAXTON_TTF("caxton", CaxtonFontLoader.CODEC);

  @Shadow
  FontTypeMixin(final String id, final MapCodec<? extends FontLoader> loaderCodec) {}
}
