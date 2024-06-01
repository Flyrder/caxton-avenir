// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.*;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.font.FontTech;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderLayers {
  private final Group msdf =
      new Group(
          RenderLayerFunctions::ofText,
          RenderLayerFunctions::ofTextSeeThrough,
          RenderLayerFunctions::ofTextOffset,
          RenderLayerFunctions::ofTextOutline);
  private final Group raster =
      new Group(
          RenderLayerFunctions::ofTextR,
          RenderLayerFunctions::ofTextSeeThroughR,
          RenderLayerFunctions::ofTextOffsetR,
          null);
  private final Group rasterAa =
      new Group(
          RenderLayerFunctions::ofTextRA,
          RenderLayerFunctions::ofTextSeeThroughRA,
          RenderLayerFunctions::ofTextOffsetRA,
          null);

  public void clear() {
    msdf.clear();
    raster.clear();
  }

  public RenderLayer text(
      CaxtonAtlas.Page page, CaxtonTextLayerType layerType, FontTech tech, boolean blur) {
    return (switch (tech) {
      case MSDF -> msdf.text(page, layerType);
      case RASTER -> (blur ? rasterAa : raster).text(page, layerType);
    });
  }

  public static class Group {
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> text = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textSeeThrough =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textOffset =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textOutline =
        new ConcurrentHashMap<>();
    private final Function<CaxtonAtlas.Page, RenderLayer> ofText,
        ofTextSeeThrough,
        ofTextOffset,
        ofTextOutline;

    public Group(
        Function<CaxtonAtlas.Page, RenderLayer> ofText,
        Function<CaxtonAtlas.Page, RenderLayer> ofTextSeeThrough,
        Function<CaxtonAtlas.Page, RenderLayer> ofTextOffset,
        Function<CaxtonAtlas.Page, RenderLayer> ofTextOutline) {
      this.ofText = ofText;
      this.ofTextSeeThrough = ofTextSeeThrough;
      this.ofTextOffset = ofTextOffset;
      this.ofTextOutline = ofTextOutline;
    }

    public void clear() {
      text.clear();
      textSeeThrough.clear();
      textOffset.clear();
      textOutline.clear();
    }

    public RenderLayer text(CaxtonAtlas.Page page, CaxtonTextLayerType layerType) {
      return switch (layerType) {
        case NORMAL -> text.computeIfAbsent(page, this.ofText);
        case SEE_THROUGH -> textSeeThrough.computeIfAbsent(page, this.ofTextSeeThrough);
        case OUTLINE -> textOutline.computeIfAbsent(page, this.ofTextOutline);
        case POLYGON_OFFSET -> textOffset.computeIfAbsent(page, this.ofTextOffset);
      };
    }
  }

  private static class RenderLayerFunctions extends RenderLayer {
    // not used; only here because I’m lazy
    public RenderLayerFunctions(
        String name,
        VertexFormat vertexFormat,
        VertexFormat.DrawMode drawMode,
        int expectedBufferSize,
        boolean hasCrumbling,
        boolean translucent,
        Runnable startAction,
        Runnable endAction) {
      super(
          name,
          vertexFormat,
          drawMode,
          expectedBufferSize,
          hasCrumbling,
          translucent,
          startAction,
          endAction);
    }

    private static RenderLayer make(
        String name,
        Supplier<net.minecraft.client.gl.ShaderProgram> shader,
        CaxtonAtlas.Page page,
        boolean seeThrough,
        boolean outline,
        boolean rasterized,
        boolean blur,
        boolean polygonOffset) {
      return RenderLayer.of(
          name,
          outline
              ? CaxtonVertexFormats.POSITION_COLOR_COLOR_TEXTURE_LIGHT
              : VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
          VertexFormat.DrawMode.QUADS,
          256,
          false,
          CaxtonModClient.CONFIG.sortTextRenderLayers,
          RenderLayer.MultiPhaseParameters.builder()
              .program(new Shayder(shader, handleTextShader(page, !rasterized)))
              .texture(new RenderPhase.Texture(page.getId(), blur, page.getNumMipmapLevels() > 1))
              .transparency(TRANSLUCENT_TRANSPARENCY)
              .lightmap(ENABLE_LIGHTMAP)
              .depthTest(seeThrough ? RenderPhase.ALWAYS_DEPTH_TEST : RenderPhase.LEQUAL_DEPTH_TEST)
              .layering(
                  polygonOffset ? RenderPhase.POLYGON_OFFSET_LAYERING : RenderPhase.NO_LAYERING)
              .build(false));
    }

    private static RenderLayer ofText(CaxtonAtlas.Page page) {
      return make(
          "caxton_text",
          () -> CaxtonShaders.caxtonTextShader,
          page,
          false,
          false,
          false,
          true,
          false);
    }

    private static RenderLayer ofTextSeeThrough(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_see_through",
          () -> CaxtonShaders.caxtonTextSeeThroughShader,
          page,
          true,
          false,
          false,
          true,
          false);
    }

    private static RenderLayer ofTextOffset(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_outline",
          () -> CaxtonShaders.caxtonTextShader,
          page,
          false,
          false,
          false,
          true,
          true);
    }

    private static RenderLayer ofTextOutline(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_outline",
          () -> CaxtonShaders.caxtonTextOutlineShader,
          page,
          false,
          true,
          false,
          true,
          false);
    }

    private static RenderLayer ofTextR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_raster",
          GameRenderer::getRenderTypeTextIntensityProgram,
          page,
          false,
          false,
          true,
          false,
          false);
    }

    private static RenderLayer ofTextSeeThroughR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_see_through_raster",
          GameRenderer::getRenderTypeTextIntensitySeeThroughProgram,
          page,
          true,
          false,
          true,
          false,
          false);
    }

    private static RenderLayer ofTextOffsetR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_outline_raster",
          GameRenderer::getRenderTypeTextIntensityProgram,
          page,
          false,
          false,
          true,
          false,
          true);
    }

    private static RenderLayer ofTextRA(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_raster",
          GameRenderer::getRenderTypeTextIntensityProgram,
          page,
          false,
          false,
          true,
          true,
          false);
    }

    private static RenderLayer ofTextSeeThroughRA(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_see_through_raster",
          GameRenderer::getRenderTypeTextIntensitySeeThroughProgram,
          page,
          true,
          false,
          true,
          true,
          false);
    }

    private static RenderLayer ofTextOffsetRA(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_outline_raster",
          GameRenderer::getRenderTypeTextIntensityProgram,
          page,
          false,
          false,
          true,
          true,
          true);
    }

    private static Consumer<net.minecraft.client.gl.ShaderProgram> handleTextShader(
        CaxtonAtlas.Page texId, boolean setUniforms) {
      if (!setUniforms) return shader -> {};
      return shader -> {
        if (shader == null) return;
        GlUniform unitRange = ((ShaderExt) shader).caxton$getUnitRange();
        if (unitRange != null) {
          unitRange.set(((float) texId.getRange()) / CaxtonAtlas.PAGE_SIZE);
        }
      };
    }

    public static class Shayder extends RenderPhase.ShaderProgram {
      public Shayder(
          Supplier<net.minecraft.client.gl.ShaderProgram> supplier,
          Consumer<net.minecraft.client.gl.ShaderProgram> callback) {
        super(
            () -> {
              net.minecraft.client.gl.ShaderProgram shader = supplier.get();
              callback.accept(shader);
              return shader;
            });
      }
    }
  }
}
