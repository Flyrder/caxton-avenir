// (C) 2026 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.IndexType;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.client.render.DrawSpec;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.state.ScissorState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.CaxtonTextRenderLayers;

@Mixin(DrawSpec.class)
public class DrawSpecMixin {
  @Mutable @Unique @Final private @Nullable GpuBuffer caxton$unitRangeBuffer;

  @Inject(method = "<init>", at = @At("RETURN"))
  private void receiveUnitRangeUniform(
      RenderPipeline pipeline,
      OutputTarget outputTarget,
      GpuBufferSlice dynamicTransforms,
      ScissorState scissorState,
      List<DrawSpec.Texture> textures,
      CallbackInfo ci) {
    try {
      this.caxton$unitRangeBuffer = CaxtonTextRenderLayers.UNIT_RANGE_UNIFORM.get();
    } catch (NoSuchElementException _) {
      this.caxton$unitRangeBuffer = null;
    }
  }

  @Inject(
      method =
          "draw(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/IndexType;III)V",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIIII)V"))
  private void setUnitRangeUniform(
      GpuBuffer vertexBuffer,
      GpuBuffer indexBuffer,
      IndexType indexType,
      int baseVertex,
      int firstIndex,
      int indexCount,
      CallbackInfo ci,
      @Local(name = "renderPass") RenderPass renderPass) {
    if (caxton$unitRangeBuffer != null) renderPass.setUniform("UnitRangeU", caxton$unitRangeBuffer);
  }
}
