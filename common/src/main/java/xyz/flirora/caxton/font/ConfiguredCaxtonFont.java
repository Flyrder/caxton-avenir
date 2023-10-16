package xyz.flirora.caxton.font;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.dll.CaxtonInternal;
import xyz.flirora.caxton.layout.ShapingResult;

import java.util.function.Function;

/**
 * A font with additional settings.
 * <p>
 * Whether an option is held by {@link CaxtonFont} or by {@link ConfiguredCaxtonFont} is determined by whether it affects MSDF generation. Options that affect MSDF generation, such as shrinkage, and those that affect atlas building, such as atlas page size, belong in {@link CaxtonFont}. Other options, such as the shadow offset or the use of OpenType features, are passed to {@link ConfiguredCaxtonFont}. To put it another way, options that are set in {@code .ttf.json} or {@code .otf.json} files are the ones passed to {@link CaxtonFont}, while those set as part of a font provider itself are the domain of {@link ConfiguredCaxtonFont}.
 *
 * @param font                the underlying {@link CaxtonFont}
 * @param ptr                 the address of the pointer to the configured font in native memory
 * @param shadowOffset        the shadow offset of the font
 * @param computedScaleFactor the scale factor from typographic units to scaled GUI units
 * @param blur                true if the glyph textures should be antialiased
 * @param shiftX              the horizontal shift to apply, in scaled GUI units
 * @param shiftY              the vertical shift to apply, in scaled GUI units
 * @see CaxtonFont
 * @see ConfiguredCaxtonFont
 */
@Environment(EnvType.CLIENT)
public record ConfiguredCaxtonFont(CaxtonFont font, long ptr, float shadowOffset,
                                   float computedScaleFactor, boolean blur, float shiftX,
                                   float shiftY) implements AutoCloseable {
    /**
     * Loads a {@link ConfiguredCaxtonFont} from its settings.
     *
     * @param manager  the {@link ResourceManager} holding the resources
     * @param settings the {@link ConfiguredCaxtonFont.Loader} that holds the settings for the font
     * @return a {@link ConfiguredCaxtonFont}
     */

    public static @Nullable ConfiguredCaxtonFont load(ResourceManager manager, @Nullable ConfiguredCaxtonFont.Loader settings) {
        if (settings == null) return null;
        String config = settings.settings == null ? null : settings.settings.toString();
        CaxtonFont font = CaxtonFontLoader.loadFontByIdentifier(manager, settings.id);
        if (font == null) {
            throw new RuntimeException("font " + settings.id + " does not exist");
        }
        long ptr = CaxtonInternal.configureFont(font.getFontPtr(), config);
        float shadowOffset = 1.0f;
        double scaleFactor = 1.0f;
        boolean blur = false;
        float shiftX = 0.0f, shiftY = 0.0f;
        if (settings.settings != null) {
            shadowOffset = JsonHelper.getFloat(settings.settings, "shadow_offset", shadowOffset);
            scaleFactor = JsonHelper.getDouble(settings.settings, "scale_factor", scaleFactor);
            blur = JsonHelper.getBoolean(settings.settings, "blur", blur);
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
        }
        float computedScaleFactor = (float) (7.0 * scaleFactor / font.getMetrics(CaxtonFont.Metrics.ASCENDER));
        return new ConfiguredCaxtonFont(font, ptr, shadowOffset, computedScaleFactor, blur, shiftX, shiftY);
    }

    /**
     * Closes this object.
     * <p>
     * This frees the native memory allocated for this object as well as freeing a reference to the underlying {@link CaxtonFont}.
     */
    @Override
    public void close() {
        CaxtonInternal.destroyConfiguredFont(ptr);
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

    /**
     * Shapes a number of runs of text.
     *
     * @param s        the array of UTF-16 code units for the entire text
     * @param bidiRuns an array of integers consisting of interleaved {@code [start, end, level]} triples
     * @return an array of {@link ShapingResult}s whose length is {@code bidiRuns.length / 4}, such that {@code result[i]} corresponds to {@code new String(s, bidiRuns[4 * i], bidiRuns[4 * i + 1] - bidiRuns[4 * i]}
     */
    public ShapingResult[] shape(char[] s, int[] bidiRuns) {
        return CaxtonInternal.shape(ptr, s, bidiRuns);
    }

    /**
     * Settings for loading a {@link ConfiguredCaxtonFont}.
     *
     * @param id       the identifier of the {@link CaxtonFont} to use
     * @param settings the original JSON object specifying the {@link ConfiguredCaxtonFont}, or {@code null} if it was passed as an identifier alone
     */
    public record Loader(Identifier id, @Nullable JsonObject settings) {
        private static final Codec<Identifier> FILE_FIELD_CODEC = Identifier.CODEC.fieldOf("file").codec();
        public static final Codec<Loader> CODEC = Codec.either(
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
                .xmap(either -> either.left().orElse(either.right().get()), x -> x.settings == null ? Either.left(x) : Either.right(x));
        // If you’ve made it this far, why not take a break with a nice
        // f i c t i o n a l  l a n g u a g e  song?
        // https://www.youtube.com/watch?v=jaJQM4BqlAY
    }
}
