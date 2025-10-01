// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin.gui;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.layout.gui.TextFieldWidgetExt;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin extends ClickableWidget
    implements Drawable, Element, TextFieldWidgetExt {
  private static final Style SUGGESTION = Style.EMPTY.withColor(0xFF808080);
  @Unique private static final Matrix4f IDENTITY = new Matrix4f();
  @Shadow @Final private static String HORIZONTAL_CURSOR;
  @Shadow @Final private TextRenderer textRenderer;
  @Shadow private String text;
  @Shadow private boolean editable;
  @Shadow private int editableColor;
  @Shadow private int uneditableColor;
  @Shadow private boolean drawsBackground;
  @Shadow private int firstCharacterIndex;
  @Unique private CaxtonText caxtonText;
  @Shadow private int selectionStart;
  @Shadow private int selectionEnd;
  @Shadow private @Nullable String suggestion;
  @Shadow private @Nullable Text placeholder;
  @Shadow private @Nullable Consumer<String> changedListener;
  @Unique private boolean updatingCaxtonText = false;

  public TextFieldWidgetMixin(int x, int y, int width, int height, Text message) {
    super(x, y, width, height, message);
  }

  @Shadow
  public abstract int getInnerWidth();

  @Shadow
  protected abstract int getMaxLength();

  @Shadow
  public abstract void setCursor(int cursor, boolean shiftKeyPressed);

  @Shadow
  public abstract void setSelectionStart(int cursor);

  @Shadow private long lastSwitchFocusTime;

  @Shadow private int textX;

  @Shadow private int textY;

  @Shadow
  protected abstract boolean isCentered();

  @Shadow
  protected abstract void updateTextPosition();

  @Shadow private boolean textShadow;

  @Shadow
  protected abstract OrderedText format(String string, int firstCharacterIndex);

  @Override
  public void updateCaxtonText(boolean changed) {
    // Protect against mods that do things such as *gasp* registering
    // changed listeners that set suggestion text.
    //        System.err.println("updateCaxtonText(changed = " + changed + ", updatingCaxtonText = "
    // + updatingCaxtonText + ") with text " + this.text);
    //        System.err.println("firstCharacterIndex = " + firstCharacterIndex + ", selection = " +
    // selectionStart + ".." + selectionEnd);
    if (updatingCaxtonText) return;
    updatingCaxtonText = true;

    if (changed && this.changedListener != null) {
      this.changedListener.accept(text);
    }

    try {
      boolean hideSuggestion = selectionStart < text.length() || text.length() >= getMaxLength();
      // We need to check for empty text – some mods (such as EMI)
      // assume that their render text providers will never be called
      // for an empty text box
      OrderedText text = this.text.isEmpty() ? OrderedText.EMPTY : this.format(this.text, 0);
      if (!hideSuggestion && suggestion != null) {
        text =
            OrderedText.concat(
                text, OrderedText.styledForwardsVisitedString(suggestion, SUGGESTION));
      }

      if (textRenderer == null) return;
      CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
      this.caxtonText =
          CaxtonText.from(
              text, ctr.getFontStorageAccessor(), true, false, ctr.getHandler().getCache());
      //        System.err.println("caxtonText = " + this.caxtonText);
    } finally {
      updatingCaxtonText = false;
      this.updateTextPosition();
    }
  }

  @Inject(at = @At("HEAD"), method = "onChanged", cancellable = true)
  private void stubOnChanged(String newText, CallbackInfo ci) {
    ci.cancel();
  }

  // We can’t inject on `onChanged`, as some methods that update `text`
  // call `setSelectionEnd` before `onChanged`.
  @Inject(
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;text:Ljava/lang/String;",
              opcode = Opcodes.PUTFIELD,
              shift = At.Shift.AFTER),
      method = "setText")
  private void updateCaxtonTextOnSetText(String text, CallbackInfo ci) {
    this.selectionStart = text.length();
    this.selectionEnd = selectionStart;
    updateCaxtonText(true);
  }

  @ModifyReceiver(
      method = "write",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionStart(I)V"))
  private TextFieldWidget updateCaxtonTextOnWrite(TextFieldWidget me, int newSelectionStart) {
    this.setSelectionStart(newSelectionStart);
    this.selectionEnd = selectionStart;
    updateCaxtonText(true);
    return me;
  }

  @Inject(
      method = "setCursor",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/widget/TextFieldWidget;onChanged(Ljava/lang/String;)V"))
  private void onSetCursor(int cursor, boolean shiftKeyPressed, CallbackInfo ci) {
    updateCaxtonText(false);
  }

  @ModifyReceiver(
      method = "eraseCharactersTo",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setCursor(IZ)V"))
  private TextFieldWidget updateCaxtonTextOnEraseCharacters(
      TextFieldWidget me, int cursor, boolean shiftKeyPressed) {
    this.setSelectionStart(cursor);
    this.selectionEnd = selectionStart;
    updateCaxtonText(true);
    return me;
  }

  @Inject(
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;text:Ljava/lang/String;",
              opcode = Opcodes.PUTFIELD,
              shift = At.Shift.AFTER),
      method = "setMaxLength")
  private void updateCaxtonTextOnSetMaxLength(int maxLength, CallbackInfo ci) {
    this.selectionStart = Math.min(selectionStart, text.length());
    this.selectionEnd = Math.min(selectionEnd, text.length());
    updateCaxtonText(true);
  }

  @Inject(at = @At("TAIL"), method = "setSuggestion")
  private void updateCaxtonTextOnSuggestionSet(String text, CallbackInfo ci) {
    updateCaxtonText(false);
  }

  @Inject(
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getInnerWidth()I"),
      method = "renderWidget",
      cancellable = true)
  private void onRenderButton(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();

    int color = editable ? editableColor : uneditableColor;
    int x = this.textX;
    int y = this.textY;

    float firstCharLocation = 0, selectionStartLocation = 0;

    if (caxtonText != null) {
      Voepfxo.drawText(
          context,
          textRenderer,
          caxtonText,
          x,
          y,
          color,
          this.textShadow,
          firstCharacterIndex,
          getInnerWidth());
      firstCharLocation =
          ctr.getHandler()
              .getOffsetAtIndex(caxtonText, firstCharacterIndex, DirectionSetting.FORCE_LTR);
      selectionStartLocation =
          ctr.getHandler().getOffsetAtIndex(caxtonText, selectionStart, DirectionSetting.AUTO);
    }

    boolean cursorIsVertical = selectionStart < text.length() || text.length() >= getMaxLength();

    float cursorLocation = selectionStartLocation - firstCharLocation;

    boolean cursorInBounds = 0 <= cursorLocation && cursorLocation < getInnerWidth();
    boolean showCursor =
        this.isFocused()
            && (Util.getMeasuringTimeMs() - this.lastSwitchFocusTime) / 300L % 2L == 0L
            && cursorInBounds;

    float cursorX = Math.round(x + cursorLocation);

    if (!cursorInBounds) {
      cursorX = cursorLocation > 0 ? x + width : x;
    }

    if (this.placeholder != null && text.isEmpty() && !this.isFocused()) {
      Voepfxo.drawTextWithShadow(context, this.textRenderer, this.placeholder, cursorX, y, color);
    }

    if (showCursor) {
      if (cursorIsVertical) {
        Voepfxo.fill(
            context, cursorX, y - 1, cursorX + 1, y + 1 + this.textRenderer.fontHeight, 0xFFD0D0D0);
      } else {
        Voepfxo.drawText(
            context, this.textRenderer, HORIZONTAL_CURSOR, cursorX, y, color, this.textShadow);
      }
    }

    if (selectionStart != selectionEnd) {
      float finalFirstCharLocation = firstCharLocation;
      ctr.getHandler()
          .getHighlightRanges(
              caxtonText,
              Math.min(selectionStart, selectionEnd),
              Math.max(selectionStart, selectionEnd),
              (x0, x1) ->
                  myDrawSelectionHighlight(
                      context,
                      x + x0 - finalFirstCharLocation,
                      y - 1,
                      x + x1 - finalFirstCharLocation,
                      y + 1 + textRenderer.fontHeight));
    }

    // Ḋo.
    ci.cancel();
  }

  @Inject(
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getInnerWidth()I"),
      method = "calculateCursorPos",
      cancellable = true)
  private void overrideCursorPosition(Click click, CallbackInfoReturnable<Integer> cir) {
    if (caxtonText == null) {
      updateCaxtonText(false);
    }

    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();

    float xOffset = Math.min((float) (click.x() - this.textX), this.getInnerWidth());

    int index = ctr.getHandler().getCharIndexAtX(caxtonText, xOffset, firstCharacterIndex);

    cir.setReturnValue(index);
  }

  @Inject(at = @At("HEAD"), method = "updateFirstCharacterIndex", cancellable = true)
  private void onUpdateFirstCharacterIndex(int cursor, CallbackInfo ci) {
    int length = this.text.length();

    if (this.textRenderer != null && this.caxtonText != null) {
      if (this.firstCharacterIndex > length) {
        this.firstCharacterIndex = length;
      }

      int width = getInnerWidth();

      CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();

      float firstCharLocation =
          ctr.getHandler()
              .getOffsetAtIndex(caxtonText, firstCharacterIndex, DirectionSetting.FORCE_LTR);
      float cursorLocation =
          ctr.getHandler().getOffsetAtIndex(caxtonText, cursor, DirectionSetting.AUTO);
      //            System.err.println("updateFirstCharacterIndex: text = " + this.text);
      //            System.err.println("firstCharacterIndex = " + firstCharacterIndex + ", cursor =
      // " + cursor);
      //            System.err.println("firstCharLocation = " + firstCharLocation + ",
      // cursorLocation = " + cursorLocation);
      //            System.err.println("width = " + width + " (raw: " + this.width + ")");

      if (cursor == this.firstCharacterIndex) {
        // Be compatible with mods that set the text of a text field before its width.
        // See #47 and #48
        this.firstCharacterIndex =
            ctr.getHandler().getCharIndexAtX(caxtonText, cursorLocation - width, -1);
      } else if (cursorLocation < firstCharLocation) {
        // Update firstCharacterIndex to point to a char farther to the left
        this.firstCharacterIndex =
            ctr.getHandler()
                .getCharIndexAtX(
                    caxtonText, Math.min(firstCharLocation - width, cursorLocation), -1);
      } else if (cursorLocation >= firstCharLocation + width) {
        // Update firstCharacterIndex to point to a char farther to the right
        this.firstCharacterIndex =
            ctr.getHandler().getCharIndexAfterX(caxtonText, cursorLocation - width, -1);
      }

      this.firstCharacterIndex = MathHelper.clamp(this.firstCharacterIndex, 0, length);
    }
    ci.cancel();
  }

  @Inject(method = "updateTextPosition", at = @At("HEAD"), cancellable = true)
  private void onUpdateTextPosition(CallbackInfo ci) {
    if (this.textRenderer != null) {
      if (this.isCentered()) {
        if (this.caxtonText == null) {
          this.textX = this.getX() + (this.getWidth() / 2);
        } else {
          CaxtonTextHandler handler =
              ((HasCaxtonTextRenderer) this.textRenderer).getCaxtonTextRenderer().getHandler();
          int i =
              handler.getCharIndexAtX(
                  this.caxtonText, this.getInnerWidth(), this.firstCharacterIndex, true);
          this.textX =
              Math.round(
                  this.getX()
                      + (this.getWidth()
                              - handler.getOffsetAtIndex(
                                  this.caxtonText, i, DirectionSetting.FORCE_LTR))
                          / 2);
        }
      } else {
        this.textX = this.getX() + (this.drawsBackground ? 4 : 0);
      }
      this.textY = this.drawsBackground ? this.getY() + (this.height - 8) / 2 : this.getY();
    }
    ci.cancel();
  }

  // Copy of drawSelectionHighlight with float arguments
  @Unique private void myDrawSelectionHighlight(
      DrawContext context, float x1, float y1, float x2, float y2) {
    float i;
    if (x1 < x2) {
      i = x1;
      x1 = x2;
      x2 = i;
    }
    if (y1 < y2) {
      i = y1;
      y1 = y2;
      y2 = i;
    }
    if (x2 > this.getX() + this.width) {
      x2 = this.getX() + this.width;
    }
    if (x1 > this.getX() + this.width) {
      x1 = this.getX() + this.width;
    }
    if (x2 < this.getX()) {
      x2 = this.getX();
    }
    if (x1 < this.getX()) {
      x1 = this.getX();
    }
    Voepfxo.drawSelection(context, x1, y1, x2, y2);
  }
}
