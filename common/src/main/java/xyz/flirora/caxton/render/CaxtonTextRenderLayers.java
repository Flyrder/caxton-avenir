// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPipelines;
import net.minecraft.client.render.RenderSetup;
import org.jspecify.annotations.Nullable;
import xyz.flirora.caxton.font.FontTech;
import xyz.flirora.caxton.layout.RenderSetupExt;
import xyz.flirora.caxton.mixin.RenderLayerAccessor;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderLayers {
  public static final ScopedValue<@Nullable GpuBuffer> UNIT_RANGE_UNIFORM =
      ScopedValue.newInstance();

  private final Group msdf =
      new Group(
          RenderLayerFunctions::ofText,
          RenderLayerFunctions::ofTextSeeThrough,
          RenderLayerFunctions::ofTextOffset,
          RenderLayerFunctions::ofTextOutline,
          CaxtonShaders.GUI_TEXT,
          CaxtonShaders.TEXT_OUTLINE);
  private final Group raster =
      new Group(
          RenderLayerFunctions::ofTextR,
          RenderLayerFunctions::ofTextSeeThroughR,
          RenderLayerFunctions::ofTextOffsetR,
          RenderLayerFunctions::ofTextR,
          RenderPipelines.GUI_TEXT_GRAYSCALE,
          CaxtonShaders.TEXT_OUTLINE_RASTER);

  public void clear() {
    msdf.clear();
    raster.clear();
  }

  public TextRenderLayerSet renderLayerSet(CaxtonAtlas.Page page, FontTech tech, boolean outline) {
    return (switch (tech) {
      case MSDF -> msdf.renderLayerSet(page, outline);
      case RASTER -> raster.renderLayerSet(page, outline);
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
    private final RenderPipeline pipeline, outlinePipeline;

    public Group(
        Function<CaxtonAtlas.Page, RenderLayer> ofText,
        Function<CaxtonAtlas.Page, RenderLayer> ofTextSeeThrough,
        Function<CaxtonAtlas.Page, RenderLayer> ofTextOffset,
        Function<CaxtonAtlas.Page, RenderLayer> ofTextOutline,
        RenderPipeline pipeline,
        RenderPipeline outlinePipeline) {
      this.ofText = ofText;
      this.ofTextSeeThrough = ofTextSeeThrough;
      this.ofTextOffset = ofTextOffset;
      this.ofTextOutline = ofTextOutline;
      this.pipeline = pipeline;
      this.outlinePipeline = outlinePipeline;
    }

    public void clear() {
      text.clear();
      textSeeThrough.clear();
      textOffset.clear();
      textOutline.clear();
    }

    public TextRenderLayerSet renderLayerSet(CaxtonAtlas.Page page, boolean outline) {
      if (outline) {
        return new TextRenderLayerSet(
            textOutline.computeIfAbsent(page, this.ofTextOutline),
            textSeeThrough.computeIfAbsent(page, this.ofTextSeeThrough),
            textOffset.computeIfAbsent(page, this.ofTextOffset),
            this.outlinePipeline);
      } else {
        return new TextRenderLayerSet(
            text.computeIfAbsent(page, this.ofText),
            textSeeThrough.computeIfAbsent(page, this.ofTextSeeThrough),
            textOffset.computeIfAbsent(page, this.ofTextOffset),
            this.pipeline);
      }
    }
  }

  private abstract static class RenderLayerFunctions {
    private static RenderLayer make(
        String name, RenderPipeline pipeline, CaxtonAtlas.Page page, boolean rasterized) {
      RenderSetup setup =
          RenderSetup.builder(pipeline).texture("Sampler0", page.getId()).useLightmap().build();
      if (!rasterized) {
        ((RenderSetupExt) (Object) setup).caxton$setUnitRange(page.getUnitRangeBuffer());
      }
      return RenderLayerAccessor.construct(name, setup);
    }

    private static RenderLayer ofText(CaxtonAtlas.Page page) {
      return make("caxton_text", CaxtonShaders.TEXT, page, false);
    }

    private static RenderLayer ofTextSeeThrough(CaxtonAtlas.Page page) {
      return make("caxton_text_see_through", CaxtonShaders.TEXT_SEETHROUGH, page, false);
    }

    private static RenderLayer ofTextOffset(CaxtonAtlas.Page page) {
      return make("caxton_text_polygon_offset", CaxtonShaders.TEXT_POLYGON_OFFSET, page, false);
    }

    private static RenderLayer ofTextOutline(CaxtonAtlas.Page page) {
      return make("caxton_text_outline", CaxtonShaders.TEXT_OUTLINE, page, false);
    }

    private static RenderLayer ofTextR(CaxtonAtlas.Page page) {
      return make("caxton_text_raster", RenderPipelines.TEXT_GRAYSCALE, page, true);
    }

    private static RenderLayer ofTextSeeThroughR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_see_through_raster", RenderPipelines.TEXT_GRAYSCALE_SEE_THROUGH, page, true);
    }

    private static RenderLayer ofTextOffsetR(CaxtonAtlas.Page page) {
      return make(
          "caxton_text_polygon_offset_raster",
          RenderPipelines.TEXT_GRAYSCALE_POLYGON_OFFSET,
          page,
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
