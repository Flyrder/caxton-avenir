// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.forge.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.FontType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.flirora.caxton.font.CaxtonFontLoader;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(FontType.class)
public class FontTypeMixin {
    @Invoker("<init>")
    private static FontType newVariant(String internalName, int internalId, String id, MapCodec<? extends FontLoader> loaderCodec) {
        throw new AssertionError();
    }

    @Shadow
    private static @Final
    @Mutable
    FontType[] field_2316;

    @Inject(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/client/font/FontType;field_2316:[Lnet/minecraft/client/font/FontType;", shift = At.Shift.AFTER))
    private static void addCustomVariant(CallbackInfo ci) {
        var variants = new ArrayList<>(Arrays.asList(field_2316));
        var last = variants.get(variants.size() - 1);
        // This means our code will still work if other mods or Mojang add more variants!
        var caxton = newVariant("CAXTON", last.ordinal() + 1, "caxton", CaxtonFontLoader.CODEC);
        variants.add(caxton);
        field_2316 = variants.toArray(new FontType[0]);
    }
}
