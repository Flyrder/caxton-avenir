// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.compat.immediatelyfast.BatchingApi;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "renderExperienceLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"), cancellable = true)
    private void onRenderExpText(DrawContext context, float _x, CallbackInfo ci) {
        if (!CaxtonModClient.CONFIG.tweakExpText)
            return;
        String levelStr = "" + this.client.player.experienceLevel;
        float x = (context.getScaledWindowWidth() - this.getTextRenderer().getTextHandler().getWidth(levelStr))
                / 2.0f;
        float y = context.getScaledWindowHeight() - 31 - 4;
        BatchingApi api = CaxtonModClient.getBatchingApi();
        if (api.isBatching()) {
            api.endHudBatching();
            api.beginHudBatching();
        }
        Voepfxo.drawText4Way(context, this.getTextRenderer(), Text.literal(levelStr).asOrderedText(), x, y, 0x80ff20,
                0);
        this.client.getProfiler().pop();
        ci.cancel();
    }
}
