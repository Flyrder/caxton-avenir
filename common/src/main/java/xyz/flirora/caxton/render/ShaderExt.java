// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GlUniform;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ShaderExt {
  @Nullable GlUniform caxton$getUnitRange();

  void caxton$initUniforms();
}
