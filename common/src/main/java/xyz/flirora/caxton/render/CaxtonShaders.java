// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import static net.minecraft.client.render.RenderPipelines.register;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.DepthTest;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderPipelines;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

@Environment(EnvType.CLIENT)
public class CaxtonShaders {
  public static final Identifier TEXT_ID = Identifier.of(CaxtonModClient.MOD_ID, "core/text");

  public static final RenderPipeline TEXT =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .build());
  public static final RenderPipeline GUI_TEXT =
      register(
          RenderPipeline.builder(RenderPipelines.GUI_TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/gui_text"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .build());

  public static final RenderPipeline TEXT_POLYGON_OFFSET =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_polygon_offset"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .withDepthStencilState(
                  new DepthStencilState(DepthTest.GREATER_THAN_OR_EQUAL, true, 1.0F, 10.0F))
              .build());

  public static final RenderPipeline TEXT_SEETHROUGH =
      register(
          RenderPipeline.builder(RenderPipelines.BASE_TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_see_through"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .withShaderDefine("IS_SEE_THROUGH")
              .withDepthStencilState(Optional.empty())
              .build());

  public static final RenderPipeline TEXT_OUTLINE =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_outline"))
              .withVertexShader(TEXT_ID)
              .withFragmentShader(TEXT_ID)
              .withShaderDefine("IS_OUTLINE")
              .build());
  public static final RenderPipeline TEXT_OUTLINE_RASTER =
      register(
          RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
              .withLocation(Identifier.of(CaxtonModClient.MOD_ID, "pipeline/text_outline_raster"))
              .withVertexShader("core/text")
              .withFragmentShader("core/text")
              .withShaderDefine("IS_GRAYSCALE")
              .build());
}
