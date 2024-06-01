// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import com.google.gson.JsonSyntaxException;

public enum FontTech {
    MSDF("msdf", false),
    RASTER("raster", true),
    ;
    private final String name;
    private final boolean twoPhasedOutlineText;

    FontTech(String name, boolean twoPhasedOutlineText) {
        this.name = name;
        this.twoPhasedOutlineText = twoPhasedOutlineText;
    }

    public static FontTech fromName(String name) {
        return switch (name) {
            case "msdf" -> MSDF;
            case "raster" -> RASTER;
            default -> throw new JsonSyntaxException("invalid font tech: " + name);
        };
    }

    public String getName() {
        return name;
    }

    /**
     * Returns whether this font draws outlined glyphs in two draw calls (one for the text itself and another for the outline), as opposed to a single call that draws both.
     * <p>
     * For compatibility with Iris Shaders, the {@link FontTech#RASTER} variant has this property.
     *
     * @return true if this font tech uses two-phased outlined text rendering
     */
    public boolean hasTwoPhasedOutlineTextRendering() {
        return twoPhasedOutlineText;
    }
}
