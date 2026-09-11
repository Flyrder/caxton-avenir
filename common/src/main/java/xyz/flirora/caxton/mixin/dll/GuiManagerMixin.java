// (C) 2022-2023 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin.dll;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiManager;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.dll.LibraryLoading;

@Environment(EnvType.CLIENT)
@Mixin(GuiManager.class)
public class GuiManagerMixin {
  @Shadow @Final private MinecraftClient client;

  @Inject(method = "getStartupScreenFactories", at = @At("RETURN"))
  private void addStartupScreenFactories(
      List<Function<Runnable, Screen>> factoriesOut,
      CallbackInfoReturnable<Boolean> cir,
      @Local(name = "onboardingScreenAdded") LocalBooleanRef onboardingScreenAdded) {
    Exception e = LibraryLoading.getLoadingException();
    if (e != null) {
      factoriesOut.add(closeAction -> LibraryLoading.nativeLibraryLoadFailedScreen(e, closeAction));
      onboardingScreenAdded.set(true);
    }
  }
}
