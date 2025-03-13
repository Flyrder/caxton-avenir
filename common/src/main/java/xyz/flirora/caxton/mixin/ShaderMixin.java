// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.CaxtonShaders;
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

  @Inject(at = @At("RETURN"), method = "set")
  private void onInit(CallbackInfo ci) {
    this.caxton$initUniforms();
  }

  @Inject(at = @At("HEAD"), method = "initializeUniforms")
  private void onInitializeUniforms(
      VertexFormat.DrawMode drawMode,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      float f,
      float g,
      CallbackInfo ci) {
    if (this.unitRange != null) this.unitRange.set(CaxtonShaders.unitRange);
  }
}
