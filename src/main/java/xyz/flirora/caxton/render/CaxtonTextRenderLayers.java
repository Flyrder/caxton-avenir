package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class CaxtonTextRenderLayers {
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> text = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textSeeThrough = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CaxtonAtlas.Page, RenderLayer> textOutline = new ConcurrentHashMap<>();

    public void clear() {
        text.clear();
        textSeeThrough.clear();
        textOutline.clear();
    }

    public RenderLayer text(CaxtonAtlas.Page page, TextRenderer.TextLayerType layerType) {
        return (switch (layerType) {
            case NORMAL -> text.computeIfAbsent(page, RenderLayerFunctions::ofText);
            case SEE_THROUGH -> textSeeThrough.computeIfAbsent(page, RenderLayerFunctions::ofTextSeeThrough);
            case POLYGON_OFFSET -> textOutline.computeIfAbsent(page, RenderLayerFunctions::ofTextOutline);
        });
    }

    private static class RenderLayerFunctions extends RenderLayer {
        // not used; only here because I’m lazy
        public RenderLayerFunctions(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
        }

        private static RenderLayer ofText(CaxtonAtlas.Page page) {
            return RenderLayer.of(
                    "caxton_text",
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(textShader(page))
                            .texture(new RenderPhase.Texture(page.getId(), true, false))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false));
        }

        private static RenderLayer ofTextSeeThrough(CaxtonAtlas.Page page) {
            return RenderLayer.of(
                    "caxton_text_see_through",
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(transparentTextShader(page))
                            .texture(new RenderPhase.Texture(page.getId(), true, false))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false));
        }

        private static RenderLayer ofTextOutline(CaxtonAtlas.Page page) {
            return RenderLayer.of(
                    "caxton_text_outline",
                    CaxtonVertexFormats.POSITION_COLOR_COLOR_TEXTURE_LIGHT,
                    VertexFormat.DrawMode.QUADS,
                    256,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(textOutlineShader(page))
                            .texture(new RenderPhase.Texture(page.getId(), true, false))
                            .transparency(TRANSLUCENT_TRANSPARENCY)
                            .lightmap(ENABLE_LIGHTMAP)
                            .build(false));
        }

        private static ShaderProgram textShader(CaxtonAtlas.Page texId) {
            return new Shayder(
                    () -> CaxtonShaders.caxtonTextShader,
                    handleTextShader(texId));
        }

        private static ShaderProgram transparentTextShader(CaxtonAtlas.Page texId) {
            return new Shayder(
                    () -> CaxtonShaders.caxtonTextSeeThroughShader,
                    handleTextShader(texId));
        }

        private static ShaderProgram textOutlineShader(CaxtonAtlas.Page texId) {
            return new Shayder(
                    () -> CaxtonShaders.caxtonTextOutlineShader,
                    handleTextShader(texId));
        }

        private static Consumer<net.minecraft.client.gl.ShaderProgram> handleTextShader(CaxtonAtlas.Page texId) {
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
