// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.CaxtonShaders;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
  @Inject(method = "bindDefaultUniforms", at = @At("RETURN"))
  private static void onBindDefaultUniforms(RenderPass pass, CallbackInfo ci) {
    if (CaxtonShaders.unitRange > 0.0f) {
      pass.setUniform("UnitRangeU", CaxtonShaders.UNIT_RANGE_BUFFER.get());
    }
  }
}
