// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.TextCursors;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.ImePreEditWidget;
import net.minecraft.client.gui.widget.ScrollableTextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
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

  @Shadow @Final private static int UNFOCUSED_BOX_TEXT_COLOR;

  @Shadow private @Nullable ImePreEditWidget preEditWidget;

  public EditBoxWidgetMixin(
      int x, int y, int width, int height, Text narration, Scrollbar scrollbarSettings) {
    super(x, y, width, height, narration, scrollbarSettings);
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

    if (text.isEmpty() && !this.isFocused()) {
      context.drawWrappedTextWithShadow(
          this.textRenderer,
          this.placeholder,
          this.getTextX(),
          this.getTextY(),
          this.width - this.getPadding(),
          UNFOCUSED_BOX_TEXT_COLOR);
      ci.cancel();
      return;
    }

    int cursorPos = this.editBox.getCursor();
    boolean drawCursor =
        this.isFocused()
            && TextCursors.shouldShowMedialCursor(
                Util.getMeasuringTimeMs() - this.lastSwitchFocusTime);
    boolean needsValidCursorPos = this.preEditWidget != null;
    boolean cursorVertical = cursorPos < text.length();

    float cursorX = 0;
    int cursorY = 0;
    int textY = this.getTextY();
    int textX = this.getTextX();
    boolean hasDrawnCursor = false;
    for (EditBox.Substring line : this.editBox.getLines()) {
      boolean lineVisible = this.isVisible(textY, textY + this.textRenderer.fontHeight);
      if (!hasDrawnCursor
          && (needsValidCursorPos || drawCursor)
          && cursorVertical
          && cursorPos >= line.beginIndex()
          && cursorPos <= line.endIndex()) {
        if (lineVisible) {
          String lineStr = text.substring(line.beginIndex(), line.endIndex());
          // Draw vertical cursor for this line
          Voepfxo.drawText(
              context,
              this.textRenderer,
              lineStr,
              (float) textX,
              (float) textY,
              this.textColor,
              this.textShadow);
          cursorX =
              textX
                  + cth.getOffsetAtIndex(
                      CaxtonText.fromForwards(
                          lineStr,
                          ctr.getFontStorageAccessor(),
                          Style.EMPTY,
                          false,
                          false,
                          cth.getCache()),
                      cursorPos - line.beginIndex(),
                      DirectionSetting.AUTO);
          cursorY = textY;
          if (drawCursor) {
            Voepfxo.drawMedialCursor(
                context, cursorX, textY, this.cursorColor, this.textRenderer.fontHeight + 1);
          }
          hasDrawnCursor = true;
        }
      } else if (lineVisible) {
        String lineStr = text.substring(line.beginIndex(), line.endIndex());
        Voepfxo.drawText(
            context,
            this.textRenderer,
            lineStr,
            (float) textX,
            (float) textY,
            this.textColor,
            this.textShadow);
        if ((needsValidCursorPos || drawCursor) && !cursorVertical) {
          cursorX = textX + cth.getWidth(lineStr);
          cursorY = textY;
        }
      }
      textY += this.textRenderer.fontHeight;
    }
    if (drawCursor
        && !cursorVertical
        && this.isVisible(cursorY, cursorY + this.textRenderer.fontHeight)) {
      Voepfxo.drawTrailingCursor(
          context, this.textRenderer, cursorX, (float) cursorY, this.cursorColor, this.textShadow);
    }

    if (this.editBox.hasSelection()) {
      EditBox.Substring selection = this.editBox.getSelection();
      int selX = this.getTextX();
      textY = this.getTextY();
      for (EditBox.Substring line : this.editBox.getLines()) {
        if (selection.beginIndex() > line.endIndex()) {
          // Selection not yet reached
          textY += this.textRenderer.fontHeight;
          continue;
        }
        // Passed end of selection?
        if (line.beginIndex() > selection.endIndex()) break;

        if (this.isVisible(textY, textY + this.textRenderer.fontHeight)) {
          float selY = textY;
          CaxtonText caxtonLine =
              CaxtonText.fromForwards(
                  text.substring(line.beginIndex(), line.endIndex()),
                  ctr.getFontStorageAccessor(),
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
                    context,
                    selX + left,
                    selY,
                    selX + right,
                    selY + this.textRenderer.fontHeight,
                    true);
              });
        }
        textY += this.textRenderer.fontHeight;
      }
    }

    if (this.isHovered()) {
      context.setCursor(StandardCursors.IBEAM);
    }

    if (this.preEditWidget != null) {
      this.preEditWidget.setPosition((int) cursorX, cursorY);
      context.setPreEditWidget(this.preEditWidget);
    }

    ci.cancel(); // Ḋo.
  }
}
