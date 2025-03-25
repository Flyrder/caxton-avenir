// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.util.TriState;
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
          RenderLayerFunctions::ofTextR);
  private final Group rasterAa =
      new Group(
          RenderLayerFunctions::ofTextRA,
          RenderLayerFunctions::ofTextSeeThroughRA,
          RenderLayerFunctions::ofTextOffsetRA,
          RenderLayerFunctions::ofTextRA);

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

  private abstract static class RenderLayerFunctions extends RenderLayer {
    // not used; only here because I’m lazy
    public RenderLayerFunctions(
        String name,
        int size,
        boolean hasCrumbling,
        boolean translucent,
        Runnable begin,
        Runnable end) {
      super(name, size, hasCrumbling, translucent, begin, end);
    }

    private static RenderLayer make(
        String name,
        RenderPipeline shader,
        CaxtonAtlas.Page page,
        boolean rasterized,
        boolean blur) {
      return RenderLayer.of(
          name,
          786432,
          false,
          false,
          shader,
          RenderLayer.MultiPhaseParameters.builder()
              .texture(
                  new RenderPhase.Texture(
                      page.getId(),
                      blur ? TriState.TRUE : TriState.FALSE,
                      page.getNumMipmapLevels() > 1))
              .target(RenderPhase.TRANSLUCENT_TARGET)
              .lightmap(ENABLE_LIGHTMAP)
              .texturing(
                  rasterized
                      ? RenderPhase.DEFAULT_TEXTURING
                      : new Texturing(
                          "set_unit_range",
                          () -> {
                            CaxtonShaders.unitRange =
                                ((float) page.getRange()) / CaxtonAtlas.PAGE_SIZE;
                          },
                          () -> {
                            CaxtonShaders.unitRange = 0.0f;
                          }))
              .build(false));
    }

    private static RenderLayer ofText(CaxtonAtlas.Page page) {
      return make("caxton_text", CaxtonShaders.TEXT_OFFSET, page, false, true);
    }

    private static RenderLayer ofTextSeeThrough(CaxtonAtlas.Page page) {
      return make("caxton_text_see_through", CaxtonShaders.TEXT_SEE_THROUGH, page, false, true);
    }

    private static RenderLayer ofTextOffset(CaxtonAtlas.Page page) {
      return make("caxton_text", CaxtonShaders.TEXT, page, false, true);
    }

    private static RenderLayer ofTextOutline(CaxtonAtlas.Page page) {
      return make("caxton_text_outline", CaxtonShaders.TEXT_OUTLINE, page, false, true);
    }

    private static RenderLayer ofTextR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_raster", RenderPipelines.RENDERTYPE_TEXT_INTENSITY, page, true, false);
    }

    private static RenderLayer ofTextSeeThroughR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_see_through_raster",
          RenderPipelines.RENDERTYPE_TEXT_INTENSITY_SEETHROUGH,
          page,
          true,
          false);
    }

    private static RenderLayer ofTextOffsetR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_outline_raster",
          RenderPipelines.RENDERTYPE_TEXT_INTENSITY,
          page,
          true,
          false);
    }

    private static RenderLayer ofTextRA(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_raster", RenderPipelines.RENDERTYPE_TEXT_INTENSITY, page, true, true);
    }

    private static RenderLayer ofTextSeeThroughRA(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_see_through_raster",
          RenderPipelines.RENDERTYPE_TEXT_INTENSITY_SEETHROUGH,
          page,
          true,
          true);
    }

    private static RenderLayer ofTextOffsetRA(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_outline_raster",
          RenderPipelines.RENDERTYPE_TEXT_INTENSITY,
          page,
          true,
          true);
    }

    //        public static class Shayder extends RenderPhase.ShaderProgram {
    //            private final ShaderProgramKey key;
    //            private final CaxtonAtlas.Page page;
    //
    //            public Shayder(ShaderProgramKey key, CaxtonAtlas.Page page) {
    //                super(key);
    //                this.key = key;
    //                this.page = page;
    //            }
    //
    //            @Override
    //            public void startDrawing() {
    //                var shader = RenderSystem.setShader(this.key);
    //                GlUniform unitRange = ((ShaderExt) shader).caxton$getUnitRange();
    //                if (unitRange != null) {
    //                    unitRange.set(((float) page.getRange()) / CaxtonAtlas.PAGE_SIZE);
    //                }
    //            }
    //        }
  }
}
