// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlShaderProgram;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GlShaderProgram.class)
public abstract class ShaderMixin {
  @Final @Shadow public static Set<String> predefinedUniforms;

  // Needed for some reason on NeoForge.
  @Inject(at = @At("RETURN"), method = "<clinit>")
  private static void onInitializePredefinedUniforms(CallbackInfo ci) {
    predefinedUniforms.add("UnitRangeU");
  }
}
