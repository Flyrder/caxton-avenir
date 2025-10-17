// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SamplerCache;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GlyphGuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.CaxtonAtlas;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

@Environment(EnvType.CLIENT)
@Mixin(GlyphGuiElementRenderState.class)
public class GlyphGuiElementRenderStateMixin {
  @Mutable @Final @Unique private GpuSampler caxton$sampler;

  @Inject(method = "<init>", at = @At("RETURN"))
  private void onInit(
      Matrix3x2fc pose, TextDrawable renderable, ScreenRect scissorArea, CallbackInfo ci) {
    CaxtonAtlas.Page page =
        ((HasCaxtonTextRenderer) MinecraftClient.getInstance().textRenderer)
            .getCaxtonTextRenderer()
            .getCache()
            .getAtlas()
            .getPageByGpuTexture(renderable.textureView().texture());
    this.caxton$sampler =
        page != null ? page.getSampler() : RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
  }

  @Redirect(
      method = "textureSetup",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gl/SamplerCache;get(Lcom/mojang/blaze3d/textures/FilterMode;)Lnet/minecraft/client/gl/GpuSampler;"))
  private GpuSampler overrideSampler(SamplerCache instance, FilterMode filterMode) {
    return caxton$sampler;
  }
}
