// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.SequencedMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.render.WorldRendererVertexConsumerProvider;

@Environment(EnvType.CLIENT)
@Mixin(BufferBuilderStorage.class)
public abstract class BufferBuilderStorageMixin {
    @Shadow
    protected static void assignBufferBuilder(
            Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> builderStorage,
            RenderLayer layer) {
    }

    @Inject(
            method = "method_54639",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/BufferBuilderStorage;assignBufferBuilder(Lit/unimi/dsi/fastutil/objects/Object2ObjectLinkedOpenHashMap;Lnet/minecraft/client/render/RenderLayer;)V"))
    private void addRenderLayers(
            Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> map, CallbackInfo ci) {
//    assignBufferBuilder(map, RenderLayer.getGuiTextHighlight());
    }

    @Redirect(
            method = "<init>",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Lnet/minecraft/client/render/VertexConsumerProvider;immediate(Ljava/util/SequencedMap;Lnet/minecraft/client/util/BufferAllocator;)Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"),
            slice =
            @Slice(
                    from =
                    @At(
                            value = "FIELD",
                            opcode = Opcodes.PUTFIELD,
                            target =
                                    "Lnet/minecraft/client/render/BufferBuilderStorage;blockBufferBuildersPool:Lnet/minecraft/client/render/chunk/BlockBufferBuilderPool;"),
                    to =
                    @At(
                            value = "FIELD",
                            opcode = Opcodes.PUTFIELD,
                            target =
                                    "Lnet/minecraft/client/render/BufferBuilderStorage;entityVertexConsumers:Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;")),
            allow = 1)
    private VertexConsumerProvider.Immediate redirect(
            SequencedMap<RenderLayer, BufferAllocator> layerBuffers, BufferAllocator fallbackBuffer) {
        if (CaxtonModClient.FORCE_DISABLE_BATCHING_HACK)
            return VertexConsumerProvider.immediate(layerBuffers, fallbackBuffer);
        return new WorldRendererVertexConsumerProvider(fallbackBuffer, layerBuffers);
    }
}
