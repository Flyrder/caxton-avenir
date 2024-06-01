// (C) 2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import net.minecraft.client.font.TextRenderer;

public enum CaxtonTextLayerType {
  NORMAL,
  SEE_THROUGH,
  OUTLINE,
  POLYGON_OFFSET;

  public TextRenderer.TextLayerType asVanilla() {
    return switch (this) {
      case NORMAL -> TextRenderer.TextLayerType.NORMAL;
      case SEE_THROUGH -> TextRenderer.TextLayerType.SEE_THROUGH;
      case POLYGON_OFFSET -> TextRenderer.TextLayerType.POLYGON_OFFSET;
      default ->
          throw new IllegalArgumentException(
              "Text layer type " + this + "has no vanilla equivalent");
    };
  }

  public static CaxtonTextLayerType fromVanilla(TextRenderer.TextLayerType type) {
    return switch (type) {
      case NORMAL -> NORMAL;
      case SEE_THROUGH -> SEE_THROUGH;
      case POLYGON_OFFSET -> POLYGON_OFFSET;
    };
  }
}
