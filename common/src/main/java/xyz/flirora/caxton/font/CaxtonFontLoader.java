// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.FontType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.dll.LibraryLoading;

/** A {@link FontLoader} for the {@code caxton} font type. */
@Environment(EnvType.CLIENT)
public class CaxtonFontLoader implements FontLoader {
  public static final String FONT_PREFIX = "textures/font/";
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<Identifier, CaxtonFont> CACHE = new ConcurrentHashMap<>();
  private static final JsonObject EMPTY = new JsonObject();
  private final ConfiguredCaxtonFont.Loader regular;
  @Nullable private final ConfiguredCaxtonFont.Loader bold, italic, boldItalic;

  public static final MapCodec<CaxtonFontLoader> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      ConfiguredCaxtonFont.Loader.CODEC
                          .fieldOf("regular")
                          .forGetter(x -> x.regular),
                      ConfiguredCaxtonFont.Loader.CODEC
                          .optionalFieldOf("bold")
                          .forGetter(x -> Optional.ofNullable(x.bold)),
                      ConfiguredCaxtonFont.Loader.CODEC
                          .optionalFieldOf("italic")
                          .forGetter(x -> Optional.ofNullable(x.italic)),
                      ConfiguredCaxtonFont.Loader.CODEC
                          .optionalFieldOf("bold_italic")
                          .forGetter(x -> Optional.ofNullable(x.boldItalic)))
                  .apply(
                      instance,
                      (regular, bold, italic, boldItalic) ->
                          new CaxtonFontLoader(
                              regular,
                              bold.orElse(null),
                              italic.orElse(null),
                              boldItalic.orElse(null))));

  /**
   * Constructs a new {@link CaxtonFontLoader}.
   *
   * @param regular the {@link ConfiguredCaxtonFont.Loader} to use for the regular style
   * @param bold the {@link ConfiguredCaxtonFont.Loader} to use for the bold style, or null to fall
   *     back to the regular style
   * @param italic the {@link ConfiguredCaxtonFont.Loader} to use for the italic style, or null to
   *     fall back to the regular style
   * @param boldItalic the {@link ConfiguredCaxtonFont.Loader} to use for the bold italic style, or
   *     null to fall back to the regular style
   */
  public CaxtonFontLoader(
      ConfiguredCaxtonFont.Loader regular,
      ConfiguredCaxtonFont.@Nullable Loader bold,
      ConfiguredCaxtonFont.@Nullable Loader italic,
      ConfiguredCaxtonFont.@Nullable Loader boldItalic) {
    this.regular = regular;
    this.bold = bold;
    this.italic = italic;
    this.boldItalic = boldItalic;
  }

  /**
   * Loads a {@link CaxtonFont} by its identifier, or gets a cached copy if it has already been
   * loaded.
   *
   * @param manager the {@link ResourceManager} holding the resources
   * @param id the {@link Identifier} to load the font by
   * @return a {@link CaxtonFont}, with its reference count appropriately updated
   */
  @Nullable public static CaxtonFont loadFontByIdentifier(ResourceManager manager, @Nullable Identifier id) {
    if (id == null) return null;
    return CACHE
        .computeIfAbsent(
            id,
            id1 -> {
              LOGGER.info("Loading font {}", id);
              try {
                Identifier fontId = id1.withPrefixedPath(FONT_PREFIX);
                Identifier metaId = fontId.withPath(path -> path + ".json");
                JsonObject optionsJson = EMPTY;
                Optional<Resource> metaResource = manager.getResource(metaId);
                if (metaResource.isPresent()) {
                  try (BufferedReader metaInput = metaResource.get().getReader()) {
                    optionsJson = JsonHelper.deserialize(metaInput);
                    String path = JsonHelper.getString(optionsJson, "path", null);
                    if (path != null) {
                      fontId = new Identifier(path).withPrefixedPath(FONT_PREFIX);
                    }
                  }
                }
                try (InputStream input = manager.open(fontId)) {
                  return new CaxtonFont(input, id1, optionsJson);
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .cloneReference();
  }

  /**
   * Clears the font cache used by {@link CaxtonFontLoader#loadFontByIdentifier(ResourceManager,
   * Identifier)}.
   */
  public static void clearFontCache() {
    LOGGER.info("Clearing {} loaded font(s)", CACHE.size());
    CACHE.forEach((id, font) -> font.close());
    CACHE.clear();
  }

  /**
   * Gets a font by its identifier.
   *
   * @param id the {@link Identifier} that the font was loaded by
   * @return the {@link CaxtonFont} associated with {@code id}, or {@code null} if none. The
   *     reference count is not incremented.
   */
  public static CaxtonFont getFontById(Identifier id) {
    return CACHE.get(id);
  }

  public static Stream<Identifier> getAvailableFontIds() {
    System.err.println(CACHE);
    return CACHE.keySet().stream();
  }

  /**
   * Loads the typeface described by this object.
   *
   * @param manager the {@link ResourceManager} holding the resources
   * @return a {@link CaxtonTypeface} described by this object
   */
  @Nullable public Font load(ResourceManager manager) throws IOException {
    if (!LibraryLoading.isLibraryLoaded()) {
      throw new IOException("Refusing to load Caxton font when native library loading had failed");
    }
    try {
      ConfiguredCaxtonFont regular = ConfiguredCaxtonFont.load(manager, this.regular);
      ConfiguredCaxtonFont bold = ConfiguredCaxtonFont.load(manager, this.bold);
      ConfiguredCaxtonFont italic = ConfiguredCaxtonFont.load(manager, this.italic);
      ConfiguredCaxtonFont boldItalic = ConfiguredCaxtonFont.load(manager, this.boldItalic);
      return new CaxtonTypeface(regular, bold, italic, boldItalic);
    } catch (Exception exception) {
      LOGGER.error("Couldn't load truetype font", exception);
      throw new IOException(exception);
    }
  }

  @Override
  public Either<Loadable, Reference> build() {
    return Either.left(this::load);
  }

  @Override
  public FontType getType() {
    return CaxtonModClient.CAXTON_FONT_TYPE;
  }
}
