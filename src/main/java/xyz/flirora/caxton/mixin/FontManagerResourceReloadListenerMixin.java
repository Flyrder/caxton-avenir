package xyz.flirora.caxton.mixin;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(FontManager.class)
public abstract class FontManagerResourceReloadListenerMixin implements AutoCloseable {
    @ModifyArg(method = "loadFontProviders", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/JsonHelper;getArray(Lcom/google/gson/JsonObject;Ljava/lang/String;)Lcom/google/gson/JsonArray;"), allow = 1, require = 1, index = 1)
    private static String providers(JsonObject object, String key) {
        if (object.has("caxton_providers"))
            return "caxton_providers";
        return key;
    }
}
