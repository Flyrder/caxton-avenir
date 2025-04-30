// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class CaxtonBakedGlyph extends BakedGlyph {

  public CaxtonBakedGlyph(
      TextRenderLayerSet textRenderLayers,
      @Nullable GpuTexture gpuTexture,
      float minU,
      float maxU,
      float minV,
      float maxV,
      float minX,
      float maxX,
      float minY,
      float maxY) {
    super(textRenderLayers, gpuTexture, minU, maxU, minV, maxV, minX, maxX, minY, maxY);
  }

  public void draw(
      BakedGlyph.DrawnGlyph glyph, Matrix4f matrix, VertexConsumer vertexConsumer, int light) {}

  public void addGlyphToGui(
      GuiRenderState guiState,
      RenderLayer layer,
      BakedGlyph.DrawnGlyph drawnGlyph,
      boolean shadow,
      TextGuiElementRenderState textElementState) {
    Style style = drawnGlyph.style();
    float x = drawnGlyph.x() + (shadow ? drawnGlyph.shadowOffset() : 0.0F);
    float y = drawnGlyph.y() + (shadow ? drawnGlyph.shadowOffset() : 0.0F);
    int color = shadow ? drawnGlyph.shadowColor() : drawnGlyph.color();
    boolean bold = style.isBold();
    this.addGlyphToGui(guiState, layer, x, y, color, shadow, textElementState);
    if (bold) {
      this.addGlyphToGui(
          guiState, layer, x + drawnGlyph.boldOffset(), y, color, shadow, textElementState);
    }
  }

  private void addGlyphToGui(
      GuiRenderState guiState,
      RenderLayer layer,
      float x,
      float y,
      int color,
      boolean shadow,
      TextGuiElementRenderState textElementState) {
    // TODO (will need AW)
    //        float x0 = x + this.minX;
    //        float x1 = x + this.maxX;
    //        float y0 = y + this.minY;
    //        float y1 = y + this.maxY;
    //        float j = italic ? 1.0F - 0.25F * this.minY : 0.0F;
    //        float k = italic ? 1.0F - 0.25F * this.maxY : 0.0F;
    //        float l = bold ? 0.1F : 0.0F;
    //        guiState.forceAddSimpleElementToCurrentLayer(
    //                new GlyphGuiElementRenderState(
    //                        layer.getPipeline(),
    //                        TextureSetup.of(this.gpuTexture),
    //                        textElementState.pose(),
    //                        x0 - l,
    //                        y0 - l,
    //                        x1 + l,
    //                        y1 + l,
    //                        j,
    //                        k,
    //                        this.minU,
    //                        this.maxU,
    //                        this.minV,
    //                        this.maxV,
    //                        color,
    //                        textElementState.textRenderState().packedLightCoords(),
    //                        textElementState.scissorArea()
    //                )
    //        );
  }

  public void addEffectToGui(
      GuiRenderState guiState,
      RenderLayer layer,
      BakedGlyph.Rectangle rect,
      boolean shadow,
      TextGuiElementRenderState textElementState) {}

  public void addBackgroundToGui(
      GuiRenderState guiState,
      RenderLayer layer,
      BakedGlyph.Rectangle rect,
      TextGuiElementRenderState textElementState) {}

  public RenderLayer getLayer(TextRenderer.TextLayerType layerType) {}
}
