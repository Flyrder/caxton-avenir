// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import net.minecraft.client.render.DrawSpec;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.oit.OitPipelineSet;
import net.minecraft.client.render.state.ScissorState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.flirora.caxton.layout.RenderSetupExt;
import xyz.flirora.caxton.render.CaxtonTextRenderLayers;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {
  @Shadow @Final private RenderSetup renderSetup;

  @WrapOperation(
      method = "getDrawSpec",
      at =
          @At(
              value = "NEW",
              target =
                  "(Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/render/oit/OitPipelineSet;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/state/ScissorState;Ljava/util/List;)Lnet/minecraft/client/render/DrawSpec;"))
  private DrawSpec sendUnitRangeUniform(
      String name,
      RenderPipeline pipeline,
      OitPipelineSet oitPipelineSet,
      GpuBufferSlice dynamicTransforms,
      ScissorState scissorState,
      List<DrawSpec.Texture> textures,
      Operation<DrawSpec> original) {
    GpuBuffer unitRangeBuffer = ((RenderSetupExt) (Object) this.renderSetup).caxton$getUnitRange();
    return ScopedValue.where(CaxtonTextRenderLayers.UNIT_RANGE_UNIFORM, unitRangeBuffer)
        .call(
            () ->
                original.call(
                    name, pipeline, oitPipelineSet, dynamicTransforms, scissorState, textures));
  }
}
