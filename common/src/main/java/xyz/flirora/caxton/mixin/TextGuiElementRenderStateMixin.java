// (C) 2025 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.text.OrderedText;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.render.CaxtonTextRenderer;
import xyz.flirora.caxton.render.HasCaxtonTextRenderer;
import xyz.flirora.caxton.render.Voepfxo;

@Mixin(TextGuiElementRenderState.class)
public class TextGuiElementRenderStateMixin {
  @Shadow @Final public OrderedText orderedText;

  @Shadow @Nullable private TextRenderer.GlyphDrawable preparation;

  @Shadow @Final public TextRenderer textRenderer;

  @Shadow @Final public int color;

  @Shadow @Nullable private ScreenRect bounds;

  @Shadow @Final @Nullable public ScreenRect clipBounds;

  @Shadow @Final public Matrix3x2fc matrix;

  @Shadow @Final public int backgroundColor;

  @Shadow @Final public boolean shadow;

  @Shadow @Final private boolean trackSkeletons;

  @Inject(method = "prepare", at = @At("HEAD"))
  private void onPrepare(CallbackInfoReturnable<TextRenderer.GlyphDrawable> cir) {
    if (this.preparation == null && this.orderedText instanceof Voepfxo.SneakyBoi sneakyBoi) {
      CaxtonTextRenderer ctr = ((HasCaxtonTextRenderer) this.textRenderer).getCaxtonTextRenderer();
      // Is this an outlined drawer?
      if (sneakyBoi.outlineColor() != 0) {
        this.preparation =
            ctr.prepareOutlined(
                sneakyBoi.caxtonText(),
                sneakyBoi.x(),
                sneakyBoi.y(),
                this.color,
                sneakyBoi.outlineColor(),
                0xff0000ff,
                false);
      } else {
        this.preparation =
            ctr.prepare(
                sneakyBoi.caxtonText(),
                sneakyBoi.x(),
                sneakyBoi.y(),
                this.color,
                this.shadow,
                this.trackSkeletons,
                this.backgroundColor,
                sneakyBoi.firstCharacterIndex(),
                sneakyBoi.maxWidth());
      }
      ScreenRect screenRect = this.preparation.getScreenRect();
      if (screenRect != null) {
        screenRect = screenRect.transformEachVertex(this.matrix);
        this.bounds =
            this.clipBounds != null ? this.clipBounds.intersection(screenRect) : screenRect;
      }
    }
  }
}
