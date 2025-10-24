// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.layout;

import com.mojang.blaze3d.buffers.GpuBuffer;

public interface RenderSetupExt {
  void caxton$setUnitRange(GpuBuffer range);

  GpuBuffer caxton$getUnitRange();
}
