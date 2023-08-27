package xyz.flirora.caxton.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xyz.flirora.caxton.CaxtonModClient;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModResourceLocationFlavor {
    public CaxtonModResourceLocationFlavor() {
        // Submit our event bus to let architectury register our content on the right time
//        EventBuses.registerModEventBus(CaxtonModClient.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CaxtonModClient.init(new PlatformHooksResourceLocationFlavor());
    }
}
