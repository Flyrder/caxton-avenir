// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.flirora.caxton.layout.RenderSetupExt;

@Mixin(RenderSetup.class)
public class RenderSetupMixin implements RenderSetupExt {
  @Unique private GpuBuffer caxton$unitRange;

  @Override
  public void caxton$setUnitRange(GpuBuffer range) {
    this.caxton$unitRange = range;
  }

  @Override
  public GpuBuffer caxton$getUnitRange() {
    return caxton$unitRange;
  }
}
