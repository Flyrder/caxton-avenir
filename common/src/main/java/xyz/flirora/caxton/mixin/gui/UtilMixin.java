package xyz.flirora.caxton.mixin.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.layout.gui.CaxtonUtil;

@Environment(EnvType.CLIENT)
@Mixin(Util.class)
public class UtilMixin {
    @Inject(at = @At("HEAD"), method = "moveCursor", cancellable = true)
    private static void onMoveCursor(String string, int cursor, int delta, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(CaxtonUtil.moveCursor(string, cursor, delta));
    }
}
