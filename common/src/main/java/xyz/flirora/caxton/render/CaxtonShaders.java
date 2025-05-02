// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

@Environment(EnvType.CLIENT)
public class CaxtonShaders {
  public static final Identifier TEXT_ID =
      Identifier.of(CaxtonModClient.MOD_ID, "core/rendertype_text");
  public static final Identifier TEXT_SEE_THROUGH_ID =
      Identifier.of(CaxtonModClient.MOD_ID, "core/rendertype_text_see_through");
  public static final Identifier TEXT_OUTLINE_ID =
      Identifier.of(CaxtonModClient.MOD_ID, "core/rendertype_text_outline");

  public static final RenderPipeline TEXT =
      RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
          .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text"))
          .withVertexShader(TEXT_ID)
          .withFragmentShader(TEXT_ID)
          .withSampler("Sampler0")
          .withSampler("Sampler2")
          .withUniform("UnitRange", UniformType.UNIFORM_BUFFER)
          .build();
  public static final RenderPipeline TEXT_OFFSET =
      RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
          .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_polygon_offset"))
          .withVertexShader(TEXT_ID)
          .withFragmentShader(TEXT_ID)
          .withSampler("Sampler0")
          .withSampler("Sampler2")
          .withDepthBias(-1.0f, -10.0f)
          .withUniform("UnitRange", UniformType.UNIFORM_BUFFER)
          .build();
  public static final RenderPipeline TEXT_SEE_THROUGH =
      RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
          .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_see_through"))
          .withVertexShader(TEXT_SEE_THROUGH_ID)
          .withFragmentShader(TEXT_SEE_THROUGH_ID)
          .withSampler("Sampler0")
          .withDepthWrite(false)
          .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
          .withUniform("UnitRange", UniformType.UNIFORM_BUFFER)
          .build();
  public static final RenderPipeline TEXT_OUTLINE =
      RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
          .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_outline"))
          .withVertexShader(TEXT_OUTLINE_ID)
          .withFragmentShader(TEXT_OUTLINE_ID)
          .withSampler("Sampler0")
          .withSampler("Sampler2")
          .withUniform("UnitRange", UniformType.UNIFORM_BUFFER)
          .build();

  // Hacky shit to deal with Mojang fuckage
  public static GpuBuffer currentUnitRangeBuffer = null;
}
