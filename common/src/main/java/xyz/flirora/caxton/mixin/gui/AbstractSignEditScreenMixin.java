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
import net.minecraft.client.render.RenderLayer;
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
import xyz.flirora.caxton.mixin.DrawContextAccessor;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
  @Shadow @Final private SignBlockEntity blockEntity;
  @Shadow @Final private String[] messages;
  @Shadow private SelectionManager selectionManager;
  @Shadow private int currentRow;
  @Unique private CaxtonText rowText;
  @Unique private CaxtonText currentRowText;

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
                  "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
              ordinal = 0))
  private int drawRowTextProxy(
      DrawContext instance,
      TextRenderer textRenderer,
      String currentLine,
      int x,
      int y,
      int color,
      boolean shadow) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();
    this.rowText =
        CaxtonText.fromFormatted(
            currentLine,
            ctr::getFontStorage,
            Style.EMPTY,
            false,
            this.textRenderer.isRightToLeft(),
            cth.getCache());
    // The use of `==` is intentional; this check will pass if we are
    // indeed on the current row.
    if (currentLine == this.messages[this.currentRow]) this.currentRowText = rowText;
    int x2 =
        (int)
            ctr.draw(
                this.rowText,
                -cth.getWidth(rowText) / 2.0f,
                y,
                color,
                false,
                instance.getMatrices().peek().getPositionMatrix(),
                instance.getVertexConsumers(),
                false,
                0,
                0xF000F0,
                0,
                Float.POSITIVE_INFINITY);
    ((DrawContextAccessor) instance).callDraw();
    return x2;
  }

  // Use the more precise CaxtonTextHandler.getWidth instead of TextHandler.getWidth
  @Redirect(
      method = "renderSignText",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
              ordinal = 1))
  private int drawEndCursorProxy(
      DrawContext instance,
      TextRenderer textRenderer,
      String text,
      int x,
      int y,
      int color,
      boolean shadow) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();
    return Voepfxo.drawText(instance, textRenderer, "_", cth.getWidth(rowText) / 2.0f, y, color);
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

    float x = cth.getWidth(currentRowText) / 2;
    float cursorOffset =
        cth.getOffsetAtIndex(
            currentRowText, Math.min(selectionStart, currentLine.length()), DirectionSetting.AUTO);
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
              target =
                  "Lnet/minecraft/client/gui/DrawContext;fill(Lnet/minecraft/client/render/RenderLayer;IIIII)V"))
  private void drawHighlights(
      DrawContext context, RenderLayer renderLayer, int x1, int y1, int x2, int y2, int color) {
    CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
    CaxtonTextHandler cth = ctr.getHandler();

    int selectionStart = selectionManager.getSelectionStart();
    int selectionEnd = selectionManager.getSelectionEnd();
    float x = cth.getWidth(this.currentRowText) / 2;
    int yAdj = 4 * this.blockEntity.getTextLineHeight() / 2;
    int lineY = currentRow * this.blockEntity.getTextLineHeight() - yAdj;

    cth.getHighlightRanges(
        currentRowText,
        Math.min(selectionStart, selectionEnd),
        Math.max(selectionStart, selectionEnd),
        (start, end) -> {
          Voepfxo.fill(
              context,
              renderLayer,
              start - x,
              lineY,
              end - x,
              lineY + this.blockEntity.getTextLineHeight(),
              color);
        });
  }
}
