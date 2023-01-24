package xyz.flirora.caxton.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Options for rendering text in a {@link CaxtonFont}.
 * <p>
 * This does not contain all options relevant to the font, only the ones that are needed by the Java side of the code.
 *
 * @param shrinkage the number of font units corresponding to one pixel of the MSDF atlas
 * @param margin    the number of extra pixels to add as margins around all sides of the glyph. This should be greater than or equal to {@code range}.
 * @param range     the pixel range between the minimum and maximum representable signed distance of the MSDF
 * @param invert    whether to invert the MSDF of each glyph
 */
@Environment(EnvType.CLIENT)
public record CaxtonFontOptions(
        double shrinkage,
        int margin,
        int range,
        Invert invert) {
    public CaxtonFontOptions {
        if (shrinkage <= 0.0) {
            throw new IllegalArgumentException("shrinkage must be positive");
        }
        if (margin < 0) {
            margin = range;
        }
        if (range <= 0) {
            throw new IllegalArgumentException("range must be positive");
        }
        if (range >= 256) {
            throw new IllegalArgumentException("range cannot be greater than 255");
        }
    }

    public CaxtonFontOptions(JsonObject json) {
        this(
                JsonHelper.getDouble(json, "shrinkage", 32.0),
                JsonHelper.getInt(json, "margin", -1),
                JsonHelper.getInt(json, "range", 4),
                Invert.fromJson(json.get("invert"))
        );
    }

    public enum Invert {
        UNSET,
        FALSE,
        TRUE;

        public static Invert fromJson(@Nullable JsonElement json) {
            if (json == null || json.isJsonNull()) {
                return UNSET;
            }
            try {
                boolean b = json.getAsBoolean();
                return b ? TRUE : FALSE;
            } catch (Exception e) {
                throw new JsonSyntaxException("invert must be true, false, or null", e);
            }
        }
    }
}
