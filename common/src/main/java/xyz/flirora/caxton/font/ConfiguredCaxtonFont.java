// (C) 2022-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.lang.foreign.MemorySegment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.dll.Caxton1;
import xyz.flirora.caxton.layout.ShapingResult;

/**
 * A font with additional settings.
 *
 * <p>Whether an option is held by {@link CaxtonFont} or by {@link ConfiguredCaxtonFont} is
 * determined by whether it affects MSDF generation. Options that affect MSDF generation, such as
 * shrinkage, and those that affect atlas building, such as atlas page size, belong in {@link
 * CaxtonFont}. Other options, such as the shadow offset or the use of OpenType features, are passed
 * to {@link ConfiguredCaxtonFont}. To put it another way, options that are set in {@code .ttf.json}
 * or {@code .otf.json} files are the ones passed to {@link CaxtonFont}, while those set as part of
 * a font provider itself are the domain of {@link ConfiguredCaxtonFont}.
 *
 * @param font the underlying {@link CaxtonFont}
 * @param ptr the address of the pointer to the configured font in native memory
 * @param shadowOffset the shadow offset of the font
 * @param computedScaleFactor the scale factor from typographic units to scaled GUI units
 * @param shiftX the horizontal shift to apply, in scaled GUI units
 * @param shiftY the vertical shift to apply, in scaled GUI units
 * @param slant the slant amount
 * @see CaxtonFont
 * @see ConfiguredCaxtonFont
 */
@Environment(EnvType.CLIENT)
public record ConfiguredCaxtonFont(
    CaxtonFont font,
    MemorySegment segment,
    float shadowOffset,
    float computedScaleFactor,
    float shiftX,
    float shiftY,
    float slant)
    implements AutoCloseable {
  /**
   * Loads a {@link ConfiguredCaxtonFont} from its settings.
   *
   * @param manager the {@link ResourceManager} holding the resources
   * @param settings the {@link ConfiguredCaxtonFont.Loader} that holds the settings for the font
   * @return a {@link ConfiguredCaxtonFont}
   */
  public static @Nullable ConfiguredCaxtonFont load(
      ResourceManager manager, @Nullable ConfiguredCaxtonFont.Loader settings) {
    if (settings == null) return null;
    String config = settings.settings == null ? null : settings.settings.toString();
    CaxtonFont font = CaxtonFontLoader.loadFontByIdentifier(manager, settings.id);
    if (font == null) {
      throw new RuntimeException("font " + settings.id + " does not exist");
    }
    MemorySegment segment = Caxton1.fontConfigure(font.getFontSegment(), config);
    float shadowOffset = 1.0f;
    double scaleFactor = 1.0f;
    float shiftX = 0.0f, shiftY = 0.0f, slant = 0.0f;
    if (settings.settings != null) {
      shadowOffset = JsonHelper.getFloat(settings.settings, "shadow_offset", shadowOffset);
      scaleFactor = JsonHelper.getDouble(settings.settings, "scale_factor", scaleFactor);
      if (JsonHelper.hasBoolean(settings.settings, "blur")) {
        throw new JsonSyntaxException("blur is no longer configurable per glyph provider");
      }
      JsonElement shiftE = settings.settings.get("shift");
      if (shiftE instanceof JsonArray shift) {
        if (shift.size() != 2) {
          throw new JsonSyntaxException("shift must have 2 elements");
        }
        shiftX = shift.get(0).getAsFloat();
        shiftY = shift.get(1).getAsFloat();
      } else if (shiftE != null) {
        throw new JsonSyntaxException("shift must be an array");
      }
      slant =
          -Float.intBitsToFloat(
              JsonHelper.getInt(
                  settings.settings,
                  "the_font_designer_couldnt_be_assed_to_make_an_italic_variant_so_slant_the_text",
                  0));
    }
    float computedScaleFactor =
        (float) (7.0 * scaleFactor / font.getMetrics(CaxtonFont.Metrics.ASCENDER));
    return new ConfiguredCaxtonFont(
        font, segment, shadowOffset, computedScaleFactor, shiftX, shiftY, slant);
  }

  /**
   * Closes this object.
   *
   * <p>This frees the native memory allocated for this object as well as freeing a reference to the
   * underlying {@link CaxtonFont}.
   */
  @Override
  public void close() {
    Caxton1.cfontDestroy(segment);
    font.close();
  }

  /**
   * Gets the scale used to convert from font units to GUI units.
   *
   * @return the factor to multiply font units by to get GUI units
   */
  public float getScale() {
    return computedScaleFactor;
  }

  public ShapingResult shape(MemorySegment s, int start, int end, boolean rtl, int script) {
    return Caxton1.cfontShape(this.segment, s, start, end, rtl, script);
  }

  /**
   * Settings for loading a {@link ConfiguredCaxtonFont}.
   *
   * @param id the identifier of the {@link CaxtonFont} to use
   * @param settings the original JSON object specifying the {@link ConfiguredCaxtonFont}, or {@code
   *     null} if it was passed as an identifier alone
   */
  public record Loader(Identifier id, @Nullable JsonObject settings) {
    private static final Codec<Identifier> FILE_FIELD_CODEC =
        Identifier.CODEC.fieldOf("file").codec();
    public static final Codec<Loader> CODEC =
        Codec.either(
                Identifier.CODEC.xmap(id -> new Loader(id, null), Loader::id),
                Codec.PASSTHROUGH.comapFlatMap(
                    obj -> {
                      var elem = obj.cast(JsonOps.INSTANCE);
                      if (!(elem instanceof JsonObject settings)) {
                        return DataResult.error(() -> "Not a JSON object: " + elem);
                      }
                      return FILE_FIELD_CODEC.parse(obj).map(id -> new Loader(id, settings));
                    },
                    loader -> new Dynamic<>(JsonOps.INSTANCE, loader.settings)))
            .xmap(
                either -> either.left().orElse(either.right().get()),
                x -> x.settings == null ? Either.left(x) : Either.right(x));
    // If you’ve made it this far, why not take a break with a nice
    // f i c t i o n a l  l a n g u a g e  song?
    // https://www.youtube.com/watch?v=jaJQM4BqlAY
  }
}
