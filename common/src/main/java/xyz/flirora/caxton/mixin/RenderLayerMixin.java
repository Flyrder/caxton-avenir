// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.flirora.caxton.layout.RenderSetupExt;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {
  @Shadow @Final private RenderSetup renderSetup;

  @ModifyReceiver(
      method = "draw",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V"))
  private RenderPass setUnitRangeUniform(
      RenderPass instance, int baseVertex, int firstIndex, int count, int instanceCount) {
    GpuBuffer unitRangeBuffer = ((RenderSetupExt) (Object) this.renderSetup).caxton$getUnitRange();
    if (unitRangeBuffer != null) instance.setUniform("UnitRangeU", unitRangeBuffer);
    return instance;
  }
}
