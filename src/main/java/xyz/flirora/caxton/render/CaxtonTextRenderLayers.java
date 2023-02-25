package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.*;
import xyz.flirora.caxton.font.FontTech;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderLayers {
    private final Group msdf = new Group(
            RenderLayerFunctions::ofText,
            RenderLayerFunctions::ofTextSeeThrough,
            RenderLayerFunctions::ofTextOutline);
    private final Group raster = new Group(
            RenderLayerFunctions::ofTextR,
            RenderLayerFunctions::ofTextSeeThroughR,
            RenderLayerFunctions::ofTextOutlineR);

    public void clear() {
        msdf.clear();
        raster.clear();
    }

    public RenderLayer text(CaxtonAtlas.Page page, TextRenderer.TextLayerType layerType, FontTech tech) {
        return (switch (tech) {
            case MSDF -> msdf.text(page, layerType);
            case RASTER -> raster.text(page, layerType);
        });
    }


    public static class Group {
        private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> text = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textSeeThrough = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textOutline = new ConcurrentHashMap<>();
        private final Function<CaxtonAtlas.Page, RenderLayer> ofText, ofTextSeeThrough, ofTextOutline;

        public Group(Function<CaxtonAtlas.Page, RenderLayer> ofText, Function<CaxtonAtlas.Page, RenderLayer> ofTextSeeThrough, Function<CaxtonAtlas.Page, RenderLayer> ofTextOutline) {
            this.ofText = ofText;
            this.ofTextSeeThrough = ofTextSeeThrough;
            this.ofTextOutline = ofTextOutline;
        }

        public void clear() {
            text.clear();
            textSeeThrough.clear();
            textOutline.clear();
        }

        public RenderLayer text(CaxtonAtlas.Page page, TextRenderer.TextLayerType layerType) {
            return switch (layerType) {
                case NORMAL -> text.computeIfAbsent(page, this.ofText);
                case SEE_THROUGH -> textSeeThrough.computeIfAbsent(page, this.ofTextSeeThrough);
                case POLYGON_OFFSET -> textOutline.computeIfAbsent(page, this.ofTextOutline);
            };
        }
    }

    private static class RenderLayerFunctions extends RenderLayer {
        // not used; only here because I’m lazy
        public RenderLayerFunctions(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
        }

        private static RenderLayer make(String name, Supplier<net.minecraft.client.gl.ShaderProgram> shader, CaxtonAtlas.Page page, boolean outline, boolean rasterized) {
            return RenderLayer.of(
                    name,
                    outline ?
                            CaxtonVertexFormats.POSITION_COLOR_COLOR_TEXTURE_LIGHT :
                            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(new Shayder(
                                    shader,
                                    handleTextShader(page, !rasterized)))
                            .texture(new RenderPhase.Texture(page.getId(), !rasterized, page.getNumMipmapLevels() > 1))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false));
        }

        private static RenderLayer ofText(CaxtonAtlas.Page page) {
            return make("caxton_text", () -> CaxtonShaders.caxtonTextShader, page, false, false);
        }

        private static RenderLayer ofTextSeeThrough(CaxtonAtlas.Page page) {
            return make("caxton_text_see_through", () -> CaxtonShaders.caxtonTextSeeThroughShader, page, false, false);
        }

        private static RenderLayer ofTextOutline(CaxtonAtlas.Page page) {
            return make("caxton_text_outline", () -> CaxtonShaders.caxtonTextOutlineShader, page, true, false);
        }

        private static RenderLayer ofTextR(CaxtonAtlas.Page page) {
            return make("caxton_text_raster", GameRenderer::getRenderTypeTextIntensityProgram, page, false, true);
        }

        private static RenderLayer ofTextSeeThroughR(CaxtonAtlas.Page page) {
            return make("caxton_text_see_through_raster", GameRenderer::getRenderTypeTextIntensitySeeThroughProgram, page, false, true);
        }

        private static RenderLayer ofTextOutlineR(CaxtonAtlas.Page page) {
            return make("caxton_text_outline_raster", GameRenderer::getRenderTypeTextIntensityProgram, page, true, true);
        }

        private static Consumer<net.minecraft.client.gl.ShaderProgram> handleTextShader(CaxtonAtlas.Page texId, boolean setUniforms) {
            if (!setUniforms) return shader -> {
            };
            return shader -> {
                if (shader == null) return;
                GlUniform unitRange = ((ShaderExt) shader).getUnitRange();
                if (unitRange != null) {
                    unitRange.set(((float) texId.getRange()) / CaxtonAtlas.PAGE_SIZE);
                }
            };
        }

        public static class Shayder extends RenderPhase.ShaderProgram {
            public Shayder(
                    Supplier<net.minecraft.client.gl.ShaderProgram> supplier,
                    Consumer<net.minecraft.client.gl.ShaderProgram> callback) {
                super(() -> {
                    net.minecraft.client.gl.ShaderProgram shader = supplier.get();
                    callback.accept(shader);
                    return shader;
                });
            }
        }
    }
}
