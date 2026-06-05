// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.render.CaxtonAtlas;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
  @Inject(
      method =
          "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/systems/RenderPass;bindTexture(Ljava/lang/String;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
              ordinal = 0))
  private void onBindSampler0(GuiRenderer.Draw draw, RenderPass pass, CallbackInfo ci) {
    GpuTextureView texture = draw.textureSetup().texure0();
    CaxtonAtlas.Page page =
        ((HasCaxtonTextRenderer) MinecraftClient.getInstance().textRenderer)
            .getCaxtonTextRenderer()
            .getCache()
            .getAtlas()
            .getPageByGpuTexture(texture.texture());
    if (page != null) {
      pass.setUniform("UnitRangeU", page.getUnitRangeBuffer());
    }
  }
}
