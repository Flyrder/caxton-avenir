// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

@Environment(EnvType.CLIENT)
public record CaxtonGlyphGuiElementRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2f pose,
    float x1,
    float y1,
    float x2,
    float y2,
    float shearY1,
    float shearY2,
    float u1,
    float u2,
    float v1,
    float v2,
    int color,
    int packedLightCoords,
    @Nullable ScreenRect scissorArea)
    implements SimpleGuiElementRenderState {
  @Override
  public void setupVertices(VertexConsumer vertices, float depth) {
    vertices
        .vertex(this.pose(), this.x1() + this.shearY1(), this.y1(), depth)
        .texture(this.u1(), this.v1())
        .light(this.packedLightCoords())
        .color(this.color());
    vertices
        .vertex(this.pose(), this.x1() + this.shearY2(), this.y2(), depth)
        .texture(this.u1(), this.v2())
        .light(this.packedLightCoords())
        .color(this.color());
    vertices
        .vertex(this.pose(), this.x2() + this.shearY2(), this.y2(), depth)
        .texture(this.u2(), this.v2())
        .light(this.packedLightCoords())
        .color(this.color());
    vertices
        .vertex(this.pose(), this.x2() + this.shearY1(), this.y1(), depth)
        .texture(this.u2(), this.v1())
        .light(this.packedLightCoords())
        .color(this.color());
  }

  @Nullable @Override
  public ScreenRect bounds() {
    return null;
  }
}
