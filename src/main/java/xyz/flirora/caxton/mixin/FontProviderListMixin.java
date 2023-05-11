package xyz.flirora.caxton.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
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
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(FontManager.Providers.class)
public abstract class FontProviderListMixin implements AutoCloseable {
    @Mutable
    @Shadow
    @Final
    public static Codec<FontManager.Providers> CODEC;

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/client/font/FontManager$Providers;CODEC:Lcom/mojang/serialization/Codec;"))
    private static void aaa(Codec<FontManager.Providers> value) {
        Codec<FontManager.Providers> caxtonCodec = RecordCodecBuilder.create(instance -> instance.group(
                        FontLoader.CODEC.listOf().fieldOf("caxton_providers")
                                .forGetter(FontManager.Providers::providers))
                .apply(instance, FontManager.Providers::new));
        CODEC = Codec.either(caxtonCodec, value).xmap(e -> e.map(Function.identity(), Function.identity()), Either::right);
    }
}
