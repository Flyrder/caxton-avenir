// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.flirora.caxton.CaxtonModClient;

@Environment(EnvType.CLIENT)
public class CaxtonShaders {
  public static final Identifier TEXT_ID =
      new Identifier(CaxtonModClient.MOD_ID, "rendertype_text");
  public static final Identifier TEXT_SEE_THROUGH_ID =
      new Identifier(CaxtonModClient.MOD_ID, "rendertype_text_see_through");
  public static final Identifier TEXT_OUTLINE_ID =
      new Identifier(CaxtonModClient.MOD_ID, "rendertype_text_outline");
  @Nullable public static ShaderProgram caxtonTextShader, caxtonTextSeeThroughShader, caxtonTextOutlineShader;
}
