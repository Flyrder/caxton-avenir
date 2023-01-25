package xyz.flirora.caxton.mixin.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
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
public abstract class EditBoxWidgetMixin extends ScrollableWidget {
    @Shadow
    @Final
    private static String UNDERSCORE;
    @Shadow
    @Final
    private EditBox editBox;
    @Shadow
    @Final
    private TextRenderer textRenderer;
    @Shadow
    @Final
    private Text placeholder;
    @Shadow
    private int tick;

    public EditBoxWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/EditBox;getCursor()I"), cancellable = true)
    private void onRenderContents(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) this.textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();
        String text = this.editBox.getText();
        int cursorPos = this.editBox.getCursor();
        boolean drawCursor = this.isFocused() && this.tick / 6 % 2 == 0;
        boolean cursorVertical = cursorPos < text.length();

        int x = 0;
        int endY = 0;
        int y = this.getY() + this.getPadding();
        for (EditBox.Substring line : this.editBox.getLines()) {
            boolean lineVisible = this.isVisible(y, y + this.textRenderer.fontHeight);
            String lineStr = text.substring(line.beginIndex(), line.endIndex());
            if (lineVisible) {
                if (drawCursor && cursorVertical && cursorPos >= line.beginIndex() && cursorPos <= line.endIndex()) {
                    // Draw vertical cursor for this line
                    x = this.textRenderer.drawWithShadow(
                            matrices,
                            lineStr,
                            (float) (this.getX() + this.getPadding()),
                            (float) y,
                            0xffe0e0e0) - 1;
                    float cursorX = this.getX() + this.getPadding() + cth.getOffsetAtIndex(CaxtonText.fromForwards(lineStr, ctr.getFontStorageAccessor(), Style.EMPTY, false, false, cth.getCache()), cursorPos - line.beginIndex(), DirectionSetting.AUTO);
                    Voepfxo.fill(matrices, cursorX, y - 1, cursorX + 1, y + 1 + this.textRenderer.fontHeight, 0xffd0d0d0);
                } else {
                    x = this.textRenderer.drawWithShadow(
                            matrices,
                            lineStr,
                            (float) (this.getX() + this.getPadding()),
                            (float) y,
                            0xffe0e0e0) - 1;
                }
            }
            endY = y;
            y += this.textRenderer.fontHeight;
        }
        if (drawCursor && !cursorVertical && this.isVisible(endY, endY + this.textRenderer.fontHeight)) {
            this.textRenderer.drawWithShadow(matrices, UNDERSCORE, (float) x, (float) endY, 0xffd0d0d0);
        }

        if (this.editBox.hasSelection()) {
            EditBox.Substring selection = this.editBox.getSelection();
            int selX = this.getX() + this.getPadding();
            y = this.getY() + this.getPadding();
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
                    CaxtonText caxtonLine = CaxtonText.fromForwards(
                            text.substring(line.beginIndex(), line.endIndex()),
                            ctr.getFontStorageAccessor(),
                            Style.EMPTY,
                            false, false,
                            cth.getCache());
                    cth.getHighlightRanges(
                            caxtonLine,
                            Math.max(0, selection.beginIndex() - line.beginIndex()),
                            Math.min(selection.endIndex(), line.endIndex()) - line.beginIndex(),
                            (left, right) -> {
                                Voepfxo.drawSelection(matrices, selX + left, selY, selX + right, selY + this.textRenderer.fontHeight);
                            });
                }
                y += this.textRenderer.fontHeight;
            }
        }

        ci.cancel(); // Ḋo.
    }
}
