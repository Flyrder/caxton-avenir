package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.render.WorldRendererVertexConsumerProvider;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Unique
    private boolean warnOnNonWrvcp;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initFields(MinecraftClient client, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
        warnOnNonWrvcp = !CaxtonModClient.FORCE_DISABLE_BATCHING_HACK;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
                    ordinal = 0),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getWaterMask()Lnet/minecraft/client/render/RenderLayer;")
            ))
    private void addTranslucentDraws(VertexConsumerProvider.Immediate immediate, RenderLayer layer) {
        immediate.draw(layer);
        if (immediate instanceof WorldRendererVertexConsumerProvider wrvcp) {
            wrvcp.drawCaxtonTextLayers();
        } else if (warnOnNonWrvcp) {
            CaxtonModClient.LOGGER.warn("World text batching hack was enabled but did not get a WorldRendererVertexConsumerProvider back. Assuming that another mod is batching world rendering.");
            warnOnNonWrvcp = false;
        }
    }
}
