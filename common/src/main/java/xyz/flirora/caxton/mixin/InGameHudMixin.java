// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.compat.immediatelyfast.BatchingApi;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(Bar.class)
public interface InGameHudMixin {
  @Inject(
      method = "drawExperienceLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"),
      cancellable = true)
  private static void onRenderExpText(
      DrawContext context, TextRenderer textRenderer, int level, CallbackInfo ci) {
    if (!CaxtonModClient.CONFIG.tweakExpText) return;
    Text text = Text.translatable("gui.experience.level", level);
    float x =
        (context.getScaledWindowWidth() - textRenderer.getTextHandler().getWidth(text)) / 2.0f;
    float y = context.getScaledWindowHeight() - 31 - 4;
    BatchingApi api = CaxtonModClient.getBatchingApi();
    if (api.isBatching()) {
      api.endHudBatching();
      api.beginHudBatching();
    }
    Voepfxo.drawText4Way(context, textRenderer, text.asOrderedText(), x, y, 0x80ff20, 0);
    ci.cancel();
  }
}
