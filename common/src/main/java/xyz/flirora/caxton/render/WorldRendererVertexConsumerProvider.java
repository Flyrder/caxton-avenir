// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import xyz.flirora.caxton.mixin.RenderLayerAccessor;

/** A version of {@link VertexConsumerProvider.Immediate} that retains all Caxton text layers. */
@Environment(EnvType.CLIENT)
public class WorldRendererVertexConsumerProvider extends VertexConsumerProvider.Immediate {
  private static final Set<String> CAXTON_TEXT_RENDER_LAYER_NAMES =
      ImmutableSet.of(
          "caxton_text",
          "caxton_text_see_through",
          "caxton_text_outline",
          "caxton_text_polygon_offset");

  private final Map<RenderLayer, BufferAllocator> caxtonTextLayerAllocators = new HashMap<>();
  private final Map<RenderLayer, BufferBuilder> caxtonTextLayerBuilders = new HashMap<>();

  public WorldRendererVertexConsumerProvider(
      BufferAllocator fallbackBuffer, SequencedMap<RenderLayer, BufferAllocator> layerBuffers) {
    super(fallbackBuffer, layerBuffers);
  }

  private static boolean isCaxtonTextLayer(RenderLayer layer) {
    return CAXTON_TEXT_RENDER_LAYER_NAMES.contains(((RenderLayerAccessor) layer).getName());
  }

  // Well done, Mojang. You truly have yfucked Caxton over in 1.21.

  // The following methods are copied from VertexConsumerProvider.Immediate
  // (with some renaming).
  @Override
  public VertexConsumer getBuffer(RenderLayer renderLayer) {
    BufferBuilder bufferBuilder = this.pending.get(renderLayer);
    if (bufferBuilder != null
        && !renderLayer.areVerticesNotShared()
        && !isCaxtonTextLayer(renderLayer)) {
      this.draw(renderLayer, bufferBuilder);
      bufferBuilder = null;
    }
    if (bufferBuilder != null) {
      return bufferBuilder;
    }
    BufferAllocator bufferAllocator;
    if (isCaxtonTextLayer(renderLayer)) {
      bufferAllocator =
          this.caxtonTextLayerAllocators.computeIfAbsent(
              renderLayer, l -> new BufferAllocator(256));
      return this.caxtonTextLayerBuilders.computeIfAbsent(
          renderLayer,
          l ->
              new BufferBuilder(
                  bufferAllocator, renderLayer.getDrawMode(), renderLayer.getVertexFormat()));
    } else {
      bufferAllocator = this.layerBuffers.get(renderLayer);
    }
    if (bufferAllocator != null) {
      bufferBuilder =
          new BufferBuilder(
              bufferAllocator, renderLayer.getDrawMode(), renderLayer.getVertexFormat());
    } else {
      if (this.currentLayer != null && !isCaxtonTextLayer(this.currentLayer)) {
        this.draw(this.currentLayer);
      }
      bufferBuilder =
          new BufferBuilder(
              this.allocator, renderLayer.getDrawMode(), renderLayer.getVertexFormat());
      this.currentLayer = renderLayer;
    }
    if (isCaxtonTextLayer(renderLayer)) {
      this.caxtonTextLayerBuilders.put(renderLayer, bufferBuilder);
    } else {
      this.pending.put(renderLayer, bufferBuilder);
    }
    return bufferBuilder;
  }

  @Override
  public void drawCurrentLayer() {
    if (this.currentLayer != null) {
      if (!isCaxtonTextLayer(this.currentLayer)
          && !this.layerBuffers.containsKey(this.currentLayer)) {
        this.draw(this.currentLayer);
      }
      this.currentLayer = null;
    }
  }

  @Override
  public void draw() {
    this.drawCurrentLayer();

    this.drawCaxtonTextLayers();
    for (RenderLayer renderLayer : this.layerBuffers.keySet()) {
      this.draw(renderLayer);
    }
  }

  public void drawCaxtonTextLayers() {
    for (var entry : caxtonTextLayerBuilders.entrySet()) {
      RenderLayer layer = entry.getKey();
      BufferBuilder builder = entry.getValue();
      BuiltBuffer buffer = builder.endNullable();
      if (buffer != null) {
        layer.draw(buffer);
      }
      if (layer.equals(this.currentLayer)) {
        this.currentLayer = null;
      }
    }
    this.caxtonTextLayerBuilders.clear();
  }

  // The following two methods must be overridden because the implementation in
  // VertexConsumerProvider.Immediate calls its own getBufferInternal method,
  // which is not overridden by this class’s implementation.
  @Override
  public void draw(RenderLayer layer) {
    if (isCaxtonTextLayer(layer)) {
      return;
    }
    BufferBuilder bufferBuilder = this.pending.remove(layer);
    if (bufferBuilder != null) {
      this.draw(layer, bufferBuilder);
    }
  }

  private void draw(RenderLayer layer, BufferBuilder builder) {
    BuiltBuffer builtBuffer = builder.endNullable();
    if (builtBuffer != null) {
      if (layer.isTranslucent()) {
        BufferAllocator bufferAllocator = this.layerBuffers.getOrDefault(layer, this.allocator);
        builtBuffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
      }
      layer.draw(builtBuffer);
    }
    if (layer.equals(this.currentLayer)) {
      this.currentLayer = null;
    }
  }
}
