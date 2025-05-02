// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ShaderProgram.class)
public abstract class ShaderMixin {
  @Shadow public static Set<String> PREDEFINED_UNIFORMS;

  // Needed for some reason on NeoForge.
  @Inject(at = @At("RETURN"), method = "<clinit>")
  private static void onInitializePredefinedUniforms(CallbackInfo ci) {
    PREDEFINED_UNIFORMS.add("UnitRangeU");
  }
}
