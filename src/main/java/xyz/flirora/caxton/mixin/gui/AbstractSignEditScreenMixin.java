package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.flirora.caxton.layout.CaxtonText;
import xyz.flirora.caxton.layout.CaxtonTextHandler;
import xyz.flirora.caxton.layout.DirectionSetting;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin
        extends Screen {
    @Shadow
    @Final
    protected SignBlockEntity blockEntity;
    @Shadow
    @Final
    protected String[] text;
    @Shadow
    private SelectionManager selectionManager;
    @Shadow
    private int currentRow;
    @Shadow
    private int ticksSinceOpened;
    @Unique
    private CaxtonText rowText;
    @Unique
    private CaxtonText currentRowText;

    protected AbstractSignEditScreenMixin(Text title) {
        super(title);
    }

    // Use the more precise CaxtonTextHandler.getWidth instead of TextHandler.getWidth
    @ModifyArg(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I", ordinal = 0), index = 1)
    private float modifyXForDrawingRow(String currentLine, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType, int backgroundColor, int light, boolean rightToLeft) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();
        this.rowText = CaxtonText.fromFormatted(currentLine, ctr.getFontStorageAccessor(), Style.EMPTY, false, this.textRenderer.isRightToLeft(), cth.getCache());
        return -cth.getWidth(rowText) / 2.0f;
    }

    // Use the more precise CaxtonTextHandler.getWidth instead of TextHandler.getWidth
    @ModifyArg(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I", ordinal = 1), index = 1)
    private float modifyXForDrawingEndCursor(float x) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();
        this.currentRowText = rowText;
        return cth.getWidth(rowText) / 2.0f;
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractSignEditScreen;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V", ordinal = 0))
    private void drawVerticalCursor(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int color) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();
        String currentLine = this.text[this.currentRow];
        int selectionStart = this.selectionManager.getSelectionStart();
        int yAdj = 4 * this.blockEntity.getTextLineHeight() / 2;
        int lineY = currentRow * this.blockEntity.getTextLineHeight() - yAdj;

        float x = cth.getWidth(currentRowText) / 2;
        float cursorOffset = cth.getOffsetAtIndex(currentRowText, Math.min(selectionStart, currentLine.length()), DirectionSetting.AUTO);
        float cursorX = cursorOffset - x;
        Voepfxo.fill(
                matrixStack,
                cursorX, lineY - 1,
                cursorX + 1, lineY + this.blockEntity.getTextLineHeight(),
                0xFF000000 | color);
    }

    @Redirect(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractSignEditScreen;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V", ordinal = 1))
    private void drawHighlights(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) textRenderer).getCaxtonTextRenderer();
        CaxtonTextHandler cth = ctr.getHandler();

        int selectionStart = selectionManager.getSelectionStart();
        int selectionEnd = selectionManager.getSelectionEnd();
        float x = cth.getWidth(this.currentRowText) / 2;
        int yAdj = 4 * this.blockEntity.getTextLineHeight() / 2;
        int lineY = currentRow * this.blockEntity.getTextLineHeight() - yAdj;

        cth.getHighlightRanges(currentRowText, Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd), (start, end) -> {
            Voepfxo.fill(matrices, start - x, lineY, end - x, lineY + this.blockEntity.getTextLineHeight(), color);
        });
    }
}
