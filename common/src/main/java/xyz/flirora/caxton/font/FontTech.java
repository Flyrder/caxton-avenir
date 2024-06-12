// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.font;

import com.google.gson.JsonSyntaxException;

public enum FontTech {
  MSDF("msdf", false),
  RASTER("raster", true),
  ;
  private final String name;
  private final boolean offsetOutlineGlyphsByGlyphCounts;

  FontTech(String name, boolean offsetOutlineGlyphsByGlyphCounts) {
    this.name = name;
    this.offsetOutlineGlyphsByGlyphCounts = offsetOutlineGlyphsByGlyphCounts;
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

  public boolean offsetOutlineGlyphsByGlyphCounts() {
    return this.offsetOutlineGlyphsByGlyphCounts;
  }
}
