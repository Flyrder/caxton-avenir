// (C) 2023-2024 +merlan #flirora.
// SPDX-License-Identifier: MIT
package xyz.flirora.caxton.mixin;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.flirora.caxton.CaxtonModClient;
import xyz.flirora.caxton.render.Voepfxo;

@Environment(EnvType.CLIENT)
@Mixin(SplashTextResourceSupplier.class)
public abstract class SplashTextResourceSupplierMixin
    extends SinglePreparationResourceReloader<List<Text>> {
  @Unique private static final Text[] caxton$TEXTS = {
    create("lena aaxa leev axel lana en laap axel var tuus salt sein, non ladia tu."),
    create("non ter vil le fai le miyut velt a faar."),
    create("drołecþe ašir eslasos anasos vonat ħara; cere fose."),
    create("pevescþasos ašir łirlen anasos vonat ħara; mena fose."),
    create("naðesos anor jonas łorcþit ħare; jorniłirþ cercli mevas’ce menat pentu se."),
    create("ênsêna orgit noldeþa inora; cajoþ il cerþel mêven tełaverþ."),
    create("feljan racriþ narviłos roc lê gcjani nêrge; aspoþ’pe mîr’moc c·erifos roc cþere."),
    create("eti crešit cfjoþelt mjonelþa meðałes nîs mivełen cenvat nevla."),
    create("telmaðirþ cehit geðates aline ar sêrnime mitrit denestâs tal!"),
  };

  @Shadow @Final private static Random RANDOM;

  @Shadow
  private static Text create(String text) {
    return null;
  }

  @Inject(method = "get", at = @At("HEAD"), cancellable = true)
  private void onGet(CallbackInfoReturnable<SplashTextRenderer> cir) {
    if (CaxtonModClient.CONFIG.disableEasterEggs) return;
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY && calendar.get(Calendar.DATE) == 4) {
      Text text = caxton$TEXTS[RANDOM.nextInt(caxton$TEXTS.length)];
      cir.setReturnValue(new SplashTextRenderer(text));
    }
  }

  @Inject(method = "get", at = @At("RETURN"), cancellable = true)
  private void tweakOnAprilFools(CallbackInfoReturnable<SplashTextRenderer> cir) {
    if (CaxtonModClient.CONFIG.disableEasterEggs) return;
    if (CaxtonModClient.APRIL_FOOLS && cir.getReturnValue() != null) {
      Text text = ((SplashTextRendererAccessor) cir.getReturnValue()).getText();
      String s;
      if ((s = text.getLiteralString()) != null) {
        text = create(Voepfxo.shingetsuGaShirokuMarukuKagayaiteIruYoruNi(s));
        cir.setReturnValue(new SplashTextRenderer(text));
      }
    }
  }
}
