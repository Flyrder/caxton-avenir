// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.EditBox;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {
  @Shadow private int cursor;
  @Unique private int caxton$ttwLength;

  @Shadow
  protected abstract EditBox.Substring getCurrentLine();

  @ModifyArg(
      method = "moveCursorLine",
      at =
          @At(
              value = "INVOKE",
              target = "Ljava/lang/String;substring(II)Ljava/lang/String;",
              ordinal = 0),
      index = 1)
  private int forceEntireLine(int cursor) {
    return this.getCurrentLine().endIndex();
  }

  @Redirect(
      method = "moveCursorLine",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I",
              ordinal = 0))
  private int redirectGetWidth(TextRenderer textRenderer, String text) {
    // text contains the contents of the whole line rather than up to the cursor
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonText caxtonText =
        CaxtonText.fromForwards(
            text,
            ctr.getFontStorageAccessor(),
            Style.EMPTY,
            false,
            false,
            ctr.getHandler().getCache());
    return Math.round(
        ctr.getHandler()
            .getOffsetAtIndex(
                caxtonText,
                this.cursor - this.getCurrentLine().beginIndex(),
                DirectionSetting.AUTO));
  }

  @Redirect(
      method = "moveCursorLine",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/font/TextRenderer;trimToWidth(Ljava/lang/String;I)Ljava/lang/String;"))
  private String redirectTrimToWidthL(TextRenderer textRenderer, String text, int maxWidth) {
    // reinterpreted to be getCharIndexAtX
    this.caxton$ttwLength =
        textRenderer.getTextHandler().getTrimmedLength(text, maxWidth, Style.EMPTY);
    return null;
  }

  @Redirect(
      method = "moveCursorLine",
      at = @At(value = "INVOKE", target = "Ljava/lang/String;length()I"))
  private int redirectTrimmedStringLengthL(String s) {
    return caxton$ttwLength;
  }

  @Redirect(
      method = "moveCursor(DD)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/font/TextRenderer;trimToWidth(Ljava/lang/String;I)Ljava/lang/String;"))
  private String redirectTrimToWidthXy(TextRenderer textRenderer, String text, int maxWidth) {
    // reinterpreted to be getCharIndexAtX
    this.caxton$ttwLength =
        textRenderer.getTextHandler().getTrimmedLength(text, maxWidth, Style.EMPTY);
    return null;
  }

  @Redirect(
      method = "moveCursor(DD)V",
      at = @At(value = "INVOKE", target = "Ljava/lang/String;length()I"))
  private int redirectTrimmedStringLengthXy(String s) {
    return caxton$ttwLength;
  }
}
