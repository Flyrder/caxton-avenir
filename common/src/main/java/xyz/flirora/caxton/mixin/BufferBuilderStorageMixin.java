// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilderStorage;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.CLIENT)
@Mixin(BufferBuilderStorage.class)
public abstract class BufferBuilderStorageMixin {
  //  @Inject(
  //      method = "method_54639",
  //      at =
  //          @At(
  //              value = "INVOKE",
  //              target =
  //
  // "Lnet/minecraft/client/render/BufferBuilderStorage;assignBufferBuilder(Lit/unimi/dsi/fastutil/objects/Object2ObjectLinkedOpenHashMap;Lnet/minecraft/client/render/RenderLayer;)V"))
  //  private void addRenderLayers(
  //      Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> map, CallbackInfo ci) {
  //    //    assignBufferBuilder(map, RenderLayer.getGuiTextHighlight());
  //  }
  //
  //  @WrapOperation(
  //      method = "<init>",
  //      at =
  //          @At(
  //              value = "INVOKE",
  //              target =
  //
  // "Lnet/minecraft/client/render/VertexConsumerProvider;immediate(Ljava/util/SequencedMap;Lnet/minecraft/client/util/BufferAllocator;)Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"),
  //      slice =
  //          @Slice(
  //              from =
  //                  @At(
  //                      value = "FIELD",
  //                      opcode = Opcodes.PUTFIELD,
  //                      target =
  //
  // "Lnet/minecraft/client/render/BufferBuilderStorage;blockBufferBuildersPool:Lnet/minecraft/client/render/chunk/BlockBufferBuilderPool;"),
  //              to =
  //                  @At(
  //                      value = "FIELD",
  //                      opcode = Opcodes.PUTFIELD,
  //                      target =
  //
  // "Lnet/minecraft/client/render/BufferBuilderStorage;entityVertexConsumers:Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;")),
  //      allow = 1)
  //  private VertexConsumerProvider.Immediate redirect(
  //      SequencedMap<RenderLayer, BufferAllocator> layerBuffers,
  //      BufferAllocator fallbackBuffer,
  //      Operation<VertexConsumerProvider.Immediate> original) {
  //    if (CaxtonModClient.FORCE_DISABLE_BATCHING_HACK)
  //      return original.call(layerBuffers, fallbackBuffer);
  //    return new WorldRendererVertexConsumerProvider(fallbackBuffer, layerBuffers);
  //  }
}
