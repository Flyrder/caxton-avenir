// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.ShaderExt;

@Environment(EnvType.CLIENT)
@Mixin(ShaderProgram.class)
public abstract class ShaderMixin implements ShaderExt {
  @Unique @Nullable private GlUniform unitRange;

  @Override
  public @Nullable GlUniform caxton$getUnitRange() {
    return unitRange;
  }

  @Override
  public void caxton$initUniforms() {
    this.unitRange = this.getUniform("UnitRange");
  }

  @Shadow
  public abstract @Nullable GlUniform getUniform(String name);

  // We have to match a pattern
  @Inject(at = @At("RETURN"), method = "/^<init>$/")
  private void onInit(CallbackInfo ci) {
    this.caxton$initUniforms();
  }
}
