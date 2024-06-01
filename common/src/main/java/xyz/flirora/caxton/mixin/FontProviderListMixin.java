// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
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
import xyz.flirora.caxton.dll.LibraryLoading;

@Environment(EnvType.CLIENT)
@Mixin(FontManager.Providers.class)
public abstract class FontProviderListMixin implements AutoCloseable {
  @Mutable @Shadow @Final public static Codec<FontManager.Providers> CODEC;

  @Redirect(
      method = "<clinit>",
      at =
          @At(
              value = "FIELD",
              opcode = Opcodes.PUTSTATIC,
              target =
                  "Lnet/minecraft/client/font/FontManager$Providers;CODEC:Lcom/mojang/serialization/Codec;"))
  private static void aaa(Codec<FontManager.Providers> value) {
    Codec<FontManager.Providers> caxtonCodec =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        FontLoader.Provider.CODEC
                            .listOf()
                            .fieldOf("caxton_providers")
                            .forGetter(FontManager.Providers::providers))
                    .apply(instance, FontManager.Providers::new));
    Codec<FontManager.Providers> caxtonCodec1 =
        caxtonCodec.validate(
            providers -> {
              if (LibraryLoading.isLibraryLoaded()) {
                return DataResult.success(providers);
              } else {
                return DataResult.error(() -> "Library not loaded");
              }
            });
    CODEC =
        Codec.either(caxtonCodec1, value)
            .xmap(e -> e.map(Function.identity(), Function.identity()), Either::right);
  }
}
