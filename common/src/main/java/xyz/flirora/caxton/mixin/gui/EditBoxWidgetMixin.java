// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.ScrollableTextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Mixin(EditBoxWidget.class)
public abstract class EditBoxWidgetMixin extends ScrollableTextFieldWidget {
  @Shadow @Final private static String UNDERSCORE;
  @Shadow @Final private EditBox editBox;
  @Shadow @Final private TextRenderer textRenderer;
  @Shadow @Final private Text placeholder;

  @Shadow private long lastSwitchFocusTime;

  @Shadow @Final private int textColor;

  @Shadow @Final private boolean textShadow;

  @Shadow @Final private int cursorColor;

  public EditBoxWidgetMixin(int i, int j, int k, int l, Text text) {
    super(i, j, k, l, text);
  }

  @Inject(
      method = "renderContents",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/EditBox;getCursor()I"),
      cancellable = true)
  private void onRenderContents(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) this.textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();
    String text = this.editBox.getText();
    int cursorPos = this.editBox.getCursor();
    boolean drawCursor =
        this.isFocused()
            && (Util.getMeasuringTimeMs() - this.lastSwitchFocusTime) / 300L % 2L == 0L;
    boolean cursorVertical = cursorPos < text.length();

    float cursorX = 0;
    int endY = 0;
    int y = this.getTextY();
    for (EditBox.Substring line : this.editBox.getLines()) {
      boolean lineVisible = this.isVisible(y, y + this.textRenderer.fontHeight);
      String lineStr = text.substring(line.beginIndex(), line.endIndex());
      int textX = this.getTextX();
      if (drawCursor
          && cursorVertical
          && cursorPos >= line.beginIndex()
          && cursorPos <= line.endIndex()) {
        if (lineVisible) {
          // Draw vertical cursor for this line
          Voepfxo.drawText(
              context,
              this.textRenderer,
              lineStr,
              (float) textX,
              (float) y,
              this.textColor,
              this.textShadow);
          cursorX =
              textX
                  + cth.getOffsetAtIndex(
                      CaxtonText.fromForwards(
                          lineStr, ctr::getFontStorage, Style.EMPTY, false, false, cth.getCache()),
                      cursorPos - line.beginIndex(),
                      DirectionSetting.AUTO);
          Voepfxo.fill(
              context,
              cursorX,
              y - 1,
              cursorX + 1,
              y + 1 + this.textRenderer.fontHeight,
              this.cursorColor);
        }
      } else {
        if (lineVisible) {
          Voepfxo.drawText(
              context,
              this.textRenderer,
              lineStr,
              (float) textX,
              (float) y,
              this.textColor,
              this.textShadow);
          cursorX = textX + cth.getWidth(lineStr);
        }
        endY = y;
      }
      y += this.textRenderer.fontHeight;
    }
    if (drawCursor
        && !cursorVertical
        && this.isVisible(endY, endY + this.textRenderer.fontHeight)) {
      Voepfxo.drawText(
          context,
          this.textRenderer,
          UNDERSCORE,
          cursorX,
          (float) endY,
          this.cursorColor,
          this.textShadow);
    }

    if (this.editBox.hasSelection()) {
      EditBox.Substring selection = this.editBox.getSelection();
      int selX = this.getTextX();
      y = this.getTextY();
      for (EditBox.Substring line : this.editBox.getLines()) {
        if (selection.beginIndex() > line.endIndex()) {
          // Selection not yet reached
          y += this.textRenderer.fontHeight;
          continue;
        }
        // Passed end of selection?
        if (line.beginIndex() > selection.endIndex()) break;

        if (this.isVisible(y, y + this.textRenderer.fontHeight)) {
          float selY = y;
          CaxtonText caxtonLine =
              CaxtonText.fromForwards(
                  text.substring(line.beginIndex(), line.endIndex()),
                  ctr::getFontStorage,
                  Style.EMPTY,
                  false,
                  false,
                  cth.getCache());
          cth.getHighlightRanges(
              caxtonLine,
              Math.max(0, selection.beginIndex() - line.beginIndex()),
              Math.min(selection.endIndex(), line.endIndex()) - line.beginIndex(),
              (left, right) -> {
                Voepfxo.drawSelection(
                    context, selX + left, selY, selX + right, selY + this.textRenderer.fontHeight);
              });
        }
        y += this.textRenderer.fontHeight;
      }
    }

    ci.cancel(); // Ḋo.
  }
}
