// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import xyz.flirora.caxton.CaxtonModClient;

@Environment(EnvType.CLIENT)
public class CaxtonShaders {

  public static final Identifier TEXT_ID =
      Identifier.of(CaxtonModClient.MOD_ID, "core/rendertype_text");
  public static final Identifier TEXT_SEE_THROUGH_ID =
      Identifier.of(CaxtonModClient.MOD_ID, "core/rendertype_text_see_through");
  public static final Identifier TEXT_OUTLINE_ID =
      Identifier.of(CaxtonModClient.MOD_ID, "core/rendertype_text_outline");

  public static final ShaderProgramKey TEXT_KEY =
      new ShaderProgramKey(TEXT_ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, Defines.EMPTY);
  public static final ShaderProgramKey TEXT_SEE_THROUGH_KEY =
      new ShaderProgramKey(
          TEXT_SEE_THROUGH_ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, Defines.EMPTY);
  public static final ShaderProgramKey TEXT_OUTLINE_KEY =
      new ShaderProgramKey(
          TEXT_OUTLINE_ID, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, Defines.EMPTY);
}
