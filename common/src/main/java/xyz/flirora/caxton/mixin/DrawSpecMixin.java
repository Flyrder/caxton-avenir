// (C) 2026 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.MultiDrawBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.client.render.DrawSpec;
import net.minecraft.client.render.oit.OitPipelineSet;
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
      String name,
      RenderPipeline pipeline,
      OitPipelineSet oitPipelineSet,
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
      method = "drawInternal",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIIII)V"))
  private void setUnitRangeUniform(
      MultiDrawBuffer.DrawCall drawCall,
      RenderPass renderPass,
      RenderPipeline pipeline,
      CallbackInfo ci) {
    if (caxton$unitRangeBuffer != null) renderPass.setUniform("UnitRangeU", caxton$unitRangeBuffer);
  }
}
