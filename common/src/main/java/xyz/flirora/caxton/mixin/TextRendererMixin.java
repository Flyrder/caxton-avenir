// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.render.CaxtonGlyphCache;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

@Environment(EnvType.CLIENT)
@Mixin(TextRenderer.class)
public abstract class TextRendererMixin implements HasCaxtonTextRenderer {
  @Shadow @Final private TextHandler handler;

  @Shadow
  public abstract boolean isRightToLeft();

  // Would like to put this in a FontManager mixin, but it doesn’t pass itself to TextRenderer’s
  // constructor.
  @Unique private static final Supplier<CaxtonGlyphCache> CACHE =
      Suppliers.memoize(
          () -> new CaxtonGlyphCache(MinecraftClient.getInstance().getTextureManager()));

  @Unique private CaxtonTextRenderer caxtonRenderer;

  @Inject(at = @At("TAIL"), method = "<init>")
  private void onInit(TextRenderer.GlyphsProvider fonts, CallbackInfo ci) {
    this.caxtonRenderer = new CaxtonTextRenderer((TextRenderer) (Object) this, handler, CACHE);
  }

  // Stub out TextRenderer#mirror call; we will call this on each
  // legacy run instead.
  // Note that we can’t simply ask rustybuzz to shape text as if it were
  // left-to-right; see <https://harfbuzz.github.io/harfbuzz-hb-buffer.html#hb-buffer-set-direction>
  // for more details.
  @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
  private void onMirror(String text, CallbackInfoReturnable<String> cir) {
    caxtonRenderer.rtl = true;
    cir.setReturnValue(text);
  }

  @Inject(at = @At("HEAD"), method = "drawWithOutline", cancellable = true)
  private void onDrawWithOutline(
      OrderedText text,
      float x,
      float y,
      int color,
      int outlineColor,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      int light,
      CallbackInfo ci) {
    caxtonRenderer.drawWithOutline(
        text, x, y, color, outlineColor, matrix, vertexConsumers, light, true);
    ci.cancel();
  }

  @Inject(
      at = @At("HEAD"),
      method =
          "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
      cancellable = true)
  private void onPrepareString(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      int backgroundColor,
      CallbackInfoReturnable<TextRenderer.GlyphDrawable> cir) {
    cir.setReturnValue(
        caxtonRenderer.prepare(
            CaxtonText.fromFormatted(
                text,
                caxtonRenderer.getFontStorageAccessor(),
                Style.EMPTY,
                caxtonRenderer.validatesAdvance(),
                this.isRightToLeft(),
                caxtonRenderer.getHandler().getCache()),
            x,
            y,
            color,
            shadow,
            false,
            backgroundColor,
            -1,
            Float.POSITIVE_INFINITY));
  }

  @Inject(
      at = @At("HEAD"),
      method =
          "prepare(Lnet/minecraft/text/OrderedText;FFIZZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
      cancellable = true)
  private void onPrepareOrderedText(
      OrderedText text,
      float x,
      float y,
      int color,
      boolean shadow,
      boolean trackSkeletons,
      int backgroundColor,
      CallbackInfoReturnable<TextRenderer.GlyphDrawable> cir) {
    cir.setReturnValue(
        caxtonRenderer.prepare(
            CaxtonText.from(
                text,
                caxtonRenderer.getFontStorageAccessor(),
                caxtonRenderer.validatesAdvance(),
                this.isRightToLeft(),
                caxtonRenderer.getHandler().getCache()),
            x,
            y,
            color,
            shadow,
            trackSkeletons,
            backgroundColor,
            -1,
            Float.POSITIVE_INFINITY));
  }

  @Override
  public CaxtonTextRenderer getCaxtonTextRenderer() {
    return caxtonRenderer;
  }
}
