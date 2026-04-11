// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.fabric;

import com.chocohead.mm.api.ClassTinkerers;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import xyz.flirora.caxton.font.CaxtonFontLoader;

/**
 * An early riser for the Caxton mod.
 *
 * <p>This has the primary purpose of adding a variant to the {@link
 * net.minecraft.client.font.FontType} enum. It also initializes MixinExtras.
 */
public class CaxtonEarlyRiser implements Runnable {
  @Override
  public void run() {
    MappingResolver remapper = FabricLoader.getInstance().getMappingResolver();

    String fontType =
        remapper.mapClassName(
            "official", "net.minecraft.client.gui.font.providers.GlyphProviderType");
    ClassTinkerers.enumBuilder(fontType, String.class, "Lcom/mojang/serialization/MapCodec;")
        .addEnum("CAXTON_TTF", () -> new Object[] {"caxton", CaxtonFontLoader.CODEC})
        .build();
  }
}
