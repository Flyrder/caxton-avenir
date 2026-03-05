// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import static net.minecraft.client.gl.RenderPipelines.register;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.DepthTest;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .withSampler("Sampler0")
              .withSampler("Sampler2")
              .withUniform("UnitRangeU", UniformType.UNIFORM_BUFFER)
              .build());

  public static final RenderPipeline GUI_TEXT =
      register(
          RenderPipeline.builder(RenderPipelines.GUI_TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/gui_text"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .withSampler("Sampler0")
              .withSampler("Sampler2")
              .withUniform("UnitRangeU", UniformType.UNIFORM_BUFFER)
              .build());

  public static final RenderPipeline TEXT_OFFSET =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_polygon_offset"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .withSampler("Sampler0")
              .withSampler("Sampler2")
              .withDepthStencilState(
                  new DepthStencilState(DepthTest.LESS_THAN_OR_EQUAL, true, -1.0f, -10.0f))
              .withUniform("UnitRangeU", UniformType.UNIFORM_BUFFER)
              .build());

  public static final RenderPipeline TEXT_SEE_THROUGH =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_see_through"))
              .withVertexShader(TEXT_SEE_THROUGH_ID)
              .withFragmentShader(TEXT_SEE_THROUGH_ID)
              .withSampler("Sampler0")
              //                    .withDepthWrite(false)
              //                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
              .withUniform("UnitRangeU", UniformType.UNIFORM_BUFFER)
              .build());

  public static final RenderPipeline TEXT_OUTLINE =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_outline"))
              .withVertexShader(TEXT_OUTLINE_ID)
              .withFragmentShader(TEXT_OUTLINE_ID)
              .withSampler("Sampler0")
              .withSampler("Sampler2")
              .withUniform("UnitRangeU", UniformType.UNIFORM_BUFFER)
              .build());
  public static final RenderPipeline TEXT_OUTLINE_RASTER =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_outline_raster"))
              .withVertexShader("core/rendertype_text_intensity")
              .withFragmentShader("core/rendertype_text_intensity")
              .withSampler("Sampler0")
              .withSampler("Sampler2")
              .build());
}
