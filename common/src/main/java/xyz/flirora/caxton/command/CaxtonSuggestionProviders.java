// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.font.CaxtonFontLoader;

public class CaxtonSuggestionProviders {
  public static final SuggestionProvider<?> CAXTON_FONTS =
      SuggestionProviders.register(
          Identifier.of(CaxtonModClient.MOD_ID, "caxton_fonts"),
          (context, builder) ->
              CommandSource.suggestIdentifiers(CaxtonFontLoader.getAvailableFontIds(), builder));
}
