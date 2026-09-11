// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import static net.minecraft.client.render.RenderPipelines.register;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderPipelines;
import net.minecraft.client.render.oit.OitPipelineSet;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

@Environment(EnvType.CLIENT)
public class CaxtonShaders {
  public static final Identifier TEXT_ID = CaxtonModClient.id("core/text");
  public static final BindGroupLayout UNIT_RANGE_LAYOUT =
      BindGroupLayout.builder().buffer("UnitRangeU", UniformType.UNIFORM_BUFFER).build();

  public static final RenderPipeline.Snippet BASE_TEXT_SNIPPET =
      RenderPipeline.builder(RenderPipelines.BASE_TEXT_SNIPPET)
          .vertexShader(TEXT_ID)
          .fragmentShader(TEXT_ID)
          .bindGroupLayout(UNIT_RANGE_LAYOUT)
          .buildSnippet();
  public static final RenderPipeline.Snippet TEXT_SNIPPET =
      RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
          .vertexShader(TEXT_ID)
          .fragmentShader(TEXT_ID)
          .bindGroupLayout(UNIT_RANGE_LAYOUT)
          .buildSnippet();
  public static final RenderPipeline.Snippet GUI_TEXT_SNIPPET =
      RenderPipeline.builder(RenderPipelines.GUI_TEXT_SNIPPET)
          .vertexShader(TEXT_ID)
          .fragmentShader(TEXT_ID)
          .bindGroupLayout(UNIT_RANGE_LAYOUT)
          .buildSnippet();

  public static final RenderPipeline TEXT =
      register(
          RenderPipeline.builder(TEXT_SNIPPET)
              .id(CaxtonModClient.id("pipeline/text"))
              .colorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
              .build());
  public static final OitPipelineSet OIT_TEXT =
      register(OitPipelineSet.builder("caxton_text", RenderPipeline.builder(TEXT_SNIPPET)).build());

  public static final RenderPipeline GUI_TEXT =
      register(
          RenderPipeline.builder(GUI_TEXT_SNIPPET)
              .id(CaxtonModClient.id("pipeline/gui_text"))
              .build());

  public static final RenderPipeline TEXT_POLYGON_OFFSET =
      register(
          RenderPipeline.builder(TEXT_SNIPPET)
              .id(CaxtonModClient.id("pipeline/text_polygon_offset"))
              .colorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
              .depthStencilState(
                  new DepthStencilState(DepthTest.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
              .build());
  public static final OitPipelineSet OIT_TEXT_POLYGON_OFFSET =
      register(
          OitPipelineSet.builder(
                  "caxton_text_polygon_offset",
                  RenderPipeline.builder(TEXT_SNIPPET)
                      .depthStencilState(
                          new DepthStencilState(
                              DepthTest.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F)))
              .build());

  public static final RenderPipeline TEXT_SEETHROUGH =
      register(
          RenderPipeline.builder(BASE_TEXT_SNIPPET)
              .id(CaxtonModClient.id("pipeline/text_see_through"))
              .define("IS_SEE_THROUGH")
              .colorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
              .depthStencilState(Optional.empty())
              .build());

  public static final RenderPipeline TEXT_OUTLINE =
      register(
          RenderPipeline.builder(TEXT_SNIPPET)
              .id(CaxtonModClient.id("pipeline/text_outline"))
              .define("IS_OUTLINE")
              .colorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
              .build());
  public static final OitPipelineSet OIT_TEXT_OUTLINE =
      register(
          OitPipelineSet.builder(
                  "caxton_text_outline", RenderPipeline.builder(TEXT_SNIPPET).define("IS_OUTLINE"))
              .build());

  public static final RenderPipeline TEXT_OUTLINE_RASTER =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
              .id(CaxtonModClient.id("pipeline/text_outline_raster"))
              .vertexShader("core/text")
              .fragmentShader("core/text")
              .define("IS_GRAYSCALE")
              .colorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
              .build());
}
