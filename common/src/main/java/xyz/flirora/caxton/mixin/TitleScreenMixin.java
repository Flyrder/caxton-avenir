package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.flirora.caxton.CaxtonModClient;

@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private static final Identifier SGA_FONT_ID = new Identifier("minecraft", "alt");
    private static final Style STYLE = Style.EMPTY.withFont(SGA_FONT_ID);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"))
    private int voepfxo(DrawContext instance, TextRenderer textRenderer, String s, int x, int y, int color) {
        if (!CaxtonModClient.APRIL_FOOLS) {
            return instance.drawTextWithShadow(textRenderer, s, x, y, color);
        }
        return instance.drawTextWithShadow(textRenderer, visitor -> {
            int len = s.length();
            int i = 0;
            while (i < len) {
                int c = s.codePointAt(i);
                boolean sga = c == ' ' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
                if (!visitor.accept(i, sga ? STYLE : Style.EMPTY, c)) return false;
                i += Character.charCount(c);
            }
            return true;
        }, x, y, color);
    }
}
