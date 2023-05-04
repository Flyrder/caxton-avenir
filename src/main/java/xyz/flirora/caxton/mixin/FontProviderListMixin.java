package xyz.flirora.caxton.mixin;

import com.google.gson.JsonObject;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.FontManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(FontManager.class_8556.class)
public abstract class FontProviderListMixin implements AutoCloseable {
    @Mutable
    @Shadow @Final public static Codec<FontManager.class_8556> field_44798;

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/client/font/FontManager$class_8556;field_44798:Lcom/mojang/serialization/Codec;"))
    private static void aaa(Codec<FontManager.class_8556> value) {
        Codec<FontManager.class_8556> caxtonCodec = RecordCodecBuilder.create(instance -> instance.group(
                FontLoader.field_44801.listOf().fieldOf("caxton_providers")
                        .forGetter(FontManager.class_8556::providers))
                .apply(instance, FontManager.class_8556::new));
        field_44798 = Codec.either(caxtonCodec, value).xmap(e -> e.map(Function.identity(), Function.identity()), Either::right);
    }
}
