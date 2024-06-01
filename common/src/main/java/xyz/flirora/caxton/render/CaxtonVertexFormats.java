// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormatElement.ComponentType;
import net.minecraft.client.render.VertexFormatElement.Usage;

public class CaxtonVertexFormats {
  public static final VertexFormatElement COLOR_2 =
      VertexFormatElement.register(6, 0, ComponentType.UBYTE, Usage.COLOR, 4);

  public static final VertexFormat POSITION_COLOR_COLOR_TEXTURE_LIGHT =
      VertexFormat.builder()
          .add("Position", VertexFormatElement.POSITION)
          .add("Color0", VertexFormatElement.COLOR)
          .add("Color1", COLOR_2)
          .add("UV0", VertexFormatElement.UV_0)
          .add("UV2", VertexFormatElement.UV_2)
          .build();
}
