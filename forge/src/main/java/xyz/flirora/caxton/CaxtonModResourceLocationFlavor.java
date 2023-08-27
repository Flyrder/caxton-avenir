package xyz.flirora.caxton;

import net.minecraftforge.fml.common.Mod;

@Mod(CaxtonModClient.MOD_ID)
public class CaxtonModResourceLocationFlavor {
    public CaxtonModResourceLocationFlavor() {
        // Submit our event bus to let architectury register our content on the right time
//        EventBuses.registerModEventBus(CaxtonModClient.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CaxtonModClient.init();
    }
}
