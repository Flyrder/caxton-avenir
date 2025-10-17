// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
  @Shadow @Final protected SignBlockEntity blockEntity;
  @Shadow @Final private String[] messages;
  @Shadow private SelectionManager selectionManager;
  @Shadow private int currentRow;
  @Unique private CaxtonText caxton$rowText;
  @Unique private CaxtonText caxton$currentRowText;

  protected AbstractSignEditScreenMixin(Text title) {
    super(title);
  }

  // Use the more precise CaxtonTextHandler.getWidth instead of TextHandler.getWidth
  @Redirect(
      method = "renderSignText",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V",
              ordinal = 0))
  private void drawRowTextProxy(
      DrawContext instance,
      TextRenderer textRenderer,
      String currentLine,
      int x,
      int y,
      int color,
      boolean shadow) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();
    this.caxton$rowText =
        CaxtonText.fromFormatted(
            currentLine,
            ctr.getFontStorageAccessor(),
            Style.EMPTY,
            false,
            this.textRenderer.isRightToLeft(),
            cth.getCache());
    // The use of `==` is intentional; this check will pass if we are
    // indeed on the current row.
    if (currentLine == this.messages[this.currentRow]) this.caxton$currentRowText = caxton$rowText;
    Voepfxo.drawText(
        instance,
        textRenderer,
        this.caxton$rowText,
        -cth.getWidth(caxton$rowText) / 2.0f,
        y,
        color,
        false);
  }

  // Use the more precise CaxtonTextHandler.getWidth instead of TextHandler.getWidth
  @Redirect(
      method = "renderSignText",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)V",
              ordinal = 1))
  private void drawEndCursorProxy(
      DrawContext instance,
      TextRenderer textRenderer,
      String text,
      int x,
      int y,
      int color,
      boolean shadow) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();
    Voepfxo.drawText(instance, textRenderer, "_", cth.getWidth(caxton$rowText) / 2.0f, y, color);
  }

  @Redirect(
      method = "renderSignText",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
              ordinal = 0))
  private void drawVerticalCursor(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();
    String currentLine = this.messages[this.currentRow];
    int selectionStart = this.selectionManager.getSelectionStart();
    int yAdj = 4 * this.blockEntity.getTextLineHeight() / 2;
    int lineY = currentRow * this.blockEntity.getTextLineHeight() - yAdj;

    float x = cth.getWidth(caxton$currentRowText) / 2;
    float cursorOffset =
        cth.getOffsetAtIndex(
            caxton$currentRowText,
            Math.min(selectionStart, currentLine.length()),
            DirectionSetting.AUTO);
    float cursorX = cursorOffset - x;
    Voepfxo.fill(
        instance,
        cursorX,
        lineY - 1,
        cursorX + 1,
        lineY + this.blockEntity.getTextLineHeight(),
        0xFF000000 | color);
  }

  @Redirect(
      method = "renderSignText",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/DrawContext;drawSelection(IIIIZ)V"))
  private void drawHighlights(DrawContext context, int x1, int y1, int x2, int y2, boolean invert) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();

    int selectionStart = selectionManager.getSelectionStart();
    int selectionEnd = selectionManager.getSelectionEnd();
    float x = cth.getWidth(this.caxton$currentRowText) / 2;
    int yAdj = 4 * this.blockEntity.getTextLineHeight() / 2;
    int lineY = currentRow * this.blockEntity.getTextLineHeight() - yAdj;

    cth.getHighlightRanges(
        caxton$currentRowText,
        Math.min(selectionStart, selectionEnd),
        Math.max(selectionStart, selectionEnd),
        (start, end) -> {
          Voepfxo.drawSelection(
              context,
              start - x,
              lineY,
              end - x,
              lineY + this.blockEntity.getTextLineHeight(),
              invert);
        });
  }
}
