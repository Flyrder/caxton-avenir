package xyz.flirora.caxton.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.resource.SinglePreparationResourceReloader;
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(SplashTextResourceSupplier.class)
public abstract class SplashTextResourceSupplierMixin extends SinglePreparationResourceReloader<List<String>> {
    @Unique
    private static final String[] TEXTS = {
            "lena aaxa leev axel lana en laap axel var tuus salt sein, non ladia tu.",
            "non ter vil le fai le miyut velt a faar.",
            "drołecþe ašir eslasos anasos vonat ħara; cere fose.",
            "pevescþasos ašir łirlen anasos vonat ħara; mena fose.",
            "naðesos anor jonas łorcþit ħare; jorniłirþ cercli mevas’ce menat pentu se.",
            "ênsêna orgit noldeþa inora; cajoþ il cerþel mêven tełaverþ.",
            "feljan racriþ narviłos roc lê gcjani nêrge; aspoþ’pe mîr’moc c·erifos roc cþere.",
            "eti crešit cfjoþelt mjonelþa meðałes nîs mivełen cenvat nevla.",
            "telmaðirþ cehit geðates aline ar sêrnime mitrit denestâs tal!",
    };
    @Shadow
    @Final
    private static Random RANDOM;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onGet(CallbackInfoReturnable<SplashTextRenderer> cir) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if (calendar.get(Calendar.MONTH) == Calendar.FEBRUARY && calendar.get(Calendar.DATE) == 4) {
            String text = TEXTS[RANDOM.nextInt(TEXTS.length)];
            cir.setReturnValue(new SplashTextRenderer(text));
        }
    }

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void tweakOnAprilFools(CallbackInfoReturnable<SplashTextRenderer> cir) {
        if (CaxtonModClient.APRIL_FOOLS && cir.getReturnValue() != null) {
            cir.setReturnValue(new SplashTextRenderer(Voepfxo.shingetsuGaShirokuMarukuKagayaiteIruYoruNi(((SplashTextRendererAccessor) cir.getReturnValue()).getText())));
        }
    }
}
