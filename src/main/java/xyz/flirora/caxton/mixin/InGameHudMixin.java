package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private int scaledWidth;

    @Shadow
    private int scaledHeight;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "renderExperienceBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"), cancellable = true)
    private void onRenderExpText(MatrixStack matrices, int _x, CallbackInfo ci) {
        if (!CaxtonModClient.CONFIG.tweakExpText) return;
        String levelStr = "" + this.client.player.experienceLevel;
        float x = (this.scaledWidth - this.getTextRenderer().getTextHandler().getWidth(levelStr)) / 2.0f;
        float y = this.scaledHeight - 31 - 4;
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        ((HasCaxtonTextRenderer) this.getTextRenderer()).getCaxtonTextRenderer().drawWithOutline(Text.literal(levelStr).asOrderedText(), x, y, 0x80ff20, 0, matrices.peek().getPositionMatrix(), immediate, 0xF000F0, false);
        immediate.draw();
        this.client.getProfiler().pop();
        ci.cancel();
    }
}
